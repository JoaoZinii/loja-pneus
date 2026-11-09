package br.unip.aps.lojapneus.model;

/**
 * Representa um cliente da loja (entidade da tabela cliente).
 */
public class Cliente {

    private int id;
    private String nome;
    private String email;
    private String telefone;
    private String endereco;

    /**
     * Construtor para cadastrar um cliente novo.
     * O id não é informado: quem gera é o banco (AUTO_INCREMENT).
     *
     * @param nome     nome do cliente (obrigatório)
     * @param email    e-mail (opcional; deve ser único)
     * @param telefone telefone, só dígitos com DDD (opcional)
     * @param endereco endereço (opcional)
     */
    public Cliente(String nome, String email, String telefone, String endereco) {
        this.nome = nome;
        this.email = email;
        this.telefone = telefone;
        this.endereco = endereco;
    }

    /**
     * Construtor para montar um cliente a partir de uma linha lida do banco.
     *
     * @param id       id gerado pelo banco
     * @param nome     nome do cliente
     * @param email    e-mail
     * @param telefone telefone
     * @param endereco endereço
     */
    public Cliente(int id, String nome, String email, String telefone,
                   String endereco) {
        this(nome, email, telefone, endereco);
        this.id = id;
    }

    /** @return o id do cliente */
    public int getId() { return id; }

    /** @param id o id gerado pelo banco após o cadastro */
    public void setId(int id) { this.id = id; }

    /** @return o nome do cliente */
    public String getNome() { return nome; }

    /** @param nome o novo nome */
    public void setNome(String nome) { this.nome = nome; }

    /** @return o e-mail do cliente */
    public String getEmail() { return email; }

    /** @param email o novo e-mail */
    public void setEmail(String email) { this.email = email; }

    /** @return o telefone do cliente */
    public String getTelefone() { return telefone; }

    /** @param telefone o novo telefone */
    public void setTelefone(String telefone) { this.telefone = telefone; }

    /** @return o endereço do cliente */
    public String getEndereco() { return endereco; }

    /** @param endereco o novo endereço */
    public void setEndereco(String endereco) { this.endereco = endereco; }

    /** @return o nome, para exibir o cliente em listas e combos */
    @Override
    public String toString() {
        return nome;
    }
}