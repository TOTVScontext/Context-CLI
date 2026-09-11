package br.com.totvs.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Fábrica de conexões JDBC com o banco Oracle utilizado pelo Context-CLI.
 * <p>
 * As credenciais podem ser sobrescritas através das variáveis de ambiente
 * {@code ORACLE_USER}, {@code ORACLE_PASSWORD} e {@code ORACLE_URL}, o que
 * evita a necessidade de alterar o código-fonte para trocar de ambiente
 * (desenvolvimento, homologação, FIAP, etc.). Caso não sejam informadas,
 * são utilizados os valores padrão do ambiente acadêmico da FIAP.
 */
public final class ConnectionFactory {

    private static final String DRIVER_JDBC = "oracle.jdbc.driver.OracleDriver";

    private static final String URL_PADRAO = "jdbc:oracle:thin:@oracle.fiap.com.br:1521:ORCL";
    private static final String USUARIO_PADRAO = "rm564929";
    private static final String SENHA_PADRAO = "060207";

    private ConnectionFactory() {
    }

    private static String resolverUrl() {
        String url = System.getenv("ORACLE_URL");
        return (url == null || url.isBlank()) ? URL_PADRAO : url.trim();
    }

    private static String resolverUsuario() {
        String usuario = System.getenv("ORACLE_USER");
        return (usuario == null || usuario.isBlank()) ? USUARIO_PADRAO : usuario.trim();
    }

    private static String resolverSenha() {
        String senha = System.getenv("ORACLE_PASSWORD");
        return (senha == null || senha.isBlank()) ? SENHA_PADRAO : senha;
    }

    /**
     * Abre uma nova conexão com o banco Oracle.
     *
     * @return a conexão aberta.
     * @throws IllegalStateException se o driver JDBC não estiver no classpath.
     * @throws SQLException          se a conexão não puder ser estabelecida.
     */
    public static Connection abrirConexao() throws SQLException {
        try {
            Class.forName(DRIVER_JDBC);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(
                    "Driver JDBC do Oracle não encontrado no classpath (ojdbc.jar ausente).", e);
        }

        return DriverManager.getConnection(resolverUrl(), resolverUsuario(), resolverSenha());
    }

    /**
     * Fecha a conexão informada silenciando (mas registrando) eventuais falhas,
     * de forma que o encerramento de recursos nunca mascare o resultado real
     * de uma operação de negócio.
     */
    public static void fecharConexao(Connection con) {
        if (con == null) {
            return;
        }
        try {
            if (!con.isClosed()) {
                con.close();
            }
        } catch (SQLException e) {
            System.err.println("Aviso: falha ao fechar conexão com o banco: " + e.getMessage());
        }
    }
}
