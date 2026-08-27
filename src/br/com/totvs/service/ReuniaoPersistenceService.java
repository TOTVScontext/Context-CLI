package br.com.totvs.service;

import br.com.totvs.dao.ConnectionFactory;
import br.com.totvs.dao.ReuniaoDAO;
import br.com.totvs.dao.ReuniaoDAOImpl;
import br.com.totvs.domain.Analysis;
import br.com.totvs.domain.Conversation;
import br.com.totvs.dto.Reuniao;

import java.sql.Connection;
import java.sql.Timestamp;

public class ReuniaoPersistenceService {

    public String salvar(Conversation conversation,
                         Analysis analysis,
                         String origemEntrada,
                         String modeloAnalise) {
        Connection con = null;

        try {
            con = ConnectionFactory.abrirConexao();

            if (con == null) {
                return "Conexao nao estabelecida";
            }

            Reuniao reuniao = new Reuniao();
            reuniao.setIdReuniao(conversation.getId());
            reuniao.setTranscricao(conversation.getText());
            reuniao.setParticipantes(String.join(", ", conversation.getParticipants()));
            reuniao.setOrigemEntrada(origemEntrada);
            reuniao.setDataAnalise(new Timestamp(System.currentTimeMillis()));
            reuniao.setPredRisco(analysis.getSentiment() < 7.0 ? 1 : 0);
            reuniao.setPredOportunidade(analysis.getProductivity() >= 8.0 ? 1 : 0);
            reuniao.setSentimento(analysis.isGood() ? "Positivo" : "Negativo");
            reuniao.setProdutosDetectados(
                    conversation.getText().toLowerCase().contains("rm") ? "RM" : null
            );
            reuniao.setModeloAnalise(modeloAnalise);

            ReuniaoDAO dao = new ReuniaoDAOImpl(con);
            return dao.inserir(reuniao);
        } finally {
            ConnectionFactory.fecharConexao(con);
        }
    }
}