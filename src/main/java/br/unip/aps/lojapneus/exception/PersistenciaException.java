package br.unip.aps.lojapneus.exception;

/**
 * Erro ocorrido na camada de persistência (acesso ao banco).
 * Guarda o código de erro do MySQL para o Controller traduzir.
 */
public class PersistenciaException extends Exception {

    /** Código próprio: a venda pediu mais pneus do que há em estoque. */
    public static final int ESTOQUE_INSUFICIENTE = -1;

    private final int codigoErro;

    /**
     * @param mensagem   descrição do erro
     * @param codigoErro código de erro do MySQL (0 se não houver)
     * @param causa      exceção original (SQLException)
     */
    public PersistenciaException(String mensagem, int codigoErro, Throwable causa) {
        super(mensagem, causa);
        this.codigoErro = codigoErro;
    }

    /** @return o código de erro do MySQL, ou um código próprio (negativo) */
    public int getCodigoErro() { return codigoErro; }
}