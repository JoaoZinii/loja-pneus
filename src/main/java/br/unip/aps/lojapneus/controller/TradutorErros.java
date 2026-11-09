package br.unip.aps.lojapneus.controller;

import java.io.IOException;
import java.sql.SQLException;

import br.unip.aps.lojapneus.exception.PersistenciaException;

/**
 * Traduz os erros do banco (códigos do MySQL) em mensagens que o usuário
 * entende.
 *
 * <p>O DAO não mostra nada para o usuário: ele embrulha a SQLException numa
 * {@link PersistenciaException} e guarda o código de erro do MySQL. Aqui o
 * Controller olha esse código e troca a mensagem técnica por uma amigável.
 * O detalhe técnico vai só para o console, para quem estiver programando.</p>
 *
 * <p>Os códigos vêm da tabela de erros do MySQL usada no projeto.</p>
 *
 * <p>A classe não é pública: só os controllers deste pacote usam.</p>
 */
final class TradutorErros {

    /** 1062: violação de UNIQUE (e-mail repetido). */
    static final int REGISTRO_DUPLICADO = 1062;

    /** 1451: a FK impede apagar um registro que tem vendas ligadas a ele. */
    static final int TEM_VENDAS_LIGADAS = 1451;

    /** 1452: a FK não encontrou o cliente ou o pneu da venda. */
    static final int REGISTRO_PAI_NAO_EXISTE = 1452;

    /** 3819: alguma restrição CHECK foi violada (aro, preço, estoque, quantidade). */
    static final int CHECK_VIOLADO = 3819;

    /** 1048: coluna NOT NULL recebeu nulo. */
    static final int CAMPO_OBRIGATORIO_NULO = 1048;

    /** 1406: texto maior que o tamanho da coluna VARCHAR. */
    static final int TEXTO_GRANDE_DEMAIS = 1406;

    /** 1264: número maior do que a coluna aceita. */
    static final int NUMERO_FORA_DO_LIMITE = 1264;

    /** 1045: usuário ou senha do banco recusados. */
    static final int ACESSO_NEGADO = 1045;

    /** 1049: o banco loja_pneus não existe (script não foi rodado). */
    static final int BANCO_NAO_EXISTE = 1049;

    /** Mensagem usada quando não dá para falar com o banco. */
    private static final String MSG_SEM_CONEXAO =
            "Não foi possível conectar ao banco de dados. Verifique se o MySQL "
          + "está ligado e se o arquivo db.properties está configurado.";

    /** Mensagem usada para qualquer código que não está na lista. */
    private static final String MSG_GENERICA =
            "Não foi possível concluir a operação. Tente de novo; se o erro "
          + "continuar, avise o responsável pelo sistema.";

    private TradutorErros() {
        // classe utilitária: não deve ser instanciada
    }

    /**
     * Cria uma nova exceção com a mensagem amigável, mantendo o código do
     * erro e a exceção original como causa (encadeamento de exceções).
     *
     * <p>Uso no controller: {@code throw TradutorErros.traduzir(e, "cliente");}</p>
     *
     * @param erro     a exceção que veio do DAO
     * @param entidade "cliente", "pneu" ou "venda", para montar a mensagem
     * @return a exceção pronta para ser lançada para a tela
     */
    static PersistenciaException traduzir(PersistenciaException erro, String entidade) {
        String mensagem = mensagemAmigavel(erro, entidade);
        registrarNoConsole(erro, entidade, mensagem);
        return new PersistenciaException(mensagem, erro.getCodigoErro(), erro);
    }

    /** Escolhe a mensagem para o usuário de acordo com o código do erro. */
    private static String mensagemAmigavel(PersistenciaException erro, String entidade) {
        int codigo = erro.getCodigoErro();

        if (falhaDeConexao(erro)) {
            return MSG_SEM_CONEXAO;
        }

        if (codigo == 0) {
            // Código 0 aparece em dois casos:
            // 1) o próprio DAO criou a exceção, sem SQLException por trás
            //    (ex.: "Cliente não encontrado."). A mensagem já é amigável.
            // 2) houve falha de conexão (MySQL desligado, db.properties faltando).
            if (erro.getCause() == null) {
                return erro.getMessage();
            }
            if (erro.getCause() instanceof SQLException) {
                return MSG_SEM_CONEXAO;
            }
            return MSG_GENERICA;
        }

        return switch (codigo) {
            case REGISTRO_DUPLICADO -> "cliente".equals(entidade)
                    ? "Já existe um cliente com este e-mail."
                    : "Já existe um registro com esses dados.";
            case TEM_VENDAS_LIGADAS -> "Este " + entidade
                    + " possui vendas registradas e não pode ser excluído.";
            case REGISTRO_PAI_NAO_EXISTE ->
                    "O cliente ou o pneu escolhido não existe mais. "
                  + "Atualize a lista e tente de novo.";
            case CHECK_VIOLADO ->
                    "Algum valor está fora das regras do sistema. "
                  + "Confira os campos e tente de novo.";
            case CAMPO_OBRIGATORIO_NULO -> "Preencha todos os campos obrigatórios.";
            case TEXTO_GRANDE_DEMAIS -> "Algum campo passou do tamanho máximo permitido.";
            case NUMERO_FORA_DO_LIMITE -> "Algum número passou do limite permitido.";
            case PersistenciaException.ESTOQUE_INSUFICIENTE ->
                    "Estoque insuficiente para esta venda.";
            case ACESSO_NEGADO, BANCO_NAO_EXISTE -> MSG_SEM_CONEXAO;
            default -> MSG_GENERICA;
        };
    }

    /**
     * Confere se o erro foi falha de conexão. Pelo padrão SQL, todo
     * SQLState que começa com "08" significa problema de conexão
     * (ex.: 08S01 = MySQL desligado ou fora do ar).
     */
    private static boolean falhaDeConexao(PersistenciaException erro) {
        if (erro.getCause() instanceof SQLException) {
            String estado = ((SQLException) erro.getCause()).getSQLState();
            return estado != null && estado.startsWith("08");
        }
        return false;
    }

    /**
     * Mostra o detalhe técnico no console (nunca na tela). Para erros
     * desconhecidos, imprime também a pilha de chamadas, que ajuda a
     * achar a causa.
     */
    private static void registrarNoConsole(PersistenciaException erro,
                                           String entidade, String mensagem) {
        Throwable causa = erro.getCause();
        String detalhe = (causa != null) ? causa.getMessage() : erro.getMessage();
        if (causa != null && causa.getCause() instanceof IOException) {
            detalhe += " | " + causa.getCause().getMessage();
        }
        System.err.println("[ERRO BANCO] " + entidade + " | código "
                + erro.getCodigoErro() + " | " + detalhe);

        if (MSG_GENERICA.equals(mensagem)) {
            erro.printStackTrace();
        }
    }
}
