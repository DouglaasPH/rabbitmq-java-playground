package com.estudo;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static com.estudo.TopologiaSetup.*;

public class PagamentoProducer {
    public static void main(String[] args) throws Exception {
        var factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setUsername("admin");factory.setPassword("admin");

        try (
                Connection connection = factory.newConnection();
                Channel channel = connection.createChannel()
        ) {
            declarar(channel);
            for (int i = 1; i <= 5; i++) {
                String payload = "{\"pedidoId\":\"PED-30" + i + "\",\"valor\":199.90}";
                var props = MessageProperties
                        .PERSISTENT_TEXT_PLAIN
                        .builder()
                        .messageId(UUID.randomUUID().toString())
                        .build();
                channel.basicPublish(EXCHANGE_PRINCIPAL,
                        ROUTING_KEY,
                        props,payload.getBytes(StandardCharsets.UTF_8)
                );
                System.out.println(" [x] Pagamento enviado: " + payload);
            }
        }
    }
}