package br.com.totvs.main;

import br.com.totvs.dao.ConnectionFactory;
import br.com.totvs.dao.ReuniaoDAO;
import br.com.totvs.dao.ReuniaoDAOImpl;

import java.sql.Connection;

public class TesteExcluirReuniao {
    public static void main(String[] args) {
        Connection con = null;

        try {
            con = ConnectionFactory.abrirConexao();

            ReuniaoDAO dao = new ReuniaoDAOImpl(con);
            System.out.println(dao.excluir("TESTE-002"));
        } finally {
            ConnectionFactory.fecharConexao(con);
        }
    }
}
