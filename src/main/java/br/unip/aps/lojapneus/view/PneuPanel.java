package br.unip.aps.lojapneus.view;

import java.awt.Color;
import java.awt.Component;
import java.util.List;
import javax.swing.JCheckBox;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

import br.unip.aps.lojapneus.controller.PneuController;
import br.unip.aps.lojapneus.model.Pneu;
import br.unip.aps.lojapneus.model.PneuOrdem;
import br.unip.aps.lojapneus.view.componentes.CartaoIndicador;
import br.unip.aps.lojapneus.view.componentes.ModeloTabela;
import br.unip.aps.lojapneus.view.componentes.RenderizadorCelula;
import br.unip.aps.lojapneus.view.estilo.Formato;
import br.unip.aps.lojapneus.view.estilo.Icone;
import br.unip.aps.lojapneus.view.estilo.Tema;

/** Aba Pneus: listagem, busca por marca/modelo, ordenação, cadastro, edição e exclusão. */
public class PneuPanel extends PainelListagem<Pneu, PneuOrdem> {

    /** Estoque até este valor é considerado baixo. */
    private static final int LIMITE_BAIXO = 5;
    private static final Color COR_SEM_ESTOQUE = new Color(0xFDECEA);
    private static final Color COR_BAIXO = new Color(0xFFF4E0);

    private final JCheckBox soBaixo = new JCheckBox("Só estoque baixo (até " + LIMITE_BAIXO + " un.)");
    private volatile boolean filtrarBaixo;

    /** Fundo da linha: vermelho claro sem estoque, laranja claro com estoque baixo. */
    private static Color corDaLinha(Pneu p) {
        if (p.getEstoque() == 0) {
            return COR_SEM_ESTOQUE;
        }
        return p.getEstoque() <= LIMITE_BAIXO ? COR_BAIXO : null;
    }

    private final PneuController controller;
    private final CartaoIndicador indModelos;
    private final CartaoIndicador indUnidades;
    private final CartaoIndicador indBaixo;
    private final CartaoIndicador indValor;

    /** @param controller controller de pneus */
    public PneuPanel(PneuController controller) {
        super("pneu", "pneus", false, "Buscar por marca ou modelo...",
                criarModelo(), opcoes(), 0, 7);
        this.controller = controller;
        indModelos = adicionarIndicador("Modelos listados", Tema.GRAFITE_CLARO, Icone.Tipo.PNEU);
        indUnidades = adicionarIndicador("Unidades em estoque", Tema.LARANJA, Icone.Tipo.CAIXA);
        indBaixo = adicionarIndicador("Estoque baixo", Tema.VERMELHO, Icone.Tipo.ALERTA);
        indBaixo.setToolTipText("Modelos com até 5 unidades em estoque");
        indValor = adicionarIndicador("Valor em estoque", Tema.VERDE, Icone.Tipo.CIFRA);
        soBaixo.setOpaque(false);
        soBaixo.setFont(Tema.FONTE);
        soBaixo.setForeground(Tema.TEXTO);
        soBaixo.setFocusPainted(false);
        soBaixo.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        soBaixo.addActionListener(e -> recarregar());
        adicionarFiltroExtra("Estoque", soBaixo);
        tabela.getColumnModel().getColumn(0).setCellRenderer(new RenderizadorCelula(false, true));
        tabela.getColumnModel().getColumn(1).setCellRenderer(new RenderizadorCelula(true, false));
        tabela.getColumnModel().getColumn(4).setCellRenderer(new RenderizadorEstoque());
        tabela.getColumnModel().getColumn(5).setCellRenderer(new RenderizadorCelula(true, false));
        iniciar();
    }

    private static ModeloTabela<Pneu> criarModelo() {
        return new ModeloTabela<Pneu>()
                .coluna("ID", Integer.class, 60, Pneu::getId)
                .coluna("Marca", String.class, 160, Pneu::getMarca)
                .coluna("Modelo", String.class, 220, Pneu::getModelo)
                .coluna("Aro", Integer.class, 70, Pneu::getAro)
                .coluna("Estoque", Integer.class, 90, Pneu::getEstoque)
                .coluna("Preço", java.math.BigDecimal.class, 120, Pneu::getPreco)
                .colorirLinhas(PneuPanel::corDaLinha);
    }

    private static List<Opcao<PneuOrdem>> opcoes() {
        return List.of(
                new Opcao<>("Marca: A a Z", PneuOrdem.MARCA_ASC),
                new Opcao<>("Marca: Z a A", PneuOrdem.MARCA_DESC),
                new Opcao<>("Modelo: A a Z", PneuOrdem.MODELO_ASC),
                new Opcao<>("Modelo: Z a A", PneuOrdem.MODELO_DESC),
                new Opcao<>("Aro: menor para maior", PneuOrdem.ARO_ASC),
                new Opcao<>("Aro: maior para menor", PneuOrdem.ARO_DESC),
                new Opcao<>("Preço: menor para maior", PneuOrdem.PRECO_ASC),
                new Opcao<>("Preço: maior para menor", PneuOrdem.PRECO_DESC),
                new Opcao<>("Estoque: menor para maior", PneuOrdem.ESTOQUE_ASC),
                new Opcao<>("Estoque: maior para menor", PneuOrdem.ESTOQUE_DESC));
    }

    @Override
    protected List<Pneu> consultar(String filtro, PneuOrdem o1, PneuOrdem o2) throws Exception {
        List<Pneu> lista = controller.listar(filtro, o1, o2);
        if (filtrarBaixo) {
            lista = lista.stream().filter(p -> p.getEstoque() <= LIMITE_BAIXO).toList();
        }
        return lista;
    }

    @Override
    protected void prepararConsulta() {
        filtrarBaixo = soBaixo.isSelected();
    }

    @Override
    protected boolean temFiltroExtra() {
        return filtrarBaixo;
    }

    @Override
    protected void aoCarregar(List<Pneu> lista) {
        int unidades = lista.stream().mapToInt(Pneu::getEstoque).sum();
        long baixo = lista.stream().filter(p -> p.getEstoque() <= 5).count();
        java.math.BigDecimal valor = lista.stream()
                .map(p -> p.getPreco().multiply(java.math.BigDecimal.valueOf(p.getEstoque())))
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        indModelos.setValor(String.valueOf(lista.size()));
        indUnidades.setValor(String.valueOf(unidades));
        indBaixo.setValor(String.valueOf(baixo));
        indBaixo.setCorValor(baixo > 0 ? Tema.VERMELHO : Tema.GRAFITE);
        indValor.setValor(Formato.moeda(valor));
    }

    @Override
    protected Object chave(Pneu item) {
        return item.getId();
    }

    @Override
    protected boolean abrirFormulario(Pneu itemOuNull) {
        return new PneuDialog(SwingUtilities.getWindowAncestor(this), controller, itemOuNull)
                .exibir();
    }

    @Override
    protected void remover(Pneu item) throws Exception {
        controller.excluir(item.getId());
    }

    @Override
    protected String descricao(Pneu item) {
        return item.toString();
    }

    /** Estoque como "pílula": vermelha quando zerado, âmbar quando baixo (até 5), número simples no resto. */
    private static final class RenderizadorEstoque extends RenderizadorCelula {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object valor, boolean sel,
                boolean foco, int linha, int coluna) {
            super.getTableCellRendererComponent(t, valor, sel, foco, linha, coluna);
            if (valor instanceof Integer q && q <= 5) {
                Color base = q == 0 ? Tema.VERMELHO : Tema.ALERTA;
                setText(q == 0 ? "Sem estoque" : q + " · baixo");
                setFont(Tema.FONTE_PEQUENA.deriveFont(java.awt.Font.BOLD));
                setForeground(base);
                pilula = new Color(base.getRed(), base.getGreen(), base.getBlue(), 32);
            }
            return this;
        }
    }
}
