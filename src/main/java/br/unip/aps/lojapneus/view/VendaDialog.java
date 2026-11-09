package br.unip.aps.lojapneus.view;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import br.unip.aps.lojapneus.controller.ClienteController;
import br.unip.aps.lojapneus.controller.PneuController;
import br.unip.aps.lojapneus.controller.VendaController;
import br.unip.aps.lojapneus.model.Cliente;
import br.unip.aps.lojapneus.model.Pneu;
import br.unip.aps.lojapneus.model.PneuOrdem;
import br.unip.aps.lojapneus.model.Venda;
import br.unip.aps.lojapneus.view.componentes.CampoTexto;
import br.unip.aps.lojapneus.view.componentes.ComboTema;
import br.unip.aps.lojapneus.view.componentes.Mensagens;
import br.unip.aps.lojapneus.view.estilo.Formato;
import br.unip.aps.lojapneus.view.estilo.Tema;

/**
 * Formulário de nova venda: escolhe cliente e pneu, vê estoque e preço do pneu, digita a
 * quantidade e acompanha o total calculado na hora. O preço unitário não é digitado.
 */
public class VendaDialog extends FormularioDialog {

    private final VendaController controller;
    private final Venda existente;
    private final JComboBox<Cliente> cliente = new JComboBox<>();
    private final JComboBox<Pneu> pneu = new JComboBox<>();
    private final CampoTexto quantidade = new CampoTexto(8, "1").somenteDigitos(5);
    private final JLabel info = new JLabel(" ");
    private final JLabel total = new JLabel("R$ 0,00");

    /**
     * @param dono       janela pai
     * @param controller controller de vendas
     * @param clientes   controller de clientes (lista do combo)
     * @param pneus      controller de pneus (lista do combo)
     */
    public VendaDialog(Window dono, VendaController controller, ClienteController clientes,
            PneuController pneus) {
        this(dono, controller, clientes, pneus, null);
    }

    /**
     * @param existente venda a editar, ou null para registrar uma nova
     */
    public VendaDialog(Window dono, VendaController controller, ClienteController clientes,
            PneuController pneus, Venda existente) {
        super(dono, existente == null ? "Nova venda" : "Editar venda",
                existente == null ? "Escolha o cliente, o pneu e a quantidade."
                                  : "Corrija os dados da venda. O estoque é ajustado ao salvar.");
        this.existente = existente;
        this.controller = controller;

        try {
            for (Cliente c : clientes.listarTodos()) {
                cliente.addItem(c);
            }
            if (existente == null) {
                for (Pneu p : pneus.listarComEstoque()) { // só pneus que podem ser vendidos
                    pneu.addItem(p);
                }
            } else {
                // Edição: mostra também o pneu da venda (mesmo sem estoque) e conta de volta
                // a quantidade desta venda, que é devolvida ao estoque ao salvar.
                for (Pneu p : pneus.listar(null, PneuOrdem.MARCA_ASC, PneuOrdem.MODELO_ASC)) {
                    boolean atual = p.getId() == existente.getPneu().getId();
                    if (atual) {
                        pneu.addItem(new Pneu(p.getId(), p.getMarca(), p.getModelo(), p.getAro(),
                                p.getEstoque() + existente.getQuantidade(), p.getPreco()));
                    } else if (p.getEstoque() > 0) {
                        pneu.addItem(p);
                    }
                }
            }
        } catch (Exception ex) {
            Mensagens.erro(this, ex);
        }
        cliente.setSelectedIndex(-1);
        pneu.setSelectedIndex(-1);
        if (existente != null) {
            for (int i = 0; i < cliente.getItemCount(); i++) {
                if (cliente.getItemAt(i).getId() == existente.getCliente().getId()) {
                    cliente.setSelectedIndex(i);
                }
            }
            for (int i = 0; i < pneu.getItemCount(); i++) {
                if (pneu.getItemAt(i).getId() == existente.getPneu().getId()) {
                    pneu.setSelectedIndex(i);
                }
            }
        }
        cliente.setRenderer(new Renderizador("Selecione o cliente…"));
        pneu.setRenderer(new Renderizador("Selecione o pneu…"));
        ComboTema.aplicar(cliente);
        ComboTema.aplicar(pneu);
        int altura = quantidade.getPreferredSize().height;
        cliente.setPreferredSize(new Dimension(380, altura));
        pneu.setPreferredSize(new Dimension(380, altura));

        info.setFont(Tema.FONTE_PEQUENA.deriveFont(java.awt.Font.BOLD));
        info.setPreferredSize(new Dimension(380, altura));
        total.setFont(Tema.FONTE.deriveFont(java.awt.Font.BOLD, 22f));
        total.setForeground(Tema.LARANJA_ESCURO);
        quantidade.setText(existente == null ? "1" : String.valueOf(existente.getQuantidade()));

        adicionarCampo("cliente", "Cliente", cliente, true, null);
        adicionarCampo("pneu", "Pneu", pneu, true, null);
        adicionarCampo("info", "Disponível", info, false, null);
        adicionarCampo("quantidade", "Quantidade", quantidade, true, null);
        adicionarCampo("total", "Total", total, false, null);

        pneu.addActionListener(e -> atualizar());
        quantidade.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { atualizar(); }
            @Override public void removeUpdate(DocumentEvent e) { atualizar(); }
            @Override public void changedUpdate(DocumentEvent e) { atualizar(); }
        });
        atualizar();
        iniciar();
    }

    /** Recalcula a linha de estoque/preço e o total. */
    private void atualizar() {
        Pneu p = (Pneu) pneu.getSelectedItem();
        int q = 0;
        try {
            q = Integer.parseInt(quantidade.getText().trim());
        } catch (NumberFormatException ignorado) {
            // campo vazio: total zerado
        }
        if (p == null) {
            info.setText("Escolha um pneu para ver estoque e preço");
            info.setForeground(Tema.TEXTO_SUAVE);
            total.setText("R$ 0,00");
            return;
        }
        String base = "Estoque: " + p.getEstoque() + " un.  ·  Preço unitário: "
                + Formato.moeda(comPreco(p).getPreco());
        if (p.getEstoque() == 0) {
            info.setText("Sem estoque deste pneu");
            info.setForeground(Tema.VERMELHO);
        } else if (q > p.getEstoque()) {
            info.setText("Acima do estoque: só há " + p.getEstoque() + " un.");
            info.setForeground(Tema.VERMELHO);
        } else {
            info.setText(base);
            info.setForeground(Tema.GRAFITE_CLARO);
        }
        total.setText(Formato.moeda(controller.calcularTotal(comPreco(p), quantidade.getText())));
    }

    @Override
    protected void salvar() throws Exception {
        if (existente == null) {
            controller.registrar((Cliente) cliente.getSelectedItem(), (Pneu) pneu.getSelectedItem(),
                    quantidade.getText());
        } else {
            controller.atualizar(existente.getId(), (Cliente) cliente.getSelectedItem(),
                    (Pneu) pneu.getSelectedItem(), quantidade.getText());
        }
    }

    /** Mostra o estoque ao lado de cada pneu e um texto de dica quando nada está escolhido. */
    private static class Renderizador extends DefaultListCellRenderer {
        private final String dica;

        Renderizador(String dica) { this.dica = dica; }

        @Override
        public Component getListCellRendererComponent(JList<?> lista, Object valor, int indice,
                boolean selecionado, boolean foco) {
            String texto;
            if (valor == null) {
                texto = dica;
            } else if (valor instanceof Pneu p) {
                texto = p + "  —  " + (p.getEstoque() == 0 ? "sem estoque"
                        : p.getEstoque() + " un.");
            } else {
                texto = valor.toString();
            }
            super.getListCellRendererComponent(lista, texto, indice, false, false);
            setBorder(javax.swing.BorderFactory.createEmptyBorder(7, 10, 7, 10));
            setOpaque(true);
            setBackground(selecionado && indice >= 0 ? Tema.SELECAO : Color.WHITE);
            setForeground(Tema.TEXTO);
            if (valor == null) {
                setForeground(new Color(0xA0A8B3));
            } else if (valor instanceof Pneu p && p.getEstoque() == 0) {
                setForeground(Tema.VERMELHO);
            }
            return this;
        }
    }

    /** Na edição, se o pneu for o mesmo da venda, vale o preço unitário gravado na venda. */
    private Pneu comPreco(Pneu p) {
        if (existente != null && p.getId() == existente.getPneu().getId()) {
            return new Pneu(p.getId(), p.getMarca(), p.getModelo(), p.getAro(), p.getEstoque(),
                    existente.getPrecoUnitario());
        }
        return p;
    }

}
