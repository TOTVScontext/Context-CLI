package br.com.totvs.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionFactory {
    public static Connection abrirConexao() {
        Connection con = null;
        try {
            Class.forName("oracle.jdbc.driver.OracleDriver");
            String url = "jdbc:oracle:thin:@oracle.fiap.com.br:1521:ORCL";
            //----------------------------------------------------
            String user = System.getenv("ORACLE_USER");
            String pass = System.getenv("ORACLE_PASS");
            if (user == null || pass == null) {
                throw new IllegalStateException(
                        "Configure ORACLE_USER e ORACLE_PASS antes de executar"
                );
            }
            con = DriverManager.getConnection(url, user, pass);
            //---------------------------------------------------
            System.out.println("Conexao aberta");
        } catch (ClassNotFoundException e) {
            System.out.println("Erro: A classe de conexao nao foi encontrada" + e.getMessage());
        } catch (SQLException e) {
            System.out.println("Erro de SQL: " + e.getMessage());
        }
        return con;
    }

    public static void fecharConexao(Connection con) {
        if (con != null) {
            try {
                con.close();
                System.out.println("Conexao fechada");
            } catch (SQLException e) {
                System.out.println("Erro de SQL: " + e.getMessage());
            }
        }

    }

}