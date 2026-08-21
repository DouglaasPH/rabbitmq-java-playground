# Projeto 4 — Topic Exchange (RabbitMQ + Java 21)

Quarto projeto da série de estudos práticos de mensageria com RabbitMQ. Aqui o objetivo é entender **roteamento seletivo com wildcards**: eventos são publicados com routing keys hierárquicas (ex.: `pedido.criado.br`), e cada consumer decide, através de um padrão de binding, quais "fatias" desse fluxo de eventos quer receber.

---

## Objetivo de aprendizado

- Como a exchange do tipo **topic** compara a routing key da mensagem contra os padrões de binding registrados.
- A diferença entre os dois wildcards: `*` (substitui exatamente uma palavra) e `#` (substitui zero ou mais palavras).
- Que uma mesma mensagem pode casar com **múltiplos bindings ao mesmo tempo**.
- Como usar **virtual threads** (Java 21) para processar mensagens em callbacks de forma barata e concorrente.
- Onde o Topic Exchange se encaixa entre o Direct (roteamento exato) e o Fanout (roteamento para todos).

---

## Conceitos envolvidos

| Conceito | Onde aparece no código |
|---|---|
| Exchange do tipo `topic` | `channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.TOPIC, true)` |
| Routing key hierárquica | `"pedido.criado.br"`, `"pedido.cancelado.br"`, `"pedido.criado.us"` |
| Wildcard `#` (tudo) | Binding do `AuditoriaService` |
| Wildcard `*` (uma palavra) | Binding `"pedido.*.br"` do `NotificacaoBrService` |
| Fila exclusiva/temporária | `channel.queueDeclare().getQueue()` — sem nome, sem argumentos |
| Virtual threads | `Executors.newVirtualThreadPerTaskExecutor()` em `AuditoriaService` |

> Diferente do Projeto 3, aqui os consumers **não recebem tudo indiscriminadamente** — cada um define, através do padrão de binding, exatamente qual subconjunto do fluxo de eventos lhe interessa.

---

##️ Estrutura do projeto

```
projeto-4-topic/
├── pom.xml
├── docker-compose.yml
└── src/
    └── main/
        └── java/
            └── com/estudo/rabbitmq/topic/
                ├── PedidoEventoProducer.java
                ├── AuditoriaService.java        (assina "#")
                └── NotificacaoBrService.java    (assina "pedido.*.br")
```

---

## Pré-requisitos

- **JDK 21**
- **Maven** 3.6+
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

- **AMQP**: `localhost:5672`
- **Management UI**: [http://localhost:15672](http://localhost:15672) — login `admin` / `admin`

> Se já tem o RabbitMQ de outro projeto rodando, pode reaproveitar — as exchanges e filas têm nomes diferentes, não há conflito.

### 2. Compile o projeto

```bash
mvn clean compile
```

### 3. Suba os dois consumers (em dois terminais separados)

```bash
mvn exec:java -Dexec.mainClass="com.estudo.rabbitmq.topic.AuditoriaService"
```

```bash
mvn exec:java -Dexec.mainClass="com.estudo.rabbitmq.topic.NotificacaoBrService"
```

Cada terminal deve mostrar:

```
 [*] AuditoriaService (assina TUDO) aguardando...
 [*] NotificacaoBrService (so BR) aguardando...
```

### 4. Rode o Producer

Em um terceiro terminal:

```bash
mvn exec:java -Dexec.mainClass="com.estudo.rabbitmq.topic.PedidoEventoProducer"
```

Você verá três publicações:

```
 [x] Publicado [pedido.criado.br] {"pedidoId":"PED-01","pais":"BR"}
 [x] Publicado [pedido.criado.us] {"pedidoId":"PED-02","pais":"US"}
 [x] Publicado [pedido.cancelado.br] {"pedidoId":"PED-01","motivo":"estoque"}
```

No terminal do **AuditoriaService**, as três mensagens chegam:

```
 [auditoria] [pedido.criado.br] {"pedidoId":"PED-01","pais":"BR"}
 [auditoria] [pedido.criado.us] {"pedidoId":"PED-02","pais":"US"}
 [auditoria] [pedido.cancelado.br] {"pedidoId":"PED-01","motivo":"estoque"}
```

No terminal do **NotificacaoBrService**, apenas duas chegam (as que terminam em `.br`):

```
 [notificacao-br] {"pedidoId":"PED-01","pais":"BR"}
 [notificacao-br] {"pedidoId":"PED-01","motivo":"estoque"}
```

---

## O que observar na Management UI

Acesse [http://localhost:15672](http://localhost:15672) enquanto testa:

1. Vá em **Exchanges** → `pedidos.eventos` → aba **Bindings** e veja os dois padrões registrados (`#` e `pedido.*.br`).
2. Publique os três eventos e observe, na aba **Queues and Streams**, que a fila do `AuditoriaService` recebe 3 mensagens enquanto a do `NotificacaoBrService` recebe apenas 2.
3. Repare que as duas filas aparecem com nomes gerados automaticamente (algo como `amq.gen-...`), já que foram declaradas como exclusivas e temporárias.
