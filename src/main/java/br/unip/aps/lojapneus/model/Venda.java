package br.unip.aps.lojapneus.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Representa uma venda (entidade da tabela venda).
 * É a tabela associativa: liga um cliente a um pneu.
 */
public class Venda {

    private int id;
    private Cliente cliente;
    private Pneu pneu;
    private int quantidade;
    private BigDecimal precoUnitario;
    private LocalDateTime dataVenda;

    /**
     * Construtor para registrar uma venda nova.
     * O id não é informado: quem gera é o banco (AUTO_INCREMENT).
     *
     * @param cliente       cliente que comprou
     * @param pneu          pneu vendido
     * @param quantidade    quantidade vendida
     * @param precoUnitario preço de cada pneu no momento da venda
     * @param dataVenda     data e hora da venda
     */
    public Venda(Cliente cliente, Pneu pneu, int quantidade,
                 BigDecimal precoUnitario, LocalDateTime dataVenda) {
        this.cliente = cliente;
        this.pneu = pneu;
        this.quantidade = quantidade;
        this.precoUnitario = precoUnitario;
        this.dataVenda = dataVenda;
    }

    /**
     * Construtor para montar uma venda a partir de uma linha lida do banco.
     *
     * @param id            id gerado pelo banco
     * @param cliente       cliente que comprou
     * @param pneu          pneu vendido
     * @param quantidade    quantidade vendida
     * @param precoUnitario preço de cada pneu no momento da venda
     * @param dataVenda     data e hora da venda
     */
    public Venda(int id, Cliente cliente, Pneu pneu, int quantidade,
                 BigDecimal precoUnitario, LocalDateTime dataVenda) {
        this(cliente, pneu, quantidade, precoUnitario, dataVenda);
        this.id = id;
    }

    /** @return o id da venda */
    public int getId() { return id; }

    /** @param id o id gerado pelo banco após o registro */
    public void setId(int id) { this.id = id; }

    /** @return o cliente da venda */
    public Cliente getCliente() { return cliente; }

    /** @param cliente o cliente da venda */
    public void setCliente(Cliente cliente) { this.cliente = cliente; }

    /** @return o pneu vendido */
    public Pneu getPneu() { return pneu; }

    /** @param pneu o pneu vendido */
    public void setPneu(Pneu pneu) { this.pneu = pneu; }

    /** @return a quantidade vendida */
    public int getQuantidade() { return quantidade; }

    /** @param quantidade a nova quantidade vendida */
    public void setQuantidade(int quantidade) { this.quantidade = quantidade; }

    /** @return o preço unitário praticado na venda */
    public BigDecimal getPrecoUnitario() { return precoUnitario; }

    /** @param precoUnitario o novo preço unitário */
    public void setPrecoUnitario(BigDecimal precoUnitario) {
        this.precoUnitario = precoUnitario;
    }

    /** @return a data e hora da venda */
    public LocalDateTime getDataVenda() { return dataVenda; }

    /** @param dataVenda a nova data e hora */
    public void setDataVenda(LocalDateTime dataVenda) {
        this.dataVenda = dataVenda;
    }

    /**
     * Calcula o valor total da venda (preço unitário x quantidade).
     * Não é guardado no banco, porque pode ser calculado a qualquer momento.
     *
     * @return o valor total da venda
     */
    public BigDecimal getValorTotal() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    /** @return texto resumido da venda */
    @Override
    public String toString() {
        return "Venda " + id + ": " + cliente + " - " + pneu
                + " x" + quantidade;
    }
}