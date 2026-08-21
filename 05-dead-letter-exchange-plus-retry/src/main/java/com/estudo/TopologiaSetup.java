package com.estudo;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;

import java.util.Map;

public class TopologiaSetup {
    static final String EXCHANGE_PRINCIPAL = "pagamentos.exchange";
    private static final String EXCHANGE_DLX = "dlx.pagamentos";
    private static final String FILA_PRINCIPAL = "fila.pagamentos";
    public static final String FILA_ESPERA = "fila.pagamentos.espera";
    static final String ROUTING_KEY = "pagamentos.processar";

    public static void declarar(Channel channel) throws Exception {
        channel.exchangeDeclare(EXCHANGE_PRINCIPAL, BuiltinExchangeType.DIRECT, true);
        channel.exchangeDeclare(EXCHANGE_DLX, BuiltinExchangeType.DIRECT, true);

        // Fila principal: se uma mensagem for rejeitada (nack, requeue=false),
        // ela cai automaticamente na exchange de DLX
        channel.queueDeclare(FILA_PRINCIPAL, true, false, false, Map.of(
                "x-dead-letter-exchange", EXCHANGE_DLX,
                "x-dead-letter-routing-key", ROUTING_KEY
        ));
        channel.queueBind(FILA_PRINCIPAL, EXCHANGE_PRINCIPAL, ROUTING_KEY);

        // Fila de espera: TTL fixo de 5s, SEM consumer.
        // Quando expira, devolve para a exchange principal (backoff simples)
        channel.queueDeclare(FILA_ESPERA, true, false, false, Map.of(
                "x-dead-letter-exchange", EXCHANGE_PRINCIPAL,
                "x-dead-letter-routing-key", ROUTING_KEY,
                "x-message-ttl", 5000
        ));
        channel.queueBind(FILA_ESPERA, EXCHANGE_DLX,ROUTING_KEY);
    }
}
