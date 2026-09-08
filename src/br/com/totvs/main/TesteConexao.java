package br.com.totvs.main;

import br.com.totvs.dao.ConnectionFactory;

import java.sql.Connection;

public class TesteConexao {
    public static void main(String[] args) {
        Connection con = null;
        try {
            con = ConnectionFactory.abrirConexao();
            System.out.println("Teste de conexao realizado com sucesso");
        } catch (Exception e) {
            System.out.println("Falha ao conectar: " + e.getMessage());
        } finally {
            ConnectionFactory.fecharConexao(con);
        }
    }
}
