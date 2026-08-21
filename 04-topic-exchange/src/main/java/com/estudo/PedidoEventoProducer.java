package com.estudo;

import com.rabbitmq.client.*;

import java.nio.charset.StandardCharsets;

public class PedidoEventoProducer {
    private static final String EXCHANGE_NAME = "pedidos.eventos";

    public static void main(String[] args) throws Exception {
        var factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setUsername("admin");
        factory.setPassword("admin");

        try (
                Connection connection = factory.newConnection();
                Channel channel = connection.createChannel();
        ) {
            channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.TOPIC, true);
            publicar(channel, "pedido.criado.br",  "{\"pedidoId\":\"PED-01\",\"pais\":\"BR\"}");
            publicar(channel, "pedido.criado.us", "{\"pedidoId\":\"PED-02\",\"pais\":\"US\"}");
            publicar(channel, "pedido.cancelado.br", "{\"pedidoId\":\"PED-01\",\"motivo\":\"estoque\"}");
        }
    }

    private static void publicar(Channel channel, String routingKey, String json) throws Exception {
        channel.basicPublish(
                EXCHANGE_NAME,
                routingKey,
                MessageProperties.PERSISTENT_TEXT_PLAIN,
                json.getBytes(StandardCharsets.UTF_8)
                );
        System.out.println(" [x] Publicado [" + routingKey + "] " + json);
    }
}
