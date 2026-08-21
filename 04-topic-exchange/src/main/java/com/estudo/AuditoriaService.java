package com.estudo;

import com.rabbitmq.client.*;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AuditoriaService {
    private static final String EXCHANGE_NAME = "pedidos.eventos";

    public static void main(String[] args) throws Exception {
        var factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setUsername("admin");
        factory.setPassword("admin");

        // Virtual threads dedicadas ao processamento de cada mensagem:
        // o callback do RabbitMQ dispara rapido, e o "trabalho pesado"
        // de auditoria roda em uma virtual thread barata
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.TOPIC, true);

        String filaTemp = channel.queueDeclare().getQueue(); // fila exclusiva/temporaria
        channel.queueBind(filaTemp, EXCHANGE_NAME, "#"); // # = todos os eventos

        System.out.println(" [*] AuditoriaService (assina TUDO) aguardando...");

        DeliverCallback callback = (tag, delivery) -> {
            String json = new String(delivery.getBody(), StandardCharsets.UTF_8);
            String routingKey = delivery.getEnvelope().getRoutingKey();

            executor.submit(() -> {
                System.out.println(" [auditoria] [" + routingKey + "] " + json);
            });

            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        };

        channel.basicConsume(filaTemp, false, callback, t -> {});
    }
}
