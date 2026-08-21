# Projeto 1 — Hello World (RabbitMQ + Java 8)

Primeiro projeto da série de estudos práticos de mensageria com RabbitMQ. Aqui o objetivo é o mais simples possível: um **producer** publica uma mensagem em uma fila, e um **consumer** a recebe (sem roteamento, sem ack manual, sem concorrência). Só o fluxo básico.

---

## Objetivo de aprendizado

- Como abrir uma **connection** e um **channel** com o RabbitMQ a partir do Java.
- Que o producer **nunca publica diretamente em uma fila** — ele sempre publica em uma *exchange* (mesmo que seja a exchange default, implícita).
- Como declarar uma fila de forma **idempotente** (`queueDeclare`).
- A diferença entre publicar uma mensagem **persistente** e uma volátil.
- Como consumir mensagens de forma assíncrona com `DeliverCallback`.

---

## Conceitos envolvidos

| Conceito | Onde aparece no código |
|---|---|
| `ConnectionFactory` / `Connection` / `Channel` | Ambas as classes |
| Default Exchange (exchange sem nome `""`) | `PedidoProducer.basicPublish("", ...)` |
| Fila durável (`durable=true`) | `queueDeclare` em ambas as classes |
| Mensagem persistente | `MessageProperties.PERSISTENT_TEXT_PLAIN` |
| Consumo assíncrono (`autoAck=true`) | `PedidoConsumer.basicConsume` |

> Este projeto usa **auto-ack** de propósito, só para simplificar o primeiro contato. Ack manual (a forma recomendada em produção) é introduzido no **Projeto 2 — Work Queue**.

---

## Estrutura do projeto

```
projeto-1-hello-world/
├── pom.xml
├── docker-compose.yml
└── src/
    └── main/
        └── java/
            └── com/estudo/rabbitmq/hello/
                ├── PedidoProducer.java
                └── PedidoConsumer.java
```

---

## Pré-requisitos

- **JDK 8**
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

### 2. Compile o projeto

```bash
mvn clean compile
```

### 3. Rode o Consumer primeiro

Em um terminal:

```bash
mvn exec:java -Dexec.mainClass="com.estudo.rabbitmq.hello.PedidoConsumer"
```

Você verá:

```
 [*] Aguardando mensagens. CTRL+C para sair.
```

### 4. Rode o Producer em outro terminal

```bash
mvn exec:java -Dexec.mainClass="com.estudo.rabbitmq.hello.PedidoProducer"
```

Você verá no terminal do **producer**:

```
 [x] Enviado: 'Novo pedido: #1001 - Notebook Gamer'
```

E, quase instantaneamente, no terminal do **consumer**:

```
 [x] Recebido: 'Novo pedido: #1001 - Notebook Gamer'
```

---

## O que observar na Management UI

Acesse [http://localhost:15672](http://localhost:15672) enquanto testa:

1. Vá em **Queues and Streams** e veja a fila `fila.pedidos.simples` aparecer.
2. Pare o consumer (`Ctrl+C`) e rode o producer 2 ou 3 vezes seguidas — observe o contador **Ready** da fila subindo.
3. Rode o consumer novamente e veja as mensagens sendo consumidas e o contador zerando.

Isso demonstra visualmente o desacoplamento de tempo entre producer e consumer (a mensagem fica esperando no broker até que alguém a busque, não importa quanto tempo isso leve).
