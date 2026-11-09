package br.unip.aps.lojapneus.controller;

import java.util.List;

import br.unip.aps.lojapneus.dao.ClienteDAO;
import br.unip.aps.lojapneus.dao.ClienteDAOJdbc;
import br.unip.aps.lojapneus.exception.PersistenciaException;
import br.unip.aps.lojapneus.exception.ValidacaoException;
import br.unip.aps.lojapneus.model.Cliente;
import br.unip.aps.lojapneus.model.ClienteOrdem;
import br.unip.aps.lojapneus.util.Validador;

/**
 * Controller de clientes: a ponte entre a tela de clientes e o banco.
 *
 * <p>Todo método segue o mesmo caminho:</p>
 * <ol>
 *   <li>recebe os valores da tela como texto, do jeito que vieram dos campos;</li>
 *   <li>valida cada campo e, se algo falhar, lança {@link ValidacaoException}
 *       e para (nada chega ao banco);</li>
 *   <li>converte e monta o objeto {@link Cliente};</li>
 *   <li>chama o DAO;</li>
 *   <li>se o DAO lançar {@link PersistenciaException}, troca a mensagem
 *       técnica por uma amigável e relança;</li>
 *   <li>devolve o resultado para a tela exibir.</li>
 * </ol>
 *
 * <p>O controller não abre janelas e não contém SQL.</p>
 */
public class ClienteController {

    /** Identificadores dos campos, usados em {@link ValidacaoException#getCampo()}. */
    public static final String CAMPO_NOME = "nome";
    /** Campo e-mail. */
    public static final String CAMPO_EMAIL = "email";
    /** Campo telefone. */
    public static final String CAMPO_TELEFONE = "telefone";
    /** Campo endereço. */
    public static final String CAMPO_ENDERECO = "endereco";

    // Tamanhos máximos: os mesmos das colunas no loja_pneus.sql
    private static final int TAMANHO_NOME = 100;
    private static final int TAMANHO_EMAIL = 100;
    private static final int TAMANHO_ENDERECO = 150;

    /** Nome usado nas mensagens de erro do banco. */
    private static final String ENTIDADE = "cliente";

    /** O DAO usado. O tipo é a interface, então qualquer implementação serve. */
    private final ClienteDAO dao;

    /**
     * Cria o controller usando o DAO JDBC, que acessa o MySQL.
     * É o construtor que as telas usam.
     */
    public ClienteController() {
        this(new ClienteDAOJdbc());
    }

    /**
     * Cria o controller com um DAO escolhido por quem chama.
     * Útil para testes (dá para passar um DAO falso, sem banco).
     *
     * @param dao a implementação de ClienteDAO a usar
     */
    public ClienteController(ClienteDAO dao) {
        this.dao = dao;
    }

    /**
     * Cadastra um cliente novo.
     *
     * @param nome     nome digitado (obrigatório, até 100 caracteres)
     * @param email    e-mail digitado (opcional; se preenchido, formato válido)
     * @param telefone telefone digitado, com ou sem máscara (opcional; 10 ou 11 dígitos)
     * @param endereco endereço digitado (opcional, até 150 caracteres)
     * @return o cliente gravado, já com o id gerado pelo banco
     * @throws ValidacaoException    se algum campo não passar nas regras
     * @throws PersistenciaException se o banco recusar (ex.: e-mail repetido),
     *                               já com a mensagem amigável
     */
    public Cliente cadastrar(String nome, String email, String telefone,
                             String endereco)
            throws ValidacaoException, PersistenciaException {
        Cliente cliente = validarEMontar(nome, email, telefone, endereco);
        try {
            dao.inserir(cliente);
        } catch (PersistenciaException e) {
            throw TradutorErros.traduzir(e, ENTIDADE);
        }
        return cliente;
    }

    /**
     * Atualiza os dados de um cliente que já existe.
     *
     * @param id       id do cliente selecionado na tabela
     * @param nome     nome digitado
     * @param email    e-mail digitado
     * @param telefone telefone digitado
     * @param endereco endereço digitado
     * @return o cliente com os dados novos
     * @throws ValidacaoException    se nenhum cliente foi selecionado ou algum
     *                               campo não passar nas regras
     * @throws PersistenciaException se o banco recusar ou o cliente não existir mais
     */
    public Cliente atualizar(int id, String nome, String email, String telefone,
                             String endereco)
            throws ValidacaoException, PersistenciaException {
        exigirSelecao(id);
        Cliente cliente = validarEMontar(nome, email, telefone, endereco);
        cliente.setId(id);
        try {
            dao.atualizar(cliente);
        } catch (PersistenciaException e) {
            throw TradutorErros.traduzir(e, ENTIDADE);
        }
        return cliente;
    }

    /**
     * Exclui um cliente. O banco não deixa excluir cliente que tem vendas.
     *
     * @param id id do cliente selecionado na tabela
     * @throws ValidacaoException    se nenhum cliente foi selecionado
     * @throws PersistenciaException se o cliente tiver vendas (mensagem
     *                               amigável) ou o banco recusar por outro motivo
     */
    public void excluir(int id) throws ValidacaoException, PersistenciaException {
        exigirSelecao(id);
        try {
            dao.excluir(id);
        } catch (PersistenciaException e) {
            throw TradutorErros.traduzir(e, ENTIDADE);
        }
    }

    /**
     * Busca um cliente pelo id.
     *
     * @param id id do cliente
     * @return o cliente, ou null se não existir
     * @throws PersistenciaException se o banco recusar a consulta
     */
    public Cliente buscarPorId(int id) throws PersistenciaException {
        try {
            return dao.buscarPorId(id);
        } catch (PersistenciaException e) {
            throw TradutorErros.traduzir(e, ENTIDADE);
        }
    }

    /**
     * Lista clientes filtrando pelo nome e ordenando por dois critérios.
     *
     * @param filtroNome parte do nome digitada na busca (vazio = todos)
     * @param ordem1     primeiro critério ("Ordenar por"); null = nome A-Z
     * @param ordem2     segundo critério ("Depois por"); null = nome A-Z
     * @return a lista encontrada (vazia se nada combinar com a busca)
     * @throws PersistenciaException se o banco recusar a consulta
     */
    public List<Cliente> listar(String filtroNome, ClienteOrdem ordem1,
                                ClienteOrdem ordem2) throws PersistenciaException {
        try {
            return dao.listar(Validador.limpar(filtroNome), ordem1, ordem2);
        } catch (PersistenciaException e) {
            throw TradutorErros.traduzir(e, ENTIDADE);
        }
    }

    /**
     * Lista todos os clientes em ordem alfabética. Útil para o combo de
     * clientes da tela de vendas.
     *
     * @return todos os clientes, de A a Z
     * @throws PersistenciaException se o banco recusar a consulta
     */
    public List<Cliente> listarTodos() throws PersistenciaException {
        return listar(null, ClienteOrdem.NOME_ASC, ClienteOrdem.EMAIL_ASC);
    }

    // ------------------------------------------------------------------
    // Métodos auxiliares
    // ------------------------------------------------------------------

    /**
     * Valida todos os campos, na mesma ordem em que aparecem no formulário,
     * e monta o objeto. Para no primeiro erro.
     */
    private Cliente validarEMontar(String nome, String email, String telefone,
                                   String endereco) throws ValidacaoException {
        String nomeOk = Validador.textoObrigatorio(nome, TAMANHO_NOME,
                CAMPO_NOME, "o nome do cliente");
        String emailOk = Validador.email(email, TAMANHO_EMAIL, CAMPO_EMAIL);
        String telefoneOk = Validador.telefone(telefone, CAMPO_TELEFONE);
        String enderecoOk = Validador.textoOpcional(endereco, TAMANHO_ENDERECO,
                CAMPO_ENDERECO, "o endereço");
        return new Cliente(nomeOk, emailOk, telefoneOk, enderecoOk);
    }

    /** Barra editar ou excluir quando nenhuma linha da tabela foi selecionada. */
    private void exigirSelecao(int id) throws ValidacaoException {
        if (id <= 0) {
            throw new ValidacaoException(null, "Selecione um cliente na lista.");
        }
    }
}
