package com.estudo;

import com.rabbitmq.client.*;

import java.nio.charset.StandardCharsets;

public class NotificacaoBrService {
    private static final String EXCHANGE_NAME = "pedidos.eventos";

    public static void main(String[] args) throws Exception {
        var factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setUsername("admin");
        factory.setPassword("admin");

        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.TOPIC, true);

        String filaTemp = channel.queueDeclare().getQueue();
        // pedido.*.br -> "pedido.criado.br", "pedido.cancelado.br", mas NAO "pedido.criado.us"
        channel.queueBind(filaTemp, EXCHANGE_NAME, "pedido.*.br");

        System.out.println(" [*] NotificacaoBrService (so BR) aguardando...");

        DeliverCallback callback = (tag, delivery) -> {
            String json = new String(delivery.getBody(), StandardCharsets.UTF_8);
            System.out.println(" [notificacao-br] " + json);
            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        };

        channel.basicConsume(filaTemp, false, callback, t -> {});
    }
}
