package br.unip.aps.lojapneus.view.componentes;

import java.awt.Color;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.math.BigDecimal;
import javax.swing.BorderFactory;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

import br.unip.aps.lojapneus.view.estilo.Formato;
import br.unip.aps.lojapneus.view.estilo.IconeAvatar;
import br.unip.aps.lojapneus.view.estilo.Tema;

/**
 * Desenha as células das tabelas: fundo branco com divisória fina, destaque ao passar o mouse,
 * seleção laranja clara, valores monetários em R$ e "—" para campos vazios.
 * Pode deixar o texto em negrito, esmaecido (ex.: coluna ID) ou dentro de uma "pílula" colorida.
 */
public class RenderizadorCelula extends DefaultTableCellRenderer {

    private final boolean negrito;
    private final boolean suave;
    private Color fundo = Color.WHITE;
    /** Cor de fundo da pílula atrás do texto (null = sem pílula). Defina em getTableCellRendererComponent. */
    protected Color pilula;

    /** Célula comum. */
    public RenderizadorCelula() {
        this(false, false);
    }

    /**
     * @param negrito texto em negrito (colunas principais, como o nome)
     * @param suave   texto cinza claro (colunas secundárias, como o ID)
     */
    public RenderizadorCelula(boolean negrito, boolean suave) {
        this.negrito = negrito;
        this.suave = suave;
        setOpaque(false);
    }

    @Override
    public Component getTableCellRendererComponent(JTable tabela, Object valor,
            boolean selecionada, boolean foco, int linha, int coluna) {
        boolean vazio = valor == null || valor.toString().isBlank();
        String texto = vazio ? "—" : (valor instanceof BigDecimal bd ? Formato.moeda(bd) : valor.toString());
        super.getTableCellRendererComponent(tabela, texto, false, false, linha, coluna);
        setOpaque(false);
        setIcon(null);
        pilula = null;
        Object h = tabela.getClientProperty("linhaHover");
        boolean sobre = h instanceof Integer i && i == linha;
        fundo = selecionada ? Tema.SELECAO : (sobre ? Tema.HOVER : Color.WHITE);
        if (!selecionada && !sobre && tabela.getModel() instanceof ModeloTabela<?> modeloCor) {
            Color destaque = modeloCor.corDaLinha(linha);
            if (destaque != null) {
                fundo = destaque;
            }
        }
        setFont(negrito ? Tema.FONTE_NEGRITO : Tema.FONTE);
        Color cor = vazio ? new Color(0xA0A8B3) : (suave ? new Color(0x9AA5B1) : corTexto(valor));
        setForeground(cor != null ? cor : Tema.TEXTO);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Tema.LINHA),
                BorderFactory.createEmptyBorder(0, 12, 0, 12)));
        setHorizontalAlignment(alinhamento(tabela.getColumnClass(coluna)));
        return this;
    }

    /** @return cor do texto para o valor, ou null para usar a cor padrão */
    protected Color corTexto(Object valor) {
        return null;
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(fundo);
        g.fillRect(0, 0, getWidth(), getHeight());
        if (pilula != null) {
            FontMetrics fm = getFontMetrics(getFont());
            int w = fm.stringWidth(getText()) + 22;
            int h = fm.getHeight() + 6;
            g.setColor(pilula);
            g.fillRoundRect((getWidth() - w) / 2, (getHeight() - h) / 2, w, h, h, h);
        }
        g.dispose();
        super.paintComponent(g0);
    }

    /** @return alinhamento: moeda à direita, inteiros centralizados, texto à esquerda */
    static int alinhamento(Class<?> tipo) {
        if (BigDecimal.class.isAssignableFrom(tipo)) {
            return SwingConstants.RIGHT;
        }
        return Number.class.isAssignableFrom(tipo) ? SwingConstants.CENTER : SwingConstants.LEFT;
    }

    /** Cabeçalho claro, com título pequeno em maiúsculas, alinhado como a coluna. */
    public static class Cabecalho extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable tabela, Object valor,
                boolean selecionada, boolean foco, int linha, int coluna) {
            super.getTableCellRendererComponent(tabela,
                    valor == null ? "" : valor.toString().toUpperCase(), false, false, linha, coluna);
            setOpaque(true);
            setBackground(Tema.CABECALHO);
            setForeground(Tema.TEXTO_SUAVE);
            setFont(Tema.FONTE_PEQUENA.deriveFont(java.awt.Font.BOLD));
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, Tema.BORDA),
                    BorderFactory.createEmptyBorder(0, 12, 0, 12)));
            setHorizontalAlignment(alinhamento(tabela.getColumnClass(coluna)));
            return this;
        }
    }

    /** Coluna de nomes: círculo colorido com as iniciais ao lado do nome, em negrito. */
    public static class Avatar extends RenderizadorCelula {
        /** Cria o renderizador de avatar. */
        public Avatar() {
            super(true, false);
        }

        @Override
        public Component getTableCellRendererComponent(JTable tabela, Object valor,
                boolean selecionada, boolean foco, int linha, int coluna) {
            super.getTableCellRendererComponent(tabela, valor, selecionada, foco, linha, coluna);
            if (valor != null && !valor.toString().isBlank()) {
                setIcon(new IconeAvatar(valor.toString()));
                setIconTextGap(12);
            }
            return this;
        }
    }
}
