package br.unip.aps.lojapneus.teste;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import br.unip.aps.lojapneus.controller.ClienteController;
import br.unip.aps.lojapneus.controller.PneuController;
import br.unip.aps.lojapneus.controller.VendaController;
import br.unip.aps.lojapneus.exception.PersistenciaException;
import br.unip.aps.lojapneus.exception.ValidacaoException;
import br.unip.aps.lojapneus.model.Cliente;
import br.unip.aps.lojapneus.model.ClienteOrdem;
import br.unip.aps.lojapneus.model.Pneu;
import br.unip.aps.lojapneus.model.Venda;
import br.unip.aps.lojapneus.model.VendaOrdem;

/**
 * Testa os três controllers contra o banco de verdade, sem interface gráfica.
 * Cada teste deixa o banco como estava antes (mesma ideia do TesteDAO).
 *
 * <p>Testa as duas barreiras do controller:</p>
 * <ul>
 *   <li>entradas inválidas precisam ser barradas com {@link ValidacaoException}
 *       apontando o campo certo, antes de chegar ao banco;</li>
 *   <li>erros do banco precisam chegar como {@link PersistenciaException}
 *       com mensagem amigável, sem nada técnico.</li>
 * </ul>
 *
 * <p>Pré-requisitos: banco criado com o loja_pneus.sql (usa o cliente 1,
 * o cliente 2, o pneu 1 e o pneu 4 dos dados de exemplo) e db.properties
 * preenchido na raiz do projeto.</p>
 */
public class TesteController {

    /** Uma chamada ao controller que pode lançar as exceções do sistema. */
    @FunctionalInterface
    private interface Acao {
        void executar() throws ValidacaoException, PersistenciaException;
    }

    private static final ClienteController clientes = new ClienteController();
    private static final PneuController pneus = new PneuController();
    private static final VendaController vendas = new VendaController();

    private static int aprovados = 0;
    private static int reprovados = 0;

    /**
     * Ponto de entrada: roda os três blocos e mostra o placar.
     *
     * @param args não utilizado
     */
    public static void main(String[] args) {
        try {
            testarCliente();
            testarPneu();
            testarVenda();
        } catch (ValidacaoException | PersistenciaException e) {
            System.out.println("Erro inesperado: " + e.getMessage());
            reprovados++;
        }
        System.out.println();
        System.out.println("RESULTADO: " + aprovados + " aprovados, "
                + reprovados + " reprovados");
    }

    // ==================================================================
    // CLIENTE
    // ==================================================================

    private static void testarCliente() throws ValidacaoException, PersistenciaException {
        System.out.println("===== CLIENTE =====");

        // Entradas inválidas: todas barradas antes do banco
        barrar("nome vazio", ClienteController.CAMPO_NOME,
                () -> clientes.cadastrar("", null, null, null));
        barrar("nome só com espaços", ClienteController.CAMPO_NOME,
                () -> clientes.cadastrar("   ", null, null, null));
        barrar("nome com 101 letras", ClienteController.CAMPO_NOME,
                () -> clientes.cadastrar("A".repeat(101), null, null, null));
        barrar("e-mail sem domínio", ClienteController.CAMPO_EMAIL,
                () -> clientes.cadastrar("Ana", "ana@", null, null));
        barrar("telefone sem DDD", ClienteController.CAMPO_TELEFONE,
                () -> clientes.cadastrar("Ana", null, "9123-4567", null));
        barrar("telefone com letras", ClienteController.CAMPO_TELEFONE,
                () -> clientes.cadastrar("Ana", null, "19abc234567", null));

        // Cadastro válido: o controller limpa e converte os campos
        String email = "teste" + System.currentTimeMillis() + "@email.com";
        Cliente c = clientes.cadastrar("  Ana Teste  ", email, "(19) 99999-0000", "");
        conferir("cadastro válido recebe id", c.getId() > 0, "id " + c.getId());
        conferir("nome sem espaços nas pontas", "Ana Teste".equals(c.getNome()),
                "\"" + c.getNome() + "\"");
        conferir("telefone gravado só com dígitos", "19999990000".equals(c.getTelefone()),
                c.getTelefone());
        conferir("endereço vazio vira null", c.getEndereco() == null,
                String.valueOf(c.getEndereco()));

        // Erro do banco traduzido: e-mail repetido (1062)
        erroDoBanco("e-mail repetido", "Já existe um cliente com este e-mail",
                () -> clientes.cadastrar("Outra Pessoa", email, null, null));

        // Atualizar
        clientes.atualizar(c.getId(), "Ana Editada", email, "1932345678", "Rua Nova, 1");
        Cliente lido = clientes.buscarPorId(c.getId());
        conferir("atualização gravada", "Ana Editada".equals(lido.getNome()), lido.getNome());
        barrar("editar sem selecionar", null,
                () -> clientes.atualizar(0, "Ana", null, null, null));

        // Exclusão bloqueada pela FK (1451): cliente 1 tem vendas
        erroDoBanco("excluir cliente com vendas", "possui vendas registradas",
                () -> clientes.excluir(1));
        barrar("excluir sem selecionar", null, () -> clientes.excluir(0));

        // Excluir de verdade (limpa o teste) e tentar de novo
        clientes.excluir(c.getId());
        conferir("cliente excluído", clientes.buscarPorId(c.getId()) == null, "buscarPorId = null");
        erroDoBanco("excluir cliente que não existe mais", "Cliente não encontrado",
                () -> clientes.excluir(c.getId()));

        // Listagem com filtro
        List<Cliente> lista = clientes.listar("  souza ", ClienteOrdem.NOME_ASC,
                ClienteOrdem.EMAIL_ASC);
        conferir("filtro 'souza' acha o Carlos",
                lista.size() == 1 && lista.get(0).getNome().contains("Souza"),
                lista.size() + " resultado(s)");
    }

    // ==================================================================
    // PNEU
    // ==================================================================

    private static void testarPneu() throws ValidacaoException, PersistenciaException {
        System.out.println("===== PNEU =====");

        barrar("marca vazia", PneuController.CAMPO_MARCA,
                () -> pneus.cadastrar("", "X", "16", "100", "1"));
        barrar("modelo vazio", PneuController.CAMPO_MODELO,
                () -> pneus.cadastrar("Marca", " ", "16", "100", "1"));
        barrar("aro com letra", PneuController.CAMPO_ARO,
                () -> pneus.cadastrar("Marca", "X", "abc", "100", "1"));
        barrar("aro 30", PneuController.CAMPO_ARO,
                () -> pneus.cadastrar("Marca", "X", "30", "100", "1"));
        barrar("preço com letra", PneuController.CAMPO_PRECO,
                () -> pneus.cadastrar("Marca", "X", "16", "abc", "1"));
        barrar("preço zero", PneuController.CAMPO_PRECO,
                () -> pneus.cadastrar("Marca", "X", "16", "0", "1"));
        barrar("preço negativo", PneuController.CAMPO_PRECO,
                () -> pneus.cadastrar("Marca", "X", "16", "-5", "1"));
        barrar("estoque negativo", PneuController.CAMPO_ESTOQUE,
                () -> pneus.cadastrar("Marca", "X", "16", "100", "-1"));
        barrar("estoque com vírgula", PneuController.CAMPO_ESTOQUE,
                () -> pneus.cadastrar("Marca", "X", "16", "100", "2,5"));

        // Cadastro válido com preço digitado com vírgula
        Pneu p = pneus.cadastrar("TesteMarca", "Modelo X", "16", "589,90", "5");
        conferir("cadastro válido recebe id", p.getId() > 0, "id " + p.getId());
        conferir("preço \"589,90\" vira 589.90",
                new BigDecimal("589.90").equals(p.getPreco()), String.valueOf(p.getPreco()));

        // Atualizar preço (com ponto de milhar) e estoque
        pneus.atualizar(p.getId(), "TesteMarca", "Modelo X", "16", "1.299,90", "7");
        Pneu lido = pneus.buscarPorId(p.getId());
        conferir("preço \"1.299,90\" gravado como 1299.90",
                new BigDecimal("1299.90").compareTo(lido.getPreco()) == 0,
                String.valueOf(lido.getPreco()));
        conferir("estoque atualizado para 7", lido.getEstoque() == 7,
                String.valueOf(lido.getEstoque()));

        // Exclusão bloqueada pela FK (1451): pneu 1 tem vendas
        erroDoBanco("excluir pneu com vendas", "possui vendas registradas",
                () -> pneus.excluir(1));

        // Limpa o teste
        pneus.excluir(p.getId());
        conferir("pneu excluído", pneus.buscarPorId(p.getId()) == null, "buscarPorId = null");

        // Só pneus com estoque no combo de vendas
        boolean todosComEstoque = true;
        for (Pneu x : pneus.listarComEstoque()) {
            if (x.getEstoque() <= 0) {
                todosComEstoque = false;
            }
        }
        conferir("listarComEstoque só traz estoque > 0", todosComEstoque, "");
    }

    // ==================================================================
    // VENDA
    // ==================================================================

    private static void testarVenda() throws ValidacaoException, PersistenciaException {
        System.out.println("===== VENDA =====");

        Cliente cliente = clientes.buscarPorId(2);
        Pneu pneu = pneus.buscarPorId(4);   // Michelin Primacy 4, R$ 689,00
        int estoqueInicial = pneu.getEstoque();
        System.out.println("  (estoque inicial do pneu 4: " + estoqueInicial + ")");

        barrar("sem cliente", VendaController.CAMPO_CLIENTE,
                () -> vendas.registrar(null, pneu, "1"));
        barrar("sem pneu", VendaController.CAMPO_PNEU,
                () -> vendas.registrar(cliente, null, "1"));
        barrar("quantidade vazia", VendaController.CAMPO_QUANTIDADE,
                () -> vendas.registrar(cliente, pneu, ""));
        barrar("quantidade com letra", VendaController.CAMPO_QUANTIDADE,
                () -> vendas.registrar(cliente, pneu, "dois"));
        barrar("quantidade zero", VendaController.CAMPO_QUANTIDADE,
                () -> vendas.registrar(cliente, pneu, "0"));
        barrarComMensagem("quantidade acima do estoque", VendaController.CAMPO_QUANTIDADE,
                "há " + estoqueInicial + " unidades",
                () -> vendas.registrar(cliente, pneu, String.valueOf(estoqueInicial + 1)));

        // Total calculado na hora (para a tela)
        conferir("calcularTotal(2 x 689,00) = 1378,00",
                new BigDecimal("1378.00").compareTo(vendas.calcularTotal(pneu, "2")) == 0,
                String.valueOf(vendas.calcularTotal(pneu, "2")));
        conferir("calcularTotal com quantidade inválida = 0",
                BigDecimal.ZERO.compareTo(vendas.calcularTotal(pneu, "abc")) == 0, "");

        // O preço vem do banco, não do objeto da tela: simulamos um combo
        // desatualizado com preço errado e conferimos que a venda ignora isso.
        Pneu pneuDoCombo = new Pneu(pneu.getId(), pneu.getMarca(), pneu.getModelo(),
                pneu.getAro(), pneu.getEstoque(), new BigDecimal("1.00"));
        Venda v = vendas.registrar(cliente, pneuDoCombo, "2");
        conferir("venda registrada recebe id", v.getId() > 0, "id " + v.getId());
        conferir("preço unitário veio do banco (689,00), não da tela",
                new BigDecimal("689.00").compareTo(v.getPrecoUnitario()) == 0,
                String.valueOf(v.getPrecoUnitario()));
        int estoqueDepois = pneus.buscarPorId(4).getEstoque();
        conferir("estoque baixou 2", estoqueDepois == estoqueInicial - 2,
                estoqueInicial + " -> " + estoqueDepois);

        // Editar a venda: quantidade 2 -> 3, mesmo pneu (preço original preservado)
        Venda editada = vendas.atualizar(v.getId(), cliente, pneu, "3");
        conferir("editar venda: nova quantidade gravada", editada.getQuantidade() == 3,
                "quantidade " + editada.getQuantidade());
        conferir("editar venda: estoque ajustado (3 no total)",
                pneus.buscarPorId(4).getEstoque() == estoqueInicial - 3,
                "estoque " + pneus.buscarPorId(4).getEstoque());
        conferir("editar venda: preço unitário preservado",
                new BigDecimal("689.00").compareTo(editada.getPrecoUnitario()) == 0,
                String.valueOf(editada.getPrecoUnitario()));
        barrar("editar venda acima do estoque", VendaController.CAMPO_QUANTIDADE,
                () -> vendas.atualizar(v.getId(), cliente, pneu, String.valueOf(estoqueInicial + 1)));
        barrar("editar venda sem selecionar", null,
                () -> vendas.atualizar(0, cliente, pneu, "1"));

        // Listagem por período
        List<Venda> setembro = vendas.listar(null, "01/09/2026", "30/09/2026",
                VendaOrdem.CLIENTE_ASC, VendaOrdem.DATA_DESC);
        conferir("vendas de setembro/2026 = 6", setembro.size() == 6,
                setembro.size() + " venda(s)");
        barrar("data inexistente (31/02)", VendaController.CAMPO_DATA_INICIO,
                () -> vendas.listar(null, "31/02/2026", "", null, null));
        barrar("data final antes da inicial", VendaController.CAMPO_DATA_FIM,
                () -> vendas.listar(null, "10/10/2026", "01/10/2026", null, null));

        // Excluir a venda devolve o estoque (limpa o teste)
        barrar("excluir sem selecionar", null, () -> vendas.excluir(0));
        vendas.excluir(v.getId());
        int estoqueFinal = pneus.buscarPorId(4).getEstoque();
        conferir("excluir devolveu o estoque", estoqueFinal == estoqueInicial,
                "estoque " + estoqueFinal);
        erroDoBanco("excluir venda que não existe mais", "Venda não encontrada",
                () -> vendas.excluir(v.getId()));
    }

    // ------------------------------------------------------------------
    // Auxiliares de teste
    // ------------------------------------------------------------------

    /** A ação precisa ser barrada pela validação, apontando o campo certo. */
    private static void barrar(String descricao, String campoEsperado, Acao acao) {
        barrarComMensagem(descricao, campoEsperado, "", acao);
    }

    /** Igual a barrar, mas também confere um trecho da mensagem. */
    private static void barrarComMensagem(String descricao, String campoEsperado,
                                          String trechoMensagem, Acao acao) {
        try {
            acao.executar();
            falhou(descricao, "foi aceito, mas devia ser barrado");
        } catch (ValidacaoException e) {
            boolean campoCerto = Objects.equals(campoEsperado, e.getCampo());
            boolean mensagemCerta = e.getMessage().contains(trechoMensagem);
            conferir(descricao, campoCerto && mensagemCerta, "\"" + e.getMessage() + "\"");
        } catch (PersistenciaException e) {
            falhou(descricao, "chegou ao banco: " + e.getMessage());
        }
    }

    /** A ação precisa falhar no banco, com uma mensagem amigável esperada. */
    private static void erroDoBanco(String descricao, String trechoMensagem, Acao acao) {
        try {
            acao.executar();
            falhou(descricao, "foi aceito, mas o banco devia recusar");
        } catch (PersistenciaException e) {
            conferir(descricao, e.getMessage().contains(trechoMensagem),
                    "\"" + e.getMessage() + "\"");
        } catch (ValidacaoException e) {
            falhou(descricao, "barrado na validação: " + e.getMessage());
        }
    }

    private static void conferir(String descricao, boolean passou, String detalhe) {
        if (passou) {
            aprovados++;
            System.out.println("  OK     " + descricao + " " + detalhe);
        } else {
            falhou(descricao, detalhe);
        }
    }

    private static void falhou(String descricao, String detalhe) {
        reprovados++;
        System.out.println("  FALHOU " + descricao + " | " + detalhe);
    }
}
