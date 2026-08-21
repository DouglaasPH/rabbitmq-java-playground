# Projeto 5 — Dead Letter Exchange + Retry (RabbitMQ + Java 25)

Quinto projeto da série de estudos práticos de mensageria com RabbitMQ. Aqui o objetivo é entender como lidar com **falhas de processamento de forma resiliente**: uma mensagem que falha não é descartada — ela circula automaticamente por um circuito de espera e volta para nova tentativa, até um limite máximo de retries, sem que nenhuma linha de código faça `Thread.sleep()`.

---

## Objetivo de aprendizado

- O que é uma **Dead Letter Exchange (DLX)** e como o RabbitMQ redireciona mensagens rejeitadas automaticamente.
- Como usar **TTL de fila** (`x-message-ttl`) como um "cronômetro" de backoff, sem bloquear nenhuma thread da aplicação.
- Por que uma fila pode ter um DLX configurado **e** ser, ela mesma, o destino de outra DLX — formando um ciclo controlado.
- Como usar **structured concurrency** (Java 21+/25) para aplicar timeout em uma tarefa de processamento.

---

## Conceitos envolvidos

| Conceito | Onde aparece no código |
|---|---|
| Dead Letter Exchange | `x-dead-letter-exchange` no `queueDeclare` de `TopologiaSetup` |
| TTL por fila (backoff sem sleep) | `x-message-ttl` na `fila.pagamentos.espera` |
| Nack sem requeue (aciona o DLX) | `channel.basicNack(deliveryTag, false, false)` |
| Structured concurrency + timeout | `StructuredTaskScope` em `processarComTimeout` |
| Header `x-death` (contagem de tentativas) | `contarTentativas(delivery)` |

---

## Estrutura do projeto

```
projeto-5-dlx-retry/
├── pom.xml
├── docker-compose.yml
└── src/
    └── main/
        └── java/
            └── com/estudo/
                ├── TopologiaSetup.java      (declara exchanges/filas)
                ├── PagamentoProducer.java
                └── PagamentoConsumer.java
```

---

## Entendendo a topologia (a parte mais importante deste projeto)

Diferente dos projetos anteriores, aqui a lógica não está só no Java — ela está principalmente na **configuração das filas**. `TopologiaSetup.declarar()` monta duas exchanges e duas filas que, juntas, formam um circuito fechado:

```
[pagamentos.exchange] → [fila.pagamentos] → PagamentoConsumer processa
        ▲                                            │
        │                                     falha → nack
        │                                            ▼
[fila.pagamentos.espera] ←──────────────── [dlx.pagamentos]
   (TTL = 5000ms, SEM consumer)
```

### O papel de cada peça

| Peça | Tipo | Papel |
|---|---|---|
| `pagamentos.exchange` | Exchange `direct` | Por onde tudo entra — producer publica aqui |
| `fila.pagamentos` | Fila | Onde o `PagamentoConsumer` realmente escuta e processa |
| `dlx.pagamentos` | Exchange `direct` | Só recebe mensagens rejeitadas — nunca é usada pelo producer |
| `fila.pagamentos.espera` | Fila | **Não tem nenhum consumer.** Existe só para "segurar" a mensagem por 5 segundos |

### O ciclo completo, passo a passo

| # | Evento | Quem faz |
|---|---|---|
| 1 | Producer publica em `pagamentos.exchange` | Seu código |
| 2 | Mensagem cai em `fila.pagamentos` (via binding) | RabbitMQ |
| 3 | `PagamentoConsumer` processa e falha → `nack(requeue=false)` | Seu código |
| 4 | RabbitMQ vê o `x-dead-letter-exchange` da fila e redireciona para `dlx.pagamentos` | RabbitMQ (automático) |
| 5 | Mensagem cai em `fila.pagamentos.espera` (via binding) | RabbitMQ |
| 6 | Mensagem fica parada ali por 5 segundos (TTL) — **sem nenhum consumer lendo** | RabbitMQ |
| 7 | TTL expira → RabbitMQ vê o `x-dead-letter-exchange` **dessa fila** e republica em `pagamentos.exchange` | RabbitMQ (automático) |
| 8 | Mensagem cai de novo em `fila.pagamentos` — nova tentativa começa | RabbitMQ |

> **O pulo do gato:** "dead lettering" não significa "descartar definitivamente". Significa apenas "esta mensagem saiu da fila por algum motivo (rejeição, TTL expirado, fila cheia) e, se houver uma exchange de destino configurada, ela é redirecionada em vez de perdida". Este projeto encadeia esse mecanismo duas vezes seguidas para simular um atraso, sem nenhuma thread bloqueada esperando.

---

## Pré-requisitos

- **JDK 25**
- **Maven** 3.9+
- **Docker**

---

## Como rodar

### 1. Suba o RabbitMQ

```bash
cd ..
docker compose up -d
```

> Certifique-se de estar na pasta raiz (rabbitmq-java-playground)

Isso sobe um container RabbitMQ com o plugin de management habilitado. Aguarde alguns segundos até o broker ficar pronto.

- **AMQP** (usado pela aplicação Java): `localhost:5672`
- **Management UI** (interface web): [http://localhost:15672](http://localhost:15672) — login `admin` / `admin`

> Se você já tem o RabbitMQ do Projeto 1 rodando, pode reaproveitar o mesmo container — as filas têm nomes diferentes, não há conflito.

### 2. Compile o projeto

```bash
mvn clean compile
```

### 3. Rode o Consumer

```bash
mvn exec:java -Dexec.mainClass="com.estudo.PagamentoConsumer"
```

Você deve ver:

```
 [*] PagamentoConsumer aguardando...
```

### 4. Rode o Producer em outro terminal

```bash
mvn exec:java -Dexec.mainClass="com.estudo.PagamentoProducer"
```

No terminal do **consumer**, você verá algo como:

```
 [!] Falha, enviando para retry: {"pedidoId":"PED-301","valor":199.90}
 [v] Pagamento aprovado: {"pedidoId":"PED-302","valor":199.90}
 [v] Pagamento aprovado: {"pedidoId":"PED-303","valor":199.90}
 [!] Falha, enviando para retry: {"pedidoId":"PED-304","valor":199.90}
```

E, se você **esperar ~5 segundos sem fazer nada**, deve ver a mesma mensagem (`PED-301`, `PED-304` etc.) reaparecer no log automaticamente, como uma nova tentativa:

```
 [!] Falha, enviando para retry: {"pedidoId":"PED-301","valor":199.90}
 ... (5 segundos depois) ...
 [v] Pagamento aprovado: {"pedidoId":"PED-301","valor":199.90}
```

Se isso não acontecer — se a mensagem falhar uma vez e nunca mais voltar — veja a seção de [Troubleshooting](#️-troubleshooting) abaixo.

---

## O que observar na Management UI

Acesse [http://localhost:15672](http://localhost:15672) enquanto testa:

1. Vá em **Queues and Streams** e abra `fila.pagamentos.espera`.
2. Deixe essa aba aberta enquanto publica mensagens que falham.
3. Observe o contador de mensagens subir para 1, ficar ali por ~5 segundos, e cair de volta para 0 — **exatamente quando** a mesma mensagem reaparece no log do `PagamentoConsumer`.
4. Esse é o teste mais confiável para confirmar que o retry está funcionando: se você vir esse "pulso" de 5 segundos na fila de espera, o mecanismo está correto.
