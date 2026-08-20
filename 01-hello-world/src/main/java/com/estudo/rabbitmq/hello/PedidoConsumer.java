package com.estudo.rabbitmq.hello;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.nio.charset.StandardCharsets;

public class PedidoConsumer {
    private static final String QUEUE_NAME = "fila.pedidos.simples";

    public static void main(String[] args) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setUsername("admin");
        factory.setPassword("admin");

        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.queueDeclare(QUEUE_NAME, true, false, false, null);

        System.out.println(" [*] Aguardando mensagens. CTRL+C para sair.");

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String mensagem = new String(delivery.getBody(), StandardCharsets.UTF_8);
            System.out.println(" [x] Recebido: '" + mensagem + "'");
        };

        channel.basicConsume(QUEUE_NAME, true, deliverCallback, consumerTag -> {});
    }
}
