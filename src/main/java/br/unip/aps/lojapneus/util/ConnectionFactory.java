package br.unip.aps.lojapneus.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Fabrica de conexoes com o banco de dados MySQL.
 *
 * <p>URL, usuario e senha NAO ficam no codigo: sao lidos do arquivo
 * {@code db.properties}, que fica na pasta raiz do projeto (a pasta de
 * trabalho da aplicacao) e nunca vai para o Git. O repositorio guarda
 * apenas o modelo {@code db.properties.example}.</p>
 *
 * <p>Cada chamada de {@link #getConnection()} abre uma conexao nova.
 * Quem recebe a conexao e responsavel por fecha-la, de preferencia com
 * try-with-resources.</p>
 */
public final class ConnectionFactory {

    /** Nome do arquivo de configuracao, procurado na pasta de trabalho. */
    private static final String ARQUIVO_CONFIG = "db.properties";

    private static Properties config;

    private ConnectionFactory() {
        // classe utilitaria: nao deve ser instanciada
    }

    /**
     * Abre uma nova conexao com o banco, usando os dados do db.properties.
     *
     * @return uma conexao aberta, que deve ser fechada por quem a recebe
     * @throws SQLException se o arquivo de configuracao nao puder ser lido
     *                      ou se o MySQL recusar a conexao
     */
    public static Connection getConnection() throws SQLException {
        Properties p = carregarConfig();
        return DriverManager.getConnection(
                p.getProperty("db.url"),
                p.getProperty("db.user"),
                p.getProperty("db.password"));
    }

    /**
     * Le o db.properties uma unica vez e guarda o resultado em memoria.
     *
     * @return as propriedades de conexao ja validadas
     * @throws SQLException se o arquivo nao existir ou estiver incompleto
     */
    private static synchronized Properties carregarConfig() throws SQLException {
        if (config != null) {
            return config;
        }
        Properties p = new Properties();
        try (FileInputStream in = new FileInputStream(ARQUIVO_CONFIG)) {
            p.load(in);
        } catch (IOException e) {
            throw new SQLException("Nao foi possivel ler o arquivo "
                    + ARQUIVO_CONFIG + ". Copie o db.properties.example "
                    + "para db.properties e preencha seus dados.", e);
        }
        for (String chave : new String[] {"db.url", "db.user", "db.password"}) {
            if (p.getProperty(chave) == null) {
                throw new SQLException("Falta a chave '" + chave
                        + "' no arquivo " + ARQUIVO_CONFIG + ".");
            }
        }
        config = p;
        return config;
    }
}