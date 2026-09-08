package br.com.totvs.main;

import br.com.totvs.dao.ConnectionFactory;
import br.com.totvs.dao.ReuniaoDAO;
import br.com.totvs.dao.ReuniaoDAOImpl;
import br.com.totvs.dto.Reuniao;

import java.sql.Connection;
import java.sql.Timestamp;

public class TesteInsercaoReuniao {
    public static void main(String[] args) throws Exception {
        Connection con = null;

        try {
            con = ConnectionFactory.abrirConexao();

            Reuniao reuniao = new Reuniao();
            reuniao.setIdReuniao("TESTE-002");
            reuniao.setTranscricao("Teste de insercao de reuniao pelo Java 25.");
            reuniao.setParticipantes("Aluno");
            reuniao.setOrigemEntrada("TEXTO");
            reuniao.setDataAnalise(new Timestamp(System.currentTimeMillis()));
            reuniao.setPredRisco(0);
            reuniao.setPredOportunidade(0);
            reuniao.setSentimento("Neutro");
            reuniao.setProdutosDetectados(null);
            reuniao.setModeloAnalise("teste-manual");

            ReuniaoDAO dao = new ReuniaoDAOImpl(con);
            System.out.println(dao.inserir(reuniao));
        } finally {
            ConnectionFactory.fecharConexao(con);
        }
    }
}
