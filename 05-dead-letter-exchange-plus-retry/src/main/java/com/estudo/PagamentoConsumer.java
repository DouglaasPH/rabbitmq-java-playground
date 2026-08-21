package com.estudo;

import com.rabbitmq.client.*;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.StructuredTaskScope;

public class PagamentoConsumer {
    private static final int MAX_TENTATIVAS = 3;

    public static void main(String[] args) throws Exception {
        var factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setUsername("admin");
        factory.setPassword("admin");

        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        TopologiaSetup.declarar(channel);   // <-- monta a topologia (exchanges/filas)
        channel.basicQos(1);

        System.out.println(" [*] PagamentoConsumer aguardando...");

        DeliverCallback callback = (tag, delivery) -> {
            String payload = new String(delivery.getBody(), StandardCharsets.UTF_8);
            long deliveryTag = delivery.getEnvelope().getDeliveryTag();
            int tentativas = contarTentativas(delivery);

            try {
                boolean sucesso = processarComTimeout(payload);

                if (sucesso) {
                    System.out.println(" [v] Pagamento aprovado: " + payload);
                    channel.basicAck(deliveryTag, false);
                } else if (tentativas >= MAX_TENTATIVAS) {
                    System.out.println(" [x] Desistindo apos " + tentativas + " tentativas: " + payload);
                    channel.basicAck(deliveryTag, false); // desiste
                } else {
                    System.out.println(" [!] Falha, enviando para retry: " + payload);
                    channel.basicNack(deliveryTag, false, false); // vai para DLX
                }
            } catch (Exception e) {
                System.out.println(" [x] Excecao no processamento: " + e.getMessage());
                channel.basicNack(deliveryTag, false, false);
            }
        };

        channel.basicConsume("fila.pagamentos", false, callback, t -> {});
    }

    // Simula processamento de pagamento com timeout de 2s, usando
    // structured concurrency do Java 25: a subtarefa e o timeout
    // sao tratados como uma unica unidade de trabalho
    private static boolean processarComTimeout(String payload) throws Exception {
        try (var scope = StructuredTaskScope.open(
                StructuredTaskScope.Joiner.<Boolean>anySuccessfulResultOrThrow(),
                cfg -> cfg.withTimeout(Duration.ofSeconds(2)))
        ) {
            scope.fork(() -> {
                Thread.sleep(300);
                // simula chamada a um gateway de pagamento
                // simula falha aleatoria (~40% de chance)
                return Math.random() > 0.4;
            });
            return scope.join();
        }
    }

    private static int contarTentativas(Delivery delivery) {
        Object header = delivery.getProperties().getHeaders() == null ? null
                : delivery.getProperties().getHeaders().get("x-death");

        // Simplificacao: em producao, leia o array "x-death" do RabbitMQ
        // para contar quantas vezes a mensagem passou pela DLX.
        // Aqui, para fins didaticos, contamos via x-death.count quando presente.
        if (header == null) return 1;
        return 2; // placeholder didatico - ver observacao abaixo}
    }
}
