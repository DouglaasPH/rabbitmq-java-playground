# Projeto 3 — Publish/Subscribe Fanout (RabbitMQ + Java 17)

Terceiro projeto da série de estudos práticos de mensageria com RabbitMQ. Aqui o objetivo é entender o padrão **Publish/Subscribe**: um evento de domínio é publicado uma única vez, e **múltiplos serviços independentes** recebem cada um sua própria cópia da mensagem — sem que o producer saiba (ou precise saber) quem são os consumidores.

---

## Objetivo de aprendizado

- A diferença fundamental entre **Work Queue** (uma mensagem → um worker) e **Fanout** (uma mensagem → cópia para todas as filas vinculadas).
- Como funciona a exchange do tipo **fanout**, que ignora completamente a routing key.
- Por que cada serviço interessado precisa **declarar e vincular sua própria fila** à exchange.
- Por que a ordem de inicialização importa: quem não está com a fila pronta no momento da publicação perde aquele evento.

---

## Conceitos envolvidos

| Conceito | Onde aparece no código |
|---|---|
| Exchange do tipo `fanout` | `channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.FANOUT, true)` |
| Binding (fila ↔ exchange) | `channel.queueBind(QUEUE_NAME, EXCHANGE_NAME, "")` |
| Routing key ignorada | `basicPublish(EXCHANGE_NAME, "", ...)` — string vazia, fanout não usa isso |
| Text block para serialização JSON manual | `PedidoCriado.toJson()` |

> Diferente do Projeto 2, aqui **cada consumer tem sua própria fila** (`fila.email-service`, `fila.estoque-service`, `fila.analytics-service`). Isso é o que garante que todos recebam uma cópia completa do evento, em vez de competir pela mesma mensagem.

---

## Estrutura do projeto

```
projeto-3-fanout/
├── pom.xml
├── docker-compose.yml
└── src/
    └── main/
        └── java/
            └── com/estudo/rabbitmq/fanout/
                ├── PedidoCriado.java
                ├── PedidoCriadoProducer.java
                ├── EmailService.java
                ├── EstoqueService.java
                └── AnalyticsService.java
```

---

## Pré-requisitos

- **JDK 17** instalado e configurado (`java -version` deve mostrar `17.x`)
- **Maven** 3.6+
- **Docker** e **Docker Compose** (para subir o RabbitMQ localmente)

---

## Como rodar

### 1. Suba o RabbitMQ

```bash
docker compose up -d
```

- **AMQP**: `localhost:5672`
- **Management UI**: [http://localhost:15672](http://localhost:15672) — login `admin` / `admin`

> Se já tem o RabbitMQ de outro projeto rodando, pode reaproveitar — as exchanges e filas têm nomes diferentes, não há conflito.

### 2. Compile o projeto

```bash
mvn clean compile
```

### 3. Suba os três consumers (em três terminais separados)

```bash
mvn exec:java -Dexec.mainClass="com.estudo.rabbitmq.fanout.EmailService"
```

```bash
mvn exec:java -Dexec.mainClass="com.estudo.rabbitmq.fanout.EstoqueService"
```

```bash
mvn exec:java -Dexec.mainClass="com.estudo.rabbitmq.fanout.AnalyticsService"
```

Cada terminal deve mostrar algo como:

```
 [*] EmailService aguardando eventos...
```

### 4. Rode o Producer

Em um quarto terminal:

```bash
mvn exec:java -Dexec.mainClass="com.estudo.rabbitmq.fanout.PedidoCriadoProducer"
```

Você verá:

```
 [x] Evento publicado: PedidoCriado[pedidoId=PED-2001, cliente=Joana Silva, total=459.90]
```

E, quase simultaneamente, **os três consumers** vão reagir ao mesmo evento, cada um no seu terminal:

```
 [email] Enviando confirmacao para: {"pedidoId":"PED-2001","cliente":"Joana Silva","total":459.90}
 [estoque] Baixando estoque para: {"pedidoId":"PED-2001","cliente":"Joana Silva","total":459.90}
 [analytics] Registrando metrica para: {"pedidoId":"PED-2001","cliente":"Joana Silva","total":459.90}
```

---

## Experimento sugerido: ordem de inicialização importa

Este é o ponto mais importante do padrão fanout:

1. Suba **apenas** `EmailService` e `EstoqueService` (não suba o `AnalyticsService` ainda).
2. Rode o `PedidoCriadoProducer` — os dois consumers ativos recebem o evento normalmente.
3. Agora suba o `AnalyticsService`. Ele **não** recebe o evento que já foi publicado antes de existir.
4. Rode o producer de novo — agora sim, os três recebem.

Isso mostra que fanout é **"ao vivo"**: um serviço só recebe eventos publicados a partir do momento em que sua fila (durável) já existia e estava vinculada à exchange.

---

## O que observar na Management UI

Acesse [http://localhost:15672](http://localhost:15672) enquanto testa:

1. Vá em **Exchanges** e clique em `pedidos.criados` — na aba **Bindings**, veja as três filas vinculadas.
2. Vá em **Queues and Streams** e confirme que existem três filas distintas: `fila.email-service`, `fila.estoque-service`, `fila.analytics-service`.
3. Publique um evento e observe, em tempo real, o contador de mensagens subindo simultaneamente nas três filas — a prova visual de que a mesma mensagem foi copiada para todas.
