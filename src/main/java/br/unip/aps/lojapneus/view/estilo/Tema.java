package br.unip.aps.lojapneus.view.estilo;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.plaf.FontUIResource;

/**
 * Padrão visual do sistema: cores, fontes, espaçamentos e L&amp;F.
 * Toda tela nova (inclusive a aba Vendas) deve usar estas constantes.
 */
public final class Tema {

    public static final Color FUNDO = new Color(0xF4F5F7);
    public static final Color SUPERFICIE = Color.WHITE;
    public static final Color GRAFITE = new Color(0x1F2933);
    public static final Color GRAFITE_CLARO = new Color(0x3E4C59);
    public static final Color LARANJA = new Color(0xF2780C);
    public static final Color LARANJA_ESCURO = new Color(0xD3650A);
    public static final Color VERMELHO = new Color(0xC62828);
    public static final Color VERDE = new Color(0x2E7D32);
    public static final Color ALERTA = new Color(0xB26A00);
    public static final Color BORDA = new Color(0xD5D9E0);
    public static final Color TEXTO = new Color(0x1F2933);
    public static final Color TEXTO_SUAVE = new Color(0x6B7785);
    public static final Color ZEBRA = new Color(0xF8F9FB);
    public static final Color SELECAO = new Color(0xFDE6CF);
    public static final Color HOVER = new Color(0xFFF7EE);
    public static final Color LINHA = new Color(0xEEF0F3);
    public static final Color CABECALHO = new Color(0xF8F9FB);

    /** Espaçamento base entre blocos, em pixels. */
    public static final int ESPACO = 12;

    private static final String FAMILIA = escolherFamilia();
    public static final Font FONTE = new Font(FAMILIA, Font.PLAIN, 14);
    public static final Font FONTE_NEGRITO = FONTE.deriveFont(Font.BOLD);
    public static final Font FONTE_PEQUENA = FONTE.deriveFont(12f);
    public static final Font FONTE_TITULO = FONTE.deriveFont(Font.BOLD, 20f);

    private Tema() { }

    private static String escolherFamilia() {
        Set<String> disponiveis = new HashSet<>(Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment()
                        .getAvailableFontFamilyNames()));
        for (String f : new String[] {"Segoe UI", "Inter", "SF Pro Text",
                "Helvetica Neue", "Noto Sans", "DejaVu Sans"}) {
            if (disponiveis.contains(f)) {
                return f;
            }
        }
        return Font.SANS_SERIF;
    }

    /**
     * Instala o Look and Feel (FlatLaf se estiver no classpath, senão Nimbus),
     * a fonte padrão e os textos dos diálogos em português.
     * Chamar uma vez, antes de criar qualquer janela.
     */
    public static void instalar() {
        try {
            UIManager.setLookAndFeel("com.formdev.flatlaf.FlatLightLaf");
            // Ajustes finos que só o FlatLaf entende (em outros L&F são ignorados)
            UIManager.put("Component.arc", 10);
            UIManager.put("Button.arc", 10);
            UIManager.put("TextComponent.arc", 10);
            UIManager.put("ScrollBar.thumbArc", 999);
            UIManager.put("ScrollBar.width", 12);
            UIManager.put("ScrollBar.showButtons", false);
            UIManager.put("Component.focusColor", LARANJA);
            UIManager.put("Component.focusedBorderColor", LARANJA);
            UIManager.put("ComboBox.selectionBackground", SELECAO);
            UIManager.put("ComboBox.selectionForeground", TEXTO);
        } catch (Exception semFlatLaf) {
            try {
                UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
            } catch (Exception ignorado) {
                // fica o L&F padrão do Java
            }
        }
        for (Object chave : new ArrayList<>(UIManager.getDefaults().keySet())) {
            Object valor = UIManager.get(chave);
            if (valor instanceof FontUIResource f) {
                UIManager.put(chave, new FontUIResource(FONTE.deriveFont(f.getStyle())));
            }
        }
        UIManager.put("OptionPane.okButtonText", "OK");
        UIManager.put("OptionPane.cancelButtonText", "Cancelar");
        UIManager.put("OptionPane.yesButtonText", "Sim");
        UIManager.put("OptionPane.noButtonText", "Não");
    }

    /** @return borda vazia com margens vertical e horizontal */
    public static Border margem(int vertical, int horizontal) {
        return BorderFactory.createEmptyBorder(vertical, horizontal, vertical, horizontal);
    }
}
