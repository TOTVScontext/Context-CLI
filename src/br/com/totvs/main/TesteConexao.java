package br.com.totvs.main;

import br.com.totvs.dao.ConnectionFactory;

import java.sql.Connection;

public class TesteConexao {
    static void main() {
        Connection con = ConnectionFactory.abrirConexao();

        if (con != null) {
            System.out.println("Teste de conexao realizado com sucesso");
        }

        ConnectionFactory.fecharConexao(con);
    }
}
