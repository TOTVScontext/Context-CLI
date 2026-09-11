package br.com.totvs.model;

public class RiskInsight extends Insight{
    private double churnProb;

    public RiskInsight(String message, int priority, double churnProb){
        super(message, priority);
        this.churnProb = churnProb;
    }

    public double getChurnProb() {
        return churnProb;
    }

    @Override
    public String getMessage() {
        return "[ALERTA DE RISCO - Nível " + super.getPriority() + "] " + super.getMessage()
                + " | Risco de Churn: " + String.format(java.util.Locale.of("pt", "BR"), "%.1f", this.churnProb) + "%";
    }
}
