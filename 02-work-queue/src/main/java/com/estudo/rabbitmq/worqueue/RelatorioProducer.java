package com.estudo.rabbitmq.worqueue;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;

import java.nio.charset.StandardCharsets;

public class RelatorioProducer {
    private static final String QUEUE_NAME = "fila.relatorios";

    public static void main(String[] args) throws Exception {
        var factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setUsername("admin");
        factory.setPassword("admin");

        try (
                Connection connection = factory.newConnection();
                Channel channel = connection.createChannel();
                ) {
            channel.queueDeclare(QUEUE_NAME, true, false, false, null);

            for (int i = 1; i <= 20; i++) {
                String mensagem = "Relatorio de vendas #" + i;
                channel.basicPublish(
                        "",
                        QUEUE_NAME,
                        MessageProperties.PERSISTENT_TEXT_PLAIN,
                        mensagem.getBytes(StandardCharsets.UTF_8)
                );
                System.out.println(" [x] Solicitado: " + mensagem);
            }
        }
    }
}
