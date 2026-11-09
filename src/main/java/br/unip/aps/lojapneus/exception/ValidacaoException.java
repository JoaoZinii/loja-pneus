package br.unip.aps.lojapneus.exception;

/**
 * Erro de validação: o usuário digitou algo que não passa nas regras do
 * sistema (campo vazio, letra em campo numérico, valor negativo etc.).
 *
 * <p>Quem lança é o Controller, ANTES de qualquer acesso ao banco. A exceção
 * guarda duas informações:</p>
 * <ul>
 *   <li>o <b>campo</b> que falhou (ex.: "nome"), para a tela poder dar foco
 *       ou destacar o campo certo. Pode ser {@code null} quando o erro não é
 *       de um campo específico (ex.: "Selecione um cliente na lista.");</li>
 *   <li>a <b>mensagem</b> pronta para mostrar ao usuário.</li>
 * </ul>
 *
 * <p>É uma exceção <i>checked</i> (estende {@link Exception}): o compilador
 * obriga a tela a tratá-la com try/catch, então ninguém esquece de mostrar
 * o erro. É o mesmo tipo da {@link PersistenciaException}.</p>
 */
public class ValidacaoException extends Exception {

    private static final long serialVersionUID = 1L;

    /** Identificador do campo que falhou, ou null se não for um campo. */
    private final String campo;

    /**
     * Cria a exceção de validação.
     *
     * @param campo    identificador do campo que falhou (ex.: "nome"),
     *                 ou null se o erro não for de um campo específico
     * @param mensagem mensagem para o usuário (ex.: "Informe o nome do cliente.")
     */
    public ValidacaoException(String campo, String mensagem) {
        super(mensagem);
        this.campo = campo;
    }

    /**
     * Informa qual campo falhou, para a tela poder dar foco nele.
     *
     * @return o identificador do campo, ou null se o erro não for de um campo
     */
    public String getCampo() {
        return campo;
    }
}
