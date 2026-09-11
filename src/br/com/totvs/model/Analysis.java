package br.com.totvs.model;

import java.math.BigDecimal;

/**
 * Resultado da análise de uma {@link Conversation}.
 * <p>
 * Além das métricas clássicas (produtividade, sentimento, resolução), carrega
 * os sinais quantitativos extraídos diretamente da transcrição — probabilidade
 * de churn, valor de oportunidade, empresa e duração da reunião — para que
 * {@link InsightService} não precise recorrer a números fixos.
 */
public class Analysis {
    private double productivity;
    private double sentiment;
    private double resolution;
    private boolean hasComplaint;
    private boolean hasBudget;
    private boolean hasPersona;
    private boolean hasMixedSentiment;
    private boolean hasTrust;

    // Sinais adicionais extraídos da transcrição real (podem ser nulos/zero
    // quando o sinal não foi encontrado — nunca são preenchidos com valores
    // fixos "de exemplo").
    private String companyName;
    private Integer meetingDurationMinutes;
    private boolean durationEstimated;
    private BigDecimal budgetValueDetected;
    private double churnProbability;
    private double upsellPotentialValue;
    private int complaintTermCount;
    private int positiveTermCount;
    private int negativeTermCount;

    public Analysis(double productivity, double sentiment, double resolution,
                    boolean hasComplaint, boolean hasBudget, boolean hasPersona,
                    boolean hasMixedSentiment, boolean hasTrust) {
        this(productivity, sentiment, resolution, hasComplaint, hasBudget, hasPersona,
                hasMixedSentiment, hasTrust, null, null, false, null, 0, 0, 0);
    }

    public Analysis(double productivity, double sentiment, double resolution,
                    boolean hasComplaint, boolean hasBudget, boolean hasPersona,
                    boolean hasMixedSentiment, boolean hasTrust,
                    String companyName, Integer meetingDurationMinutes, boolean durationEstimated,
                    BigDecimal budgetValueDetected, int complaintTermCount,
                    int positiveTermCount, int negativeTermCount) {
        this.productivity = productivity;
        this.sentiment = sentiment;
        this.resolution = resolution;
        this.hasComplaint = hasComplaint;
        this.hasBudget = hasBudget;
        this.hasPersona = hasPersona;
        this.hasMixedSentiment = hasMixedSentiment;
        this.hasTrust = hasTrust;
        this.companyName = companyName;
        this.meetingDurationMinutes = meetingDurationMinutes;
        this.durationEstimated = durationEstimated;
        this.budgetValueDetected = budgetValueDetected;
        this.complaintTermCount = complaintTermCount;
        this.positiveTermCount = positiveTermCount;
        this.negativeTermCount = negativeTermCount;

        // Probabilidade de churn: derivada continuamente do sentimento real
        // (0-10) e reforçada pelos sinais qualitativos já extraídos do texto —
        // não é um valor fixo.
        double base = (10.0 - sentiment) * 10.0; // sentiment=0 -> 100 ; sentiment=10 -> 0
        if (hasComplaint) base += 8.0;
        if (hasMixedSentiment) base += 5.0;
        if (hasTrust) base -= 10.0;
        this.churnProbability = clamp(base, 2.0, 97.0);

        // Valor de oportunidade: usa o valor de budget realmente mencionado na
        // conversa, quando existe. Sem menção de valor, não há número —
        // fica em zero e a camada de apresentação trata isso de forma explícita.
        this.upsellPotentialValue = budgetValueDetected != null ? budgetValueDetected.doubleValue() : 0.0;
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    public boolean isGood() {
        return this.sentiment >= 7.0;
    }

    public double getProductivity() { return productivity; }
    public void setProductivity(double productivity) { this.productivity = productivity; }
    public double getSentiment() { return sentiment; }
    public void setSentiment(double sentiment) { this.sentiment = sentiment; }
    public double getResolution() { return resolution; }
    public void setResolution(double resolution) { this.resolution = resolution; }
    public boolean isHasComplaint() { return hasComplaint; }
    public boolean isHasBudget() { return hasBudget; }
    public boolean isHasPersona() { return hasPersona; }
    public boolean isHasMixedSentiment() { return hasMixedSentiment; }
    public boolean isHasTrust() { return hasTrust; }

    public String getCompanyName() { return companyName; }
    public Integer getMeetingDurationMinutes() { return meetingDurationMinutes; }
    public boolean isDurationEstimated() { return durationEstimated; }
    public BigDecimal getBudgetValueDetected() { return budgetValueDetected; }
    public double getChurnProbability() { return churnProbability; }
    public double getUpsellPotentialValue() { return upsellPotentialValue; }
    public int getComplaintTermCount() { return complaintTermCount; }
    public int getPositiveTermCount() { return positiveTermCount; }
    public int getNegativeTermCount() { return negativeTermCount; }
}
