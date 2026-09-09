package br.com.totvs.domain;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Gera os insights de negócio a partir de uma {@link Analysis} já calculada.
 * <p>
 * Nenhum insight carrega números "de exemplo": a probabilidade de churn vem
 * de {@link Analysis#getChurnProbability()} (derivada do sentimento real da
 * conversa) e o valor de oportunidade vem de {@link Analysis#getBudgetValueDetected()}
 * (valor efetivamente citado na transcrição). Quando a transcrição não traz
 * um valor monetário, o insight de upsell é gerado sem valor — em vez de
 * receber um número inventado.
 */
public class InsightService {
    private final double alertThreshold;

    private static final NumberFormat FORMATO_MOEDA =
            NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

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
            String contexto = a.getCompanyName() != null
                    ? " (" + a.getCompanyName() + ")"
                    : "";
            insights.add(new RiskInsight(
                    "Risco de Churn detectado" + contexto + ". Sentimento medido: "
                            + String.format(Locale.of("pt", "BR"), "%.1f/10", a.getSentiment()) + ".",
                    prioridadeChurn(a),
                    a.getChurnProbability()
            ));
        }

        if (verificarOportunidadeUpsell(a)) {
            if (a.getBudgetValueDetected() != null) {
                insights.add(new BusinessInsight(
                        "Oportunidade de Upsell identificada. Valor mencionado na conversa: "
                                + FORMATO_MOEDA.format(a.getBudgetValueDetected()) + ".",
                        2,
                        a.getUpsellPotentialValue()
                ));
            } else {
                insights.add(new Insight(
                        "Oportunidade de Upsell identificada pelo alto interesse comercial "
                                + "(produtividade " + String.format(Locale.of("pt", "BR"), "%.1f/10", a.getProductivity())
                                + "), mas nenhum valor de investimento foi mencionado na transcrição. "
                                + "Levantar orçamento na próxima interação.",
                        2
                ));
            }
        }

        if (verificarReclamacao(a)) {
            insights.add(new Insight(
                    "Reclamação de produto identificada (" + a.getComplaintTermCount()
                            + " termo(s) de insatisfação na transcrição). Acionar suporte proativamente.",
                    3
            ));
        }

        if (verificarBudget(a)) {
            String valor = a.getBudgetValueDetected() != null
                    ? " Valor identificado: " + FORMATO_MOEDA.format(a.getBudgetValueDetected()) + "."
                    : "";
            insights.add(new Insight(
                    "Budget mencionado na conversa." + valor + " Registrar para proposta comercial.",
                    2
            ));
        }

        if (a.isHasPersona()) {
            insights.add(new Insight(
                    "Decisor identificado (CFO/Diretor/Gestor). Personalizar abordagem de ROI.",
                    2
            ));
        }

        if (a.isHasMixedSentiment()) {
            insights.add(new Insight(
                    "Sentimento misto detectado (" + a.getPositiveTermCount() + " termo(s) positivo(s) e "
                            + a.getNegativeTermCount() + " negativo(s)). Cliente satisfeito parcialmente, risco latente.",
                    3
            ));
        }

        if (a.isHasTrust()) {
            insights.add(new Insight(
                    "Cliente expressou confiança no vendedor. Momento ideal para fechar proposta.",
                    1
            ));
        }

        return insights;
    }

    /** Prioridade 1 (mais urgente) quando o churn é acompanhado de reclamação explícita. */
    private int prioridadeChurn(Analysis a) {
        return a.isHasComplaint() ? 1 : 2;
    }
}
