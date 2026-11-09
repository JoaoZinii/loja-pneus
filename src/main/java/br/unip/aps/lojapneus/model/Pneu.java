package br.unip.aps.lojapneus.model;

import java.math.BigDecimal;

/**
 * Representa um pneu do estoque (entidade da tabela pneu).
 */
public class Pneu {

    private int id;
    private String marca;
    private String modelo;
    private int aro;
    private int estoque;
    private BigDecimal preco;

    /**
     * Construtor para cadastrar um pneu novo.
     * O id não é informado: quem gera é o banco (AUTO_INCREMENT).
     *
     * @param marca  marca do pneu
     * @param modelo modelo do pneu
     * @param aro    tamanho do aro (de 10 a 24)
     * @param estoque quantidade disponível em estoque
     * @param preco  preço de venda
     */
    public Pneu(String marca, String modelo, int aro,
                int estoque, BigDecimal preco) {
        this.marca = marca;
        this.modelo = modelo;
        this.aro = aro;
        this.estoque = estoque;
        this.preco = preco;
    }

    /**
     * Construtor para montar um pneu a partir de uma linha lida do banco.
     *
     * @param id      id gerado pelo banco
     * @param marca   marca do pneu
     * @param modelo  modelo do pneu
     * @param aro     tamanho do aro
     * @param estoque quantidade disponível em estoque
     * @param preco   preço de venda
     */
    public Pneu(int id, String marca, String modelo, int aro,
                int estoque, BigDecimal preco) {
        this(marca, modelo, aro, estoque, preco);
        this.id = id;
    }

    /** @return o id do pneu */
    public int getId() { return id; }

    /** @param id o id gerado pelo banco após o cadastro */
    public void setId(int id) { this.id = id; }

    /** @return a marca do pneu */
    public String getMarca() { return marca; }

    /** @param marca a nova marca */
    public void setMarca(String marca) { this.marca = marca; }

    /** @return o modelo do pneu */
    public String getModelo() { return modelo; }

    /** @param modelo o novo modelo */
    public void setModelo(String modelo) { this.modelo = modelo; }

    /** @return o tamanho do aro */
    public int getAro() { return aro; }

    /** @param aro o novo tamanho de aro */
    public void setAro(int aro) { this.aro = aro; }

    /** @return a quantidade em estoque */
    public int getEstoque() { return estoque; }

    /** @param estoque a nova quantidade em estoque */
    public void setEstoque(int estoque) { this.estoque = estoque; }

    /** @return o preço de venda */
    public BigDecimal getPreco() { return preco; }

    /** @param preco o novo preço de venda */
    public void setPreco(BigDecimal preco) { this.preco = preco; }

    /** @return texto curto para exibir o pneu em listas e combos */
    @Override
    public String toString() {
        return marca + " " + modelo + " (aro " + aro + ")";
    }
}