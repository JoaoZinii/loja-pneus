package br.unip.aps.lojapneus.view;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import br.unip.aps.lojapneus.exception.ValidacaoException;
import br.unip.aps.lojapneus.view.componentes.BotaoTema;
import br.unip.aps.lojapneus.view.componentes.CampoTexto;
import br.unip.aps.lojapneus.view.componentes.Mensagens;
import br.unip.aps.lojapneus.view.estilo.Tema;

/**
 * Base dos formulários de cadastro/edição: cabeçalho, campos com rótulo, marca de
 * obrigatório (*), botões Cancelar/Salvar, Enter salva e Esc cancela.
 * Quando o controller lança {@link ValidacaoException}, o campo indicado fica vermelho
 * e recebe o foco.
 */
public abstract class FormularioDialog extends JDialog {

    private final JPanel campos = new JPanel(new GridBagLayout());
    private final Map<String, JComponent> porNome = new HashMap<>();
    private final Window dono;
    private final BotaoTema btnSalvar = new BotaoTema("Salvar", BotaoTema.Tipo.PRIMARIO);
    private int linha;
    private boolean salvo;

    /**
     * @param dono      janela pai (o diálogo abre centralizado nela)
     * @param titulo    título da janela e do cabeçalho
     * @param subtitulo frase de apoio abaixo do título
     */
    protected FormularioDialog(Window dono, String titulo, String subtitulo) {
        super(dono, titulo, ModalityType.APPLICATION_MODAL);
        this.dono = dono;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);

        JPanel cabecalho = new JPanel(new BorderLayout(0, 2));
        cabecalho.setBackground(Tema.SUPERFICIE);
        cabecalho.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Tema.BORDA), Tema.margem(16, 22)));
        JLabel t = new JLabel(titulo);
        t.setFont(Tema.FONTE_TITULO);
        t.setForeground(Tema.GRAFITE);
        JLabel s = new JLabel(subtitulo);
        s.setForeground(Tema.TEXTO_SUAVE);
        cabecalho.add(t, BorderLayout.NORTH);
        cabecalho.add(s, BorderLayout.CENTER);

        campos.setBackground(Tema.SUPERFICIE);
        campos.setBorder(Tema.margem(18, 22));

        BotaoTema btnCancelar = new BotaoTema("Cancelar", BotaoTema.Tipo.NEUTRO);
        btnCancelar.addActionListener(e -> dispose());
        btnSalvar.addActionListener(e -> tentarSalvar());
        JLabel nota = new JLabel("<html><font color='#C62828'>*</font> campo obrigatório</html>");
        nota.setFont(Tema.FONTE_PEQUENA);
        nota.setForeground(Tema.TEXTO_SUAVE);
        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botoes.setOpaque(false);
        botoes.add(btnCancelar);
        botoes.add(btnSalvar);
        JPanel rodape = new JPanel(new BorderLayout());
        rodape.setBackground(Tema.FUNDO);
        rodape.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Tema.BORDA), Tema.margem(12, 22)));
        rodape.add(nota, BorderLayout.WEST);
        rodape.add(botoes, BorderLayout.EAST);

        getContentPane().add(cabecalho, BorderLayout.NORTH);
        getContentPane().add(campos, BorderLayout.CENTER);
        getContentPane().add(rodape, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(btnSalvar);
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    /**
     * Adiciona uma linha "rótulo + campo" ao formulário.
     *
     * @param nome        nome lógico, o mesmo usado pelo controller em ValidacaoException
     * @param rotulo      texto do rótulo
     * @param componente  campo de entrada
     * @param obrigatorio mostra o asterisco vermelho
     * @param dica        texto pequeno abaixo do campo (pode ser null)
     */
    protected final void adicionarCampo(String nome, String rotulo, JComponent componente,
            boolean obrigatorio, String dica) {
        porNome.put(nome, componente);
        JLabel l = new JLabel(obrigatorio
                ? "<html>" + rotulo + " <font color='#C62828'>*</font></html>" : rotulo);
        l.setFont(Tema.FONTE_NEGRITO);
        l.setForeground(Tema.GRAFITE_CLARO);
        l.setMinimumSize(l.getPreferredSize()); // o rótulo nunca quebra em duas linhas

        GridBagConstraints c = new GridBagConstraints();
        c.gridy = linha++;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 0, dica == null ? 14 : 2, 16);
        c.gridx = 0;
        campos.add(l, c);
        c.gridx = 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, dica == null ? 14 : 2, 0);
        campos.add(componente, c);
        if (dica != null) {
            JLabel d = new JLabel(dica);
            d.setFont(Tema.FONTE_PEQUENA);
            d.setForeground(Tema.TEXTO_SUAVE);
            c.gridy = linha++;
            c.insets = new Insets(0, 0, 14, 0);
            campos.add(d, c);
        }
    }

    /** Chamado pelo botão Salvar: envia os dados ao controller. Lance a exceção dele. */
    protected abstract void salvar() throws Exception;

    /** A subclasse chama no fim do construtor, depois de adicionar os campos. */
    protected final void iniciar() {
        pack();
        setLocationRelativeTo(dono);
    }

    /** Mostra o diálogo e espera. @return true se o usuário salvou com sucesso */
    public boolean exibir() {
        setVisible(true);
        return salvo;
    }

    private void tentarSalvar() {
        try {
            salvar();
            salvo = true;
            dispose();
        } catch (ValidacaoException ex) {
            JComponent campo = porNome.get(ex.getCampo());
            if (campo instanceof CampoTexto ct) {
                ct.setErro(true);
            }
            Mensagens.erro(this, ex);
            focar(campo);
        } catch (Exception ex) {
            Mensagens.erro(this, ex);
        }
    }

    private static void focar(JComponent campo) {
        if (campo instanceof JComboBox<?> combo && combo.isEditable()) {
            ((JComponent) combo.getEditor().getEditorComponent()).requestFocusInWindow();
        } else if (campo != null) {
            campo.requestFocusInWindow();
        }
    }
}
