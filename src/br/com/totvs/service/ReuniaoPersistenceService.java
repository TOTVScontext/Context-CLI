package br.com.totvs.service;

import br.com.totvs.dao.ConnectionFactory;
import br.com.totvs.dao.ReuniaoDAO;
import br.com.totvs.dao.ReuniaoDAOImpl;
import br.com.totvs.domain.Analysis;
import br.com.totvs.domain.Conversation;
import br.com.totvs.dto.Reuniao;
import br.com.totvs.integration.HybridRiskService;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Timestamp;

public class ReuniaoPersistenceService {
    private final HybridRiskService hybridRiskService;

    public ReuniaoPersistenceService() {
        this.hybridRiskService = new HybridRiskService();
    }

    public String salvar(Conversation conversation,
                         Analysis analysis,
                         String origemEntrada,
                         String modeloAnalise) {
        boolean risco;
        String fonteDecisao;
        String classeModelo;
        BigDecimal probabilidadeModelo;

        try {
            HybridRiskService.DecisaoRisco decisao =
                    hybridRiskService.avaliar(conversation, analysis);

            risco = decisao.isRisco();
            fonteDecisao = decisao.getFonte();
            classeModelo = decisao.getClasseModelo();
            probabilidadeModelo = BigDecimal.valueOf(
                    decisao.getProbabilidadeModelo()
            );
        } catch (IOException e) {
            risco = analysis.getSentiment() < 7.0;
            fonteDecisao = "FALLBACK_REGRAS_JAVA_ERRO_MODELO";
            classeModelo = "INDISPONIVEL";
            probabilidadeModelo = null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            risco = analysis.getSentiment() < 7.0;
            fonteDecisao = "FALLBACK_REGRAS_JAVA_INTERRUPCAO";
            classeModelo = "INDISPONIVEL";
            probabilidadeModelo = null;
        }

        Connection con = null;

        try {
            con = ConnectionFactory.abrirConexao();

            if (con == null) {
                return "Conexao nao estabelecida";
            }

            Reuniao reuniao = new Reuniao();
            reuniao.setIdReuniao(conversation.getId());
            reuniao.setTranscricao(conversation.getText());
            reuniao.setParticipantes(
                    String.join(", ", conversation.getParticipants())
            );
            reuniao.setOrigemEntrada(origemEntrada);
            reuniao.setDataAnalise(
                    new Timestamp(System.currentTimeMillis())
            );
            reuniao.setPredRisco(risco ? 1 : 0);
            reuniao.setPredOportunidade(
                    analysis.getProductivity() >= 8.0 ? 1 : 0
            );
            reuniao.setSentimento(
                    analysis.isGood() ? "Positivo" : "Negativo"
            );
            reuniao.setProdutosDetectados(
                    conversation.getText().toLowerCase().contains("rm")
                            ? "RM"
                            : null
            );
            reuniao.setModeloAnalise(modeloAnalise);
            reuniao.setClasseModelo(classeModelo);
            reuniao.setProbabilidadeModelo(probabilidadeModelo);
            reuniao.setFonteDecisao(fonteDecisao);

            ReuniaoDAO dao = new ReuniaoDAOImpl(con);
            return dao.inserir(reuniao);
        } finally {
            ConnectionFactory.fecharConexao(con);
        }
    }
}
