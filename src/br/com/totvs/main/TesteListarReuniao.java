package br.com.totvs.main;

import br.com.totvs.dao.ConnectionFactory;
import br.com.totvs.dao.ReuniaoDAO;
import br.com.totvs.dao.ReuniaoDAOImpl;
import br.com.totvs.dto.Reuniao;

import java.sql.Connection;
import java.util.List;

public class TesteListarReuniao {
    public static void main(String[] args) throws Exception {
        Connection con = null;

        try {
            con = ConnectionFactory.abrirConexao();

            ReuniaoDAO dao = new ReuniaoDAOImpl(con);
            List<Reuniao> reunioes = dao.listarTodos();

            System.out.println("Quantidade de reunioes: " + reunioes.size());

            for (Reuniao reuniao : reunioes) {
                System.out.println(
                        reuniao.getIdReuniao()
                                + " | " + reuniao.getOrigemEntrada()
                                + " | " + reuniao.getSentimento()
                                + " | " + reuniao.getModeloAnalise()
                );
            }
        } finally {
            ConnectionFactory.fecharConexao(con);
        }
    }
}
