package br.unip.aps.lojapneus.view;

import java.awt.Window;

import br.unip.aps.lojapneus.controller.ClienteController;
import br.unip.aps.lojapneus.model.Cliente;
import br.unip.aps.lojapneus.view.componentes.CampoTelefone;
import br.unip.aps.lojapneus.view.componentes.CampoTexto;

/** Formulário de cliente, usado para cadastrar (existente = null) e para editar. */
public class ClienteDialog extends FormularioDialog {

    private final ClienteController controller;
    private final Cliente existente;
    private final CampoTexto nome = new CampoTexto(28, "Ex.: Maria da Silva").limite(100);
    private final CampoTexto email = new CampoTexto(28, "Ex.: maria@email.com").limite(100);
    private final CampoTelefone telefone = new CampoTelefone();
    private final CampoTexto endereco = new CampoTexto(28, "Rua, número - cidade").limite(150);

    /**
     * @param dono       janela pai
     * @param controller controller de clientes
     * @param existente  cliente a editar, ou null para cadastrar um novo
     */
    public ClienteDialog(Window dono, ClienteController controller, Cliente existente) {
        super(dono, existente == null ? "Novo cliente" : "Editar cliente",
                existente == null ? "Preencha os dados para cadastrar."
                                  : "Altere os dados e clique em Salvar.");
        this.controller = controller;
        this.existente = existente;

        adicionarCampo("nome", "Nome", nome, true, null);
        adicionarCampo("email", "E-mail", email, false, "Opcional, mas não pode se repetir.");
        adicionarCampo("telefone", "Telefone", telefone, false, "Com DDD. Opcional.");
        adicionarCampo("endereco", "Endereço", endereco, false, "Opcional.");

        if (existente != null) {
            nome.setText(existente.getNome());
            email.setText(existente.getEmail());
            telefone.setText(existente.getTelefone());
            endereco.setText(existente.getEndereco());
        }
        iniciar();
    }

    @Override
    protected void salvar() throws Exception {
        if (existente == null) {
            controller.cadastrar(nome.getText(), email.getText(), telefone.getText(),
                    endereco.getText());
        } else {
            controller.atualizar(existente.getId(), nome.getText(), email.getText(),
                    telefone.getText(), endereco.getText());
        }
    }
}
