package br.unip.aps.lojapneus;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import javax.swing.SwingUtilities;

import br.unip.aps.lojapneus.controller.ClienteController;
import br.unip.aps.lojapneus.controller.PneuController;
import br.unip.aps.lojapneus.controller.VendaController;
import br.unip.aps.lojapneus.dao.ClienteDAO;
import br.unip.aps.lojapneus.dao.ClienteDAOJdbc;
import br.unip.aps.lojapneus.dao.PneuDAO;
import br.unip.aps.lojapneus.dao.PneuDAOJdbc;
import br.unip.aps.lojapneus.dao.VendaDAO;
import br.unip.aps.lojapneus.dao.VendaDAOJdbc;
import br.unip.aps.lojapneus.dao.memoria.BancoMemoria;
import br.unip.aps.lojapneus.dao.memoria.ClienteDAOMemoria;
import br.unip.aps.lojapneus.dao.memoria.PneuDAOMemoria;
import br.unip.aps.lojapneus.dao.memoria.VendaDAOMemoria;
import br.unip.aps.lojapneus.util.ConnectionFactory;
import br.unip.aps.lojapneus.view.MainFrame;
import br.unip.aps.lojapneus.view.componentes.Mensagens;
import br.unip.aps.lojapneus.view.estilo.Tema;

/**
 * Ponto de entrada: monta as camadas (DAO → controller → view) e abre a janela.
 * É o único lugar que conhece as implementações JDBC; a view só conhece controllers.
 * <p>
 * Uso normal: usa o MySQL. Sem banco (ou com o argumento {@code --memoria}), abre em
 * modo demonstração, com dados na memória.
 */
public final class Main {

    private Main() { }

    /** Inicia o sistema. */
    public static void main(String[] args) {
        boolean memoria = Arrays.asList(args).contains("--memoria");
        String falha = null;
        if (!memoria) {
            try (Connection teste = ConnectionFactory.getConnection()) {
                teste.isValid(2);
            } catch (SQLException e) {
                memoria = true;
                falha = "Não foi possível conectar ao banco de dados MySQL.\n"
                      + "Confira se ele está ligado e se o arquivo db.properties está correto."
                      + (e.getMessage() != null && e.getMessage().contains("db.properties")
                              ? "\n\nDetalhe: " + e.getMessage() + "\n\n" : "\n\n")
                      + "O sistema abriu em MODO DEMONSTRAÇÃO: os dados ficam só na memória.";
            }
        }
        ClienteDAO clienteDao;
        PneuDAO pneuDao;
        VendaDAO vendaDao;
        if (memoria) {
            BancoMemoria banco = new BancoMemoria();
            clienteDao = new ClienteDAOMemoria(banco);
            pneuDao = new PneuDAOMemoria(banco);
            vendaDao = new VendaDAOMemoria(banco);
        } else {
            clienteDao = new ClienteDAOJdbc();
            pneuDao = new PneuDAOJdbc();
            vendaDao = new VendaDAOJdbc();
        }
        final boolean demonstracao = memoria;
        final String aviso = falha;
        SwingUtilities.invokeLater(() -> {
            Tema.instalar();
            MainFrame janela = new MainFrame(new ClienteController(clienteDao),
                    new PneuController(pneuDao), new VendaController(vendaDao, pneuDao), demonstracao);
            janela.setVisible(true);
            if (aviso != null) {
                Mensagens.aviso(janela, "Sem conexão com o banco", aviso);
            }
        });
    }
}
