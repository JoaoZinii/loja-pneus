package br.unip.aps.lojapneus.teste;

import java.math.BigDecimal;
import java.util.List;

import br.unip.aps.lojapneus.controller.VendaController;
import br.unip.aps.lojapneus.dao.PneuDAO;
import br.unip.aps.lojapneus.dao.VendaDAO;
import br.unip.aps.lojapneus.dao.memoria.BancoMemoria;
import br.unip.aps.lojapneus.dao.memoria.ClienteDAOMemoria;
import br.unip.aps.lojapneus.dao.memoria.PneuDAOMemoria;
import br.unip.aps.lojapneus.dao.memoria.VendaDAOMemoria;
import br.unip.aps.lojapneus.exception.ValidacaoException;
import br.unip.aps.lojapneus.model.Cliente;
import br.unip.aps.lojapneus.model.Pneu;
import br.unip.aps.lojapneus.model.Venda;
import br.unip.aps.lojapneus.model.VendaOrdem;

/**
 * Teste da funcionalidade de Vendas (casos V01 a V16 do plano de testes), sem banco:
 * usa os DAOs em memória, que imitam as regras do MySQL. Execute com o main.
 * Termina com a linha "RESULTADO: N aprovados, M reprovados".
 */
public class TesteVendas {

    private static int ok = 0;
    private static int falhas = 0;

    /** Registra um resultado. */
    private static void confere(String id, String descricao, boolean passou, String detalhe) {
        if (passou) {
            ok++;
        } else {
            falhas++;
        }
        System.out.println((passou ? "  OK     " : "  FALHOU ") + id + " " + descricao + " -> " + detalhe);
    }

    /** Executa a venda e devolve a mensagem de validação (ou null se gravou). */
    private static String tentar(VendaController c, Cliente cl, Pneu p, String qtd) throws Exception {
        try {
            c.registrar(cl, p, qtd);
            return null;
        } catch (ValidacaoException e) {
            return e.getMessage();
        }
    }

    /**
     * Ponto de entrada do teste.
     *
     * @param args não usado
     * @throws Exception qualquer falha inesperada
     */
    public static void main(String[] args) throws Exception {
        BancoMemoria banco = new BancoMemoria();
        VendaDAO vdao = new VendaDAOMemoria(banco);
        PneuDAO pdao = new PneuDAOMemoria(banco);
        ClienteDAOMemoria cdao = new ClienteDAOMemoria(banco);
        VendaController c = new VendaController(vdao, pdao);

        Cliente carlos = cdao.buscarPorId(1);
        Pneu p1 = pdao.buscarPorId(1);   // Pirelli Cinturato P1, R$ 289,90, estoque 24
        Pneu dueler = pdao.buscarPorId(9); // Bridgestone Dueler, estoque 6

        // V01 listar todas
        List<Venda> todas = c.listarTodas();
        confere("V01", "lista todas as vendas", todas.size() == 15, todas.size() + " vendas");

        // V02 filtro por cliente
        List<Venda> souza = c.listar("souza", null, null, VendaOrdem.DATA_ASC, null);
        confere("V02", "filtro por nome 'souza'", souza.size() == 2, souza.size() + " vendas");

        // V03 filtro sem resultado
        confere("V03", "filtro 'zzz' devolve lista vazia",
                c.listar("zzz", null, null, null, null).isEmpty(), "vazia");

        // V04 período setembro
        List<Venda> set = c.listar(null, "01/09/2026", "30/09/2026", VendaOrdem.DATA_ASC, null);
        confere("V04", "período 01/09 a 30/09 = 6 vendas", set.size() == 6, set.size() + " vendas");

        // V05 data final antes da inicial
        String m5 = null;
        try {
            c.listar(null, "30/09/2026", "01/09/2026", null, null);
        } catch (ValidacaoException e) {
            m5 = e.getMessage();
        }
        confere("V05", "data final antes da inicial é barrada", m5 != null, String.valueOf(m5));

        // V06 data inválida
        String m6 = null;
        try {
            c.listar(null, "31/02/2026", null, null, null);
        } catch (ValidacaoException e) {
            m6 = e.getMessage();
        }
        confere("V06", "data inexistente é barrada", m6 != null, String.valueOf(m6));

        // V07 ordenação por dois critérios (total desc, depois cliente asc)
        List<Venda> ord = c.listar(null, null, null, VendaOrdem.TOTAL_DESC, VendaOrdem.CLIENTE_ASC);
        boolean ordenado = true;
        for (int i = 1; i < ord.size(); i++) {
            if (ord.get(i - 1).getValorTotal().compareTo(ord.get(i).getValorTotal()) < 0) {
                ordenado = false;
            }
        }
        confere("V07", "ordenar por total decrescente", ordenado,
                "maior total R$ " + ord.get(0).getValorTotal());

        // V08 total ao vivo
        BigDecimal t = c.calcularTotal(p1, "2");
        confere("V08", "total ao vivo 2 x 289,90 = 579,80", t.compareTo(new BigDecimal("579.80")) == 0, "R$ " + t);

        // V09 total com quantidade inválida
        confere("V09", "total com 'abc' devolve zero",
                c.calcularTotal(p1, "abc").signum() == 0, "0");

        // V10 registrar baixa o estoque e usa o preço atual
        int antes = pdao.buscarPorId(1).getEstoque();
        Venda nova = c.registrar(carlos, p1, "2");
        int depois = pdao.buscarPorId(1).getEstoque();
        confere("V10", "registrar baixa estoque (24 -> 22)", antes == 24 && depois == 22, antes + " -> " + depois);
        confere("V11", "preço unitário vem do pneu (não digitado)",
                nova.getPrecoUnitario().compareTo(new BigDecimal("289.90")) == 0, "R$ " + nova.getPrecoUnitario());
        confere("V12", "venda aparece na listagem", c.listarTodas().size() == 16, "16 vendas");

        // V13 excluir devolve estoque
        c.excluir(nova.getId());
        confere("V13", "excluir devolve estoque (22 -> 24)", pdao.buscarPorId(1).getEstoque() == 24
                && c.listarTodas().size() == 15, pdao.buscarPorId(1).getEstoque() + " un.");

        // V14 estoque insuficiente mostra o estoque atual e não grava
        String m14 = tentar(c, carlos, dueler, "999");
        confere("V14", "quantidade acima do estoque", m14 != null && m14.contains("6"), String.valueOf(m14));
        confere("V15", "venda barrada não altera estoque nem lista",
                pdao.buscarPorId(9).getEstoque() == 6 && c.listarTodas().size() == 15, "estoque 6, 15 vendas");

        // V16 entradas ruins
        String[] ruins = {"", "abc", "0", "-3", "2,5"};
        boolean todasBarradas = true;
        for (String q : ruins) {
            if (tentar(c, carlos, p1, q) == null) {
                todasBarradas = false;
                System.out.println("         aceitou indevidamente: '" + q + "'");
            }
        }
        confere("V16", "quantidade vazia/letra/zero/negativa/decimal barradas", todasBarradas, "5 entradas");
        confere("V17", "sem cliente é barrado", "Selecione o cliente.".equals(tentar(c, null, p1, "1")), "ok");
        confere("V18", "sem pneu é barrado", "Selecione o pneu.".equals(tentar(c, carlos, null, "1")), "ok");

        // V19 editar: muda quantidade e ajusta estoque
        Venda v = c.registrar(carlos, p1, "3");   // estoque 24 -> 21
        c.atualizar(v.getId(), carlos, p1, "5");  // devolve 3, baixa 5 -> 19
        confere("V19", "editar quantidade 3 -> 5 ajusta estoque", pdao.buscarPorId(1).getEstoque() == 19,
                pdao.buscarPorId(1).getEstoque() + " un.");
        c.excluir(v.getId());
        confere("V20", "estoque volta ao original", pdao.buscarPorId(1).getEstoque() == 24, "24 un.");

        System.out.println();
        System.out.println("RESULTADO: " + ok + " aprovados, " + falhas + " reprovados");
        if (falhas > 0) {
            System.exit(1);
        }
    }
}
