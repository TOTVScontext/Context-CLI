package br.com.totvs.main;

import br.com.totvs.integration.PythonModelClient;
import br.com.totvs.integration.PredicaoModelo;
import br.com.totvs.domain.Analysis;
import br.com.totvs.domain.Conversation;
import br.com.totvs.integration.HybridRiskService;



public class TesteModeloPython {
    public static void main(String[] args) {
        PythonModelClient client = new PythonModelClient();

        try {
            String resposta = client.predict(
                    "O cliente gostou do RM e quer comprar uma proposta."
            );

            System.out.println("Resposta do modelo Python:");
            System.out.println(resposta);

            PredicaoModelo predicao = PredicaoModelo.fromJson(resposta);

            System.out.println("Classe: " + predicao.getClasse());
            System.out.println("Risco: " + predicao.isRisco());
            System.out.println("Probabilidade de risco: "
                    + predicao.getProbabilidadeRisco());

            Conversation conversation = new Conversation(
                    "IA-TESTE-001",
                    "O cliente gostou do RM e quer comprar uma proposta.",
                    java.util.Arrays.asList("Vendedor", "Cliente")
            );

            Analysis analysis = new Analysis(
                    9.5, 8.0, 5.0,
                    false, false, false, false, false
            );

            HybridRiskService hybridRiskService = new HybridRiskService();
            HybridRiskService.DecisaoRisco decisao =
                    hybridRiskService.avaliar(conversation, analysis);

            System.out.println("Decisao final de risco: " + decisao.isRisco());
            System.out.println("Confianca final: " + decisao.getConfianca());
            System.out.println("Fonte da decisao: " + decisao.getFonte());

        } catch (Exception e) {
            System.out.println("Erro ao chamar o modelo Python: " + e.getMessage());
        }
    }
}
