package com.estudo;

import com.rabbitmq.client.*;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

public class PedidoCriadoProducer {
    private static final String EXCHANGE_NAME = "pedidos.criados";

    public static void main(String[] args) throws Exception {
        var factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setUsername("admin");
        factory.setPassword("admin");

        try (
                Connection connection = factory.newConnection();
                Channel channel = connection.createChannel()
        ) {
            channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.FANOUT, true);

            var evento = new PedidoCriado("PED-2001", "Joana Silva", new BigDecimal("459.90"));

            channel.basicPublish(
                    EXCHANGE_NAME,
                    "", // fanout ignora routing key
                    MessageProperties.PERSISTENT_TEXT_PLAIN,
                    evento.toJson().getBytes(StandardCharsets.UTF_8)
            );

            System.out.println(" [x] Evento publicado: " + evento);
        }
    }
}