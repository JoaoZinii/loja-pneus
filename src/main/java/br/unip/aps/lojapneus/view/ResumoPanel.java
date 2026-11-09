package br.unip.aps.lojapneus.view;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import br.unip.aps.lojapneus.controller.PneuController;
import br.unip.aps.lojapneus.controller.VendaController;
import br.unip.aps.lojapneus.model.Pneu;
import br.unip.aps.lojapneus.model.PneuOrdem;
import br.unip.aps.lojapneus.model.Venda;
import br.unip.aps.lojapneus.view.componentes.BotaoTema;
import br.unip.aps.lojapneus.view.componentes.Cartao;
import br.unip.aps.lojapneus.view.componentes.CartaoIndicador;
import br.unip.aps.lojapneus.view.componentes.ComboTema;
import br.unip.aps.lojapneus.view.componentes.GraficoBarras;
import br.unip.aps.lojapneus.view.componentes.Mensagens;
import br.unip.aps.lojapneus.view.estilo.Formato;
import br.unip.aps.lojapneus.view.estilo.Icone;
import br.unip.aps.lojapneus.view.estilo.Tema;

/**
 * Aba "Resumo": painel gerencial com faturamento por mês, melhores clientes, pneus mais
 * vendidos e estoque baixo. Tudo é calculado em Java a partir das vendas e dos pneus que os
 * controllers já listam; não cria nem altera nada no banco.
 */
public class ResumoPanel extends JPanel {

    private static final int LIMITE_BAIXO = 5;
    private static final int MESES = 6;
    private static final int TOP = 5;
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter MES = DateTimeFormatter.ofPattern("MM/yy");

    /** Períodos do filtro (a data é comparada com o dia da venda). */
    private enum Periodo {
        TUDO("Todo o período"), DIAS_30("Últimos 30 dias"), DIAS_90("Últimos 90 dias"), ANO("Este ano");

        private final String rotulo;

        Periodo(String rotulo) {
            this.rotulo = rotulo;
        }

        LocalDate inicio(LocalDate hoje) {
            return switch (this) {
                case TUDO -> null;
                case DIAS_30 -> hoje.minusDays(29);
                case DIAS_90 -> hoje.minusDays(89);
                case ANO -> hoje.withDayOfYear(1);
            };
        }

        @Override
        public String toString() {
            return rotulo;
        }
    }

    private record Dados(List<Venda> vendas, List<Pneu> pneus) { }

    private final VendaController vendas;
    private final PneuController pneus;
    private final JComboBox<Periodo> comboPeriodo = new JComboBox<>(Periodo.values());
    private final CartaoIndicador indFaturamento =
            new CartaoIndicador("Faturamento", Tema.VERDE, Icone.Tipo.CIFRA);
    private final CartaoIndicador indVendas =
            new CartaoIndicador("Vendas realizadas", Tema.GRAFITE_CLARO, Icone.Tipo.RECIBO);
    private final CartaoIndicador indTicket =
            new CartaoIndicador("Ticket médio", Tema.LARANJA, Icone.Tipo.CIFRA);
    private final CartaoIndicador indBaixo =
            new CartaoIndicador("Estoque baixo", Tema.VERMELHO, Icone.Tipo.ALERTA);
    private final GraficoBarras graficoMes =
            new GraficoBarras(GraficoBarras.Modo.COLUNAS, Tema.LARANJA);
    private final GraficoBarras rankClientes =
            new GraficoBarras(GraficoBarras.Modo.LINHAS, Tema.GRAFITE_CLARO);
    private final GraficoBarras rankPneus =
            new GraficoBarras(GraficoBarras.Modo.LINHAS, Tema.VERDE);
    private final GraficoBarras rankBaixo =
            new GraficoBarras(GraficoBarras.Modo.LINHAS, Tema.LARANJA);
    private Dados dados;
    private int sequencia;

    /**
     * @param vendas controller de vendas (fonte das vendas)
     * @param pneus  controller de pneus (fonte do estoque)
     */
    public ResumoPanel(VendaController vendas, PneuController pneus) {
        super(new BorderLayout(0, Tema.ESPACO));
        this.vendas = vendas;
        this.pneus = pneus;
        setBackground(Tema.FUNDO);
        setBorder(Tema.margem(18, 26));

        rankBaixo.setMaximo(LIMITE_BAIXO);
        indBaixo.setToolTipText("Modelos com até " + LIMITE_BAIXO
                + " unidades em estoque (não depende do período)");

        comboPeriodo.setPrototypeDisplayValue(Periodo.DIAS_90);
        ComboTema.aplicar(comboPeriodo);
        comboPeriodo.addActionListener(e -> aplicar());
        BotaoTema atualizar = new BotaoTema("Atualizar", BotaoTema.Tipo.NEUTRO,
                Icone.Tipo.ATUALIZAR);
        atualizar.addActionListener(e -> recarregar());

        Cartao topo = new Cartao();
        topo.setLayout(new BorderLayout(12, 0));
        JLabel titulo = new JLabel("Resumo do negócio");
        titulo.setFont(Tema.FONTE_TITULO);
        titulo.setForeground(Tema.GRAFITE);
        JLabel sub = new JLabel("Calculado a partir das vendas e do estoque cadastrados.");
        sub.setFont(Tema.FONTE_PEQUENA);
        sub.setForeground(Tema.TEXTO_SUAVE);
        JPanel textos = new JPanel(new BorderLayout());
        textos.setOpaque(false);
        textos.add(titulo, BorderLayout.NORTH);
        textos.add(sub, BorderLayout.SOUTH);
        JPanel filtro = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        filtro.setOpaque(false);
        filtro.add(PainelListagem.criarRotulo("Período"));
        filtro.add(comboPeriodo);
        filtro.add(atualizar);
        topo.add(textos, BorderLayout.CENTER);
        topo.add(filtro, BorderLayout.EAST);

        JPanel cartoes = new JPanel(new GridLayout(1, 0, Tema.ESPACO, 0));
        cartoes.setOpaque(false);
        cartoes.add(indFaturamento);
        cartoes.add(indVendas);
        cartoes.add(indTicket);
        cartoes.add(indBaixo);

        JPanel norte = new JPanel(new BorderLayout(0, Tema.ESPACO));
        norte.setOpaque(false);
        norte.add(topo, BorderLayout.NORTH);
        norte.add(cartoes, BorderLayout.CENTER);

        JPanel grade = new JPanel(new GridLayout(2, 2, Tema.ESPACO, Tema.ESPACO));
        grade.setOpaque(false);
        grade.add(moldura("Faturamento por mês · últimos " + MESES + " meses", graficoMes));
        grade.add(moldura("Melhores clientes (valor gasto)", rankClientes));
        grade.add(moldura("Pneus mais vendidos (unidades)", rankPneus));
        grade.add(moldura("Estoque baixo (até " + LIMITE_BAIXO + " un.)", rankBaixo));

        add(norte, BorderLayout.NORTH);
        add(grade, BorderLayout.CENTER);
        SwingUtilities.invokeLater(this::recarregar);
    }

    /** Busca os dados de novo e redesenha (chamado ao abrir a aba). */
    public void recarregar() {
        final int minha = ++sequencia;
        new SwingWorker<Dados, Void>() {
            @Override
            protected Dados doInBackground() throws Exception {
                List<Venda> todas = vendas.listarTodas();
                List<Pneu> estoque = pneus.listar(null, PneuOrdem.ESTOQUE_ASC, PneuOrdem.MARCA_ASC);
                return new Dados(todas, estoque);
            }

            @Override
            protected void done() {
                if (minha != sequencia) {
                    return;
                }
                try {
                    dados = get();
                    aplicar();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    Mensagens.erro(ResumoPanel.this, ex.getCause());
                }
            }
        }.execute();
    }

    private static Cartao moldura(String titulo, JComponent conteudo) {
        Cartao c = new Cartao();
        c.setLayout(new BorderLayout(0, 8));
        JLabel t = new JLabel(titulo);
        t.setFont(Tema.FONTE_NEGRITO);
        t.setForeground(Tema.GRAFITE_CLARO);
        c.add(t, BorderLayout.NORTH);
        c.add(conteudo, BorderLayout.CENTER);
        return c;
    }

    /** Recalcula tudo com o período escolhido e os dados já carregados. */
    private void aplicar() {
        if (dados == null) {
            return;
        }
        Periodo periodo = (Periodo) comboPeriodo.getSelectedItem();
        LocalDate inicio = periodo == null ? null : periodo.inicio(LocalDate.now());
        List<Venda> lista = new ArrayList<>();
        for (Venda v : dados.vendas()) {
            if (inicio == null || !v.getDataVenda().toLocalDate().isBefore(inicio)) {
                lista.add(v);
            }
        }

        BigDecimal total = BigDecimal.ZERO;
        for (Venda v : lista) {
            total = total.add(v.getValorTotal());
        }
        BigDecimal ticket = lista.isEmpty() ? BigDecimal.ZERO
                : total.divide(BigDecimal.valueOf(lista.size()), 2, RoundingMode.HALF_UP);
        indFaturamento.setValor(Formato.moeda(total));
        indVendas.setValor(String.valueOf(lista.size()));
        indTicket.setValor(Formato.moeda(ticket));

        // faturamento por mês: os últimos MESES meses, terminando no último mês com venda
        TreeMap<YearMonth, BigDecimal> porMes = new TreeMap<>();
        for (Venda v : lista) {
            porMes.merge(YearMonth.from(v.getDataVenda()), v.getValorTotal(), BigDecimal::add);
        }
        List<GraficoBarras.Item> meses = new ArrayList<>();
        if (!porMes.isEmpty()) {
            YearMonth ultimo = porMes.lastKey();
            for (int i = MESES - 1; i >= 0; i--) {
                YearMonth ym = ultimo.minusMonths(i);
                BigDecimal valor = porMes.getOrDefault(ym, BigDecimal.ZERO);
                meses.add(new GraficoBarras.Item(ym.format(MES), valor.doubleValue(),
                        compacto(valor), null));
            }
        }
        graficoMes.setItens(meses, "Sem vendas no período");

        // rankings de clientes e de pneus
        Map<Integer, BigDecimal> gastoPorCliente = new HashMap<>();
        Map<Integer, String> nomesClientes = new HashMap<>();
        Map<Integer, BigDecimal> unidadesPorPneu = new HashMap<>();
        Map<Integer, String> nomesPneus = new HashMap<>();
        for (Venda v : lista) {
            int idCliente = v.getCliente().getId();
            gastoPorCliente.merge(idCliente, v.getValorTotal(), BigDecimal::add);
            nomesClientes.put(idCliente, v.getCliente().getNome());
            int idPneu = v.getPneu().getId();
            unidadesPorPneu.merge(idPneu, BigDecimal.valueOf(v.getQuantidade()), BigDecimal::add);
            nomesPneus.put(idPneu, v.getPneu().toString());
        }
        rankClientes.setItens(ranking(gastoPorCliente, nomesClientes, Formato::moeda),
                "Sem vendas no período");
        rankPneus.setItens(ranking(unidadesPorPneu, nomesPneus, q -> q.intValue() + " un."),
                "Sem vendas no período");

        // estoque baixo (independe do período)
        List<Pneu> poucos = dados.pneus().stream()
                .filter(pn -> pn.getEstoque() <= LIMITE_BAIXO)
                .sorted(Comparator.comparingInt(Pneu::getEstoque).thenComparing(Pneu::getMarca))
                .toList();
        indBaixo.setValor(String.valueOf(poucos.size()));
        indBaixo.setCorValor(poucos.isEmpty() ? Tema.GRAFITE : Tema.VERMELHO);
        List<GraficoBarras.Item> itensBaixo = new ArrayList<>();
        for (Pneu pn : poucos) {
            if (itensBaixo.size() == TOP) {
                break;
            }
            boolean zerado = pn.getEstoque() == 0;
            itensBaixo.add(new GraficoBarras.Item(pn.toString(), pn.getEstoque(),
                    zerado ? "Sem estoque" : pn.getEstoque() + " un.",
                    zerado ? Tema.VERMELHO : Tema.LARANJA));
        }
        rankBaixo.setItens(itensBaixo, "Nenhum pneu com estoque baixo");
    }

    private static List<GraficoBarras.Item> ranking(Map<Integer, BigDecimal> valores,
            Map<Integer, String> nomes, Function<BigDecimal, String> texto) {
        List<Map.Entry<Integer, BigDecimal>> ordenado = new ArrayList<>(valores.entrySet());
        ordenado.sort(Map.Entry.<Integer, BigDecimal>comparingByValue().reversed());
        List<GraficoBarras.Item> itens = new ArrayList<>();
        for (Map.Entry<Integer, BigDecimal> e : ordenado) {
            if (itens.size() == TOP) {
                break;
            }
            itens.add(new GraficoBarras.Item(nomes.get(e.getKey()), e.getValue().doubleValue(),
                    texto.apply(e.getValue()), null));
        }
        return itens;
    }

    /** Valor curto para caber em cima da coluna: R$ 980, R$ 12,3 mil, R$ 1,2 mi. */
    private static String compacto(BigDecimal v) {
        double d = v.doubleValue();
        if (d >= 1_000_000) {
            return String.format(PT_BR, "R$ %.1f mi", d / 1_000_000);
        }
        if (d >= 1_000) {
            return String.format(PT_BR, "R$ %.1f mil", d / 1_000);
        }
        return String.format(PT_BR, "R$ %.0f", d);
    }
}
