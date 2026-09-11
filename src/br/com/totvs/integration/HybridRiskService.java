package br.com.totvs.integration;

import br.com.totvs.model.Analysis;
import br.com.totvs.model.Conversation;

import java.io.IOException;

public class HybridRiskService {
    private static final double LIMIAR_CONFIANCA = 0.65;

    private final PythonModelClient pythonModelClient;

    public HybridRiskService() {
        this.pythonModelClient = new PythonModelClient();
    }

    public DecisaoRisco avaliar(Conversation conversation,
                                Analysis analysis)
            throws IOException, InterruptedException {

        String respostaJson = pythonModelClient.predict(conversation.getText());
        PredicaoModelo predicao = PredicaoModelo.fromJson(respostaJson);

        double confiancaModelo = predicao.isRisco()
                ? predicao.getProbabilidadeRisco()
                : 1.0 - predicao.getProbabilidadeRisco();

        if (confiancaModelo >= LIMIAR_CONFIANCA) {
            return new DecisaoRisco(
                    predicao.isRisco(),
                    confiancaModelo,
                    "MODELO_PYTHON",
                    predicao.getClasse(),
                    predicao.getProbabilidadeRisco()
            );
        }

        boolean riscoPelasRegras = analysis.getSentiment() < 7.0;

        return new DecisaoRisco(
                riscoPelasRegras,
                confiancaModelo,
                "FALLBACK_REGRAS_JAVA",
                predicao.getClasse(),
                predicao.getProbabilidadeRisco()
        );
    }

    public static class DecisaoRisco {
        private final boolean risco;
        private final double confianca;
        private final String fonte;
        private final String classeModelo;
        private final double probabilidadeModelo;

        public DecisaoRisco(boolean risco,
                            double confianca,
                            String fonte,
                            String classeModelo,
                            double probabilidadeModelo) {
            this.risco = risco;
            this.confianca = confianca;
            this.fonte = fonte;
            this.classeModelo = classeModelo;
            this.probabilidadeModelo = probabilidadeModelo;
        }

        public boolean isRisco() {
            return risco;
        }

        public double getConfianca() {
            return confianca;
        }

        public String getFonte() {
            return fonte;
        }

        public String getClasseModelo() {
            return classeModelo;
        }

        public double getProbabilidadeModelo() {
            return probabilidadeModelo;
        }
    }
}
