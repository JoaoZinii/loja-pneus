package br.unip.aps.lojapneus.view;

import java.awt.Window;
import javax.swing.JComboBox;

import br.unip.aps.lojapneus.controller.PneuController;
import br.unip.aps.lojapneus.model.Pneu;
import br.unip.aps.lojapneus.view.componentes.CampoTexto;
import br.unip.aps.lojapneus.view.componentes.ComboTema;
import br.unip.aps.lojapneus.view.estilo.Formato;

/** Formulário de pneu, usado para cadastrar (existente = null) e para editar. */
public class PneuDialog extends FormularioDialog {

    private final PneuController controller;
    private final Pneu existente;
    private final CampoTexto marca = new CampoTexto(26, "Ex.: Pirelli").limite(50);
    private final CampoTexto modelo = new CampoTexto(26, "Ex.: Cinturato P1").limite(80);
    private final JComboBox<String> aro = new JComboBox<>();
    private final CampoTexto estoque = new CampoTexto(10, "0").somenteDigitos(6);
    private final CampoTexto preco = new CampoTexto(10, "0,00")
            .permitir(c -> Character.isDigit(c) || c == ',' || c == '.', 10);

    /**
     * @param dono       janela pai
     * @param controller controller de pneus
     * @param existente  pneu a editar, ou null para cadastrar um novo
     */
    public PneuDialog(Window dono, PneuController controller, Pneu existente) {
        super(dono, existente == null ? "Novo pneu" : "Editar pneu",
                existente == null ? "Preencha os dados para cadastrar no estoque."
                                  : "Altere os dados e clique em Salvar.");
        this.controller = controller;
        this.existente = existente;

        for (int a = 13; a <= 22; a++) {
            aro.addItem(String.valueOf(a));
        }
        aro.setEditable(true);
        ComboTema.aplicar(aro);
        aro.setSelectedItem("15");
        aro.setPreferredSize(new java.awt.Dimension(aro.getPreferredSize().width,
                estoque.getPreferredSize().height));

        adicionarCampo("marca", "Marca", marca, true, null);
        adicionarCampo("modelo", "Modelo", modelo, true, null);
        adicionarCampo("aro", "Aro", aro, true, "Escolha na lista ou digite.");
        adicionarCampo("estoque", "Estoque", estoque, true, "Quantidade disponível (unidades).");
        adicionarCampo("preco", "Preço de venda (R$)", preco, true, "Aceita vírgula: 589,90");

        if (existente != null) {
            marca.setText(existente.getMarca());
            modelo.setText(existente.getModelo());
            aro.setSelectedItem(String.valueOf(existente.getAro()));
            estoque.setText(String.valueOf(existente.getEstoque()));
            preco.setText(Formato.decimal(existente.getPreco()));
        }
        iniciar();
    }

    @Override
    protected void salvar() throws Exception {
        String aroTexto = String.valueOf(aro.getEditor().getItem());
        if (existente == null) {
            controller.cadastrar(marca.getText(), modelo.getText(), aroTexto,
                    preco.getText(), estoque.getText());
        } else {
            controller.atualizar(existente.getId(), marca.getText(), modelo.getText(),
                    aroTexto, preco.getText(), estoque.getText());
        }
    }
}
