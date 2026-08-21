package com.estudo;

import java.math.BigDecimal;

public class PedidoCriado {
    private String pedidoId;
    private String cliente;
    private BigDecimal total;

    public PedidoCriado(String pedidoId, String cliente, BigDecimal total) {
        this.pedidoId = pedidoId;
        this.cliente = cliente;
        this.total = total;
    }

    public String getPedidoId() {
        return pedidoId;
    }

    public void setPedidoId(String pedidoId) {
        this.pedidoId = pedidoId;
    }

    public String getCliente() {
        return cliente;
    }

    public void setCliente(String cliente) {
        this.cliente = cliente;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    // Serializacao manual simples (sem lib externa) usando text block
    public String toJson() {
        return """
                {"pedidoId":"%s","cliente":"%s","total":%s}"""
                .formatted(pedidoId, cliente, total.toPlainString());
    }
}
