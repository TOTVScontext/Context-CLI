package br.com.totvs.main;

import br.com.totvs.dao.ConnectionFactory;
import br.com.totvs.dao.ReuniaoDAO;
import br.com.totvs.dao.ReuniaoDAOImpl;
import br.com.totvs.dto.Reuniao;

import java.sql.Connection;
import java.sql.Timestamp;

public class TesteAlterarReuniao {
    public static void main(String[] args) throws Exception {
        Connection con = null;

        try {
            con = ConnectionFactory.abrirConexao();

            Reuniao reuniao = new Reuniao();
            reuniao.setIdReuniao("TESTE-001");
            reuniao.setTranscricao("Transcricao alterada pelo teste de UPDATE.");
            reuniao.setParticipantes("Aluno e Professor");
            reuniao.setOrigemEntrada("TEXTO");
            reuniao.setDataAnalise(new Timestamp(System.currentTimeMillis()));
            reuniao.setPredRisco(1);
            reuniao.setPredOportunidade(0);
            reuniao.setSentimento("Negativo");
            reuniao.setProdutosDetectados("RM");
            reuniao.setModeloAnalise("teste-update");

            ReuniaoDAO dao = new ReuniaoDAOImpl(con);
            System.out.println(dao.alterar(reuniao));
        } finally {
            ConnectionFactory.fecharConexao(con);
        }
    }
}
