package com.estudo;

import com.rabbitmq.client.*;

import java.nio.charset.StandardCharsets;

public class EmailService {
    private static final String EXCHANGE_NAME = "pedidos.criados";
    private static final String QUEUE_NAME = "fila.email-service";

    public static void main(String[] args) throws Exception {
        var factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setUsername("admin");
        factory.setPassword("admin");

        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.FANOUT, true);
        channel.queueDeclare(QUEUE_NAME, true, false, false, null);
        channel.queueBind(QUEUE_NAME, EXCHANGE_NAME, "");

        System.out.println(" [*] EmailService aguardando eventos...");

        DeliverCallback callback = (tag, delivery) -> {
            String json = new String(delivery.getBody(), StandardCharsets.UTF_8);
            System.out.println(" [email] Enviando confirmacao para: " + json);
            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        };

        channel.basicQos(1);
        channel.basicConsume(QUEUE_NAME, false, callback, tag -> {});
    }
}
