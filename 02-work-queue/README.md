# Projeto 2 — Work Queue (RabbitMQ + Java 11)

Segundo projeto da série de estudos práticos de mensageria com RabbitMQ. Aqui o objetivo é entender **distribuição de tarefas**: um producer publica várias mensagens, e múltiplas instâncias do mesmo consumer competem pela mesma fila — cada mensagem processada por **apenas um** worker. Este é o padrão fundamental por trás de qualquer sistema de processamento assíncrono escalável.

---

## Objetivo de aprendizado

- Como o RabbitMQ distribui mensagens em **round-robin** entre múltiplos consumers da mesma fila.
- Por que **auto-ack não é seguro** para tarefas que realmente importam.
- O que é **ack manual** (`basicAck` / `basicNack`) e como ele garante *at-least-once delivery*.
- Para que serve o **prefetch** (`basicQos`) e como ele evita sobrecarregar um worker lento.
- O que acontece quando um worker morre no meio do processamento — e por que a mensagem não se perde.

---

## Conceitos envolvidos

| Conceito | Onde aparece no código |
|---|---|
| Múltiplos consumers na mesma fila | Rodar `RelatorioWorker` várias vezes |
| Ack manual | `channel.basicAck(deliveryTag, false)` |
| Nack com requeue | `channel.basicNack(deliveryTag, false, true)` |
| Prefetch / QoS | `channel.basicQos(1)` |
| Distribuição round-robin | Comportamento padrão do RabbitMQ para múltiplos consumers |

> ℹEste projeto introduz o mecanismo de confiabilidade que faltava no Projeto 1. Se uma mensagem não for confirmada (por queda do worker, exceção não tratada etc.), o RabbitMQ a devolve automaticamente para a fila — nenhuma tarefa se perde.

---

## Estrutura do projeto

```
projeto-2-work-queue/
├── pom.xml
├── docker-compose.yml
└── src/
    └── main/
        └── java/
            └── com/estudo/rabbitmq/workqueue/
                ├── RelatorioProducer.java
                └── RelatorioWorker.java
```

---

## Pré-requisitos

- **JDK 11**
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

- **AMQP** (usado pela aplicação Java): `localhost:5672`
- **Management UI** (interface web): [http://localhost:15672](http://localhost:15672) — login `admin` / `admin`

> Se você já tem o RabbitMQ do Projeto 1 rodando, pode reaproveitar o mesmo container — as filas têm nomes diferentes, não há conflito.

### 2. Compile o projeto

```bash
mvn clean compile
```

### 3. Suba 3 workers (em 3 terminais separados)

```bash
mvn exec:java -Dexec.mainClass="com.estudo.rabbitmq.workqueue.RelatorioWorker"
```

Cada terminal deve mostrar:

```
 [*] Worker pronto. Aguardando tarefas...
```

### 4. Rode o Producer

Em um quarto terminal:

```bash
mvn exec:java -Dexec.mainClass="com.estudo.rabbitmq.workqueue.RelatorioProducer"
```

Isso publica 20 mensagens de uma vez. Observe os 3 terminais dos workers — as tarefas vão se distribuir entre eles, cada mensagem processada por apenas um worker:

```
 [x] Processando: Relatorio de vendas #1
 [v] Concluido: Relatorio de vendas #1
```

---

## Experimento sugerido: matar um worker no meio do processamento

Este é o experimento mais importante do projeto:

1. Com o producer publicando mensagens, escolha um worker e derrube-o (`Ctrl+C`) **enquanto ele está processando** (ou seja, durante o `Thread.sleep` simulando trabalho).
2. Observe na Management UI (aba **Queues and Streams** → `fila.relatorios`) que a mensagem que estava com aquele worker desaparece da coluna **Unacked** e volta para **Ready**.
3. Um dos workers restantes pega essa mensagem e a processa do zero.

Isso demonstra, na prática, a garantia de **at-least-once delivery**: nenhuma tarefa é perdida quando um consumer cai, mas ela pode ser reprocessada.

---

## O que observar na Management UI

Acesse [http://localhost:15672](http://localhost:15672) enquanto testa:

1. Vá em **Queues and Streams** → `fila.relatorios` e acompanhe os gráficos de **Ready**, **Unacked** e **Total**.
2. Publique as 20 mensagens com os 3 workers ativos e veja o número de **Unacked** oscilando entre 0 e 3 (por causa do `prefetch=1` — cada worker só recebe uma nova mensagem depois de confirmar a anterior).
3. Troque `channel.basicQos(1)` por `channel.basicQos(5)` no código, recompile e repita o teste — note que agora cada worker pode acumular até 5 mensagens não confirmadas, mudando o padrão de distribuição.
