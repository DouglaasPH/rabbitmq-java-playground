package com.estudo.rabbitmq.worqueue;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.nio.charset.StandardCharsets;

public class RelatorioWorker {
    private static final String QUEUE_NAME = "fila.relatorios";

    public static void main(String[] args) throws Exception {
        var factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setUsername("admin");
        factory.setPassword("admin");

        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.queueDeclare(QUEUE_NAME, true, false, false, null);

        // Cada worker so recebe 1 mensagem nao confirmada por vez:
        // garante distribuição justa entre varias instancias
        channel.basicQos(1);

        System.out.println(" [*] Worker pronto. Aguardando tarefas...");

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String mensagem = new String(delivery.getBody(), StandardCharsets.UTF_8);

            try {
                System.out.println(" [x] Processando: " + mensagem);
                // simula trabalho pesado, tempo variavel
                var tempoProcessamento = 500 + (long) (Math.random() * 2000);
                Thread.sleep(tempoProcessamento);
                System.out.println(" [v] Concluido: " + mensagem);

                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            } catch (InterruptedException e) {
                // requeue = true: falha temporaria, devolve para a fila
                channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, true);
            }
        };

        channel.basicConsume(QUEUE_NAME, false, deliverCallback, consumerTag -> {});
    }
}
