package com.estudo.rabbitmq.hello;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;

import java.nio.charset.StandardCharsets;

public class PedidoProducer {
    private static final String QUEUE_NAME = "fila.pedidos.simples";

    public static void main(String[] args) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setUsername("admin");
        factory.setPassword("admin");

        try (
                Connection connection = factory.newConnection();
                Channel channel = connection.createChannel()
        ) {
            channel.queueDeclare(QUEUE_NAME, true, false, false, null);
            String mensagem = "Novo pedido: #1001 - Notebook Gamer";

            channel.basicPublish(
                    "",     // default exchange
                    QUEUE_NAME,
                    MessageProperties.PERSISTENT_TEXT_PLAIN,
                    mensagem.getBytes(StandardCharsets.UTF_8)
            );

            System.out.println(" [x] Enviado: '" + mensagem + "'");
        }
    }
}
