package br.com.totvs.domain;

import java.util.ArrayList;
import java.util.List;

public class InsightService {
    private double alertThreshold;

    public InsightService(double alertThreshold) {
        this.alertThreshold = alertThreshold;
    }

    public boolean verificarRiscoChurn(Analysis analysis) {
        return analysis.getSentiment() < this.alertThreshold;
    }

    public boolean verificarOportunidadeUpsell(Analysis analysis) {
        return analysis.getProductivity() >= 8.0;
    }

    public boolean verificarReclamacao(Analysis analysis) {
        return analysis.isHasComplaint();
    }

    public boolean verificarBudget(Analysis analysis) {
        return analysis.isHasBudget();
    }

    public List<Insight> generate(Analysis a) {
        List<Insight> insights = new ArrayList<>();

        if (verificarRiscoChurn(a)) {
            insights.add(new RiskInsight("Risco de Churn detectado!", 1, 80.0));
        }

        if (verificarOportunidadeUpsell(a)) {
            insights.add(new BusinessInsight("Oportunidade de Upsell!", 2, 50000.0));
        }

        if (verificarReclamacao(a)) {
            insights.add(new Insight(
                    "Reclamacao de produto identificada. Acionar suporte proativamente.",
                    3
            ));
        }

        if (verificarBudget(a)) {
            insights.add(new Insight(
                    "Budget mencionado na conversa. Registrar para proposta comercial.",
                    2
            ));
        }

        // Regras diferenciais já existentes no projeto
        if (a.isHasPersona()) {
            insights.add(new Insight(
                    "Decisor identificado (CFO/Diretor/Gestor). Personalizar abordagem de ROI.",
                    2
            ));
        }

        if (a.isHasMixedSentiment()) {
            insights.add(new Insight(
                    "Sentimento misto detectado. Cliente satisfeito parcialmente, risco latente.",
                    3
            ));
        }

        if (a.isHasTrust()) {
            insights.add(new Insight(
                    "Cliente expressou confianca no vendedor. Momento ideal para fechar proposta.",
                    1
            ));
        }

        return insights;
    }
}
