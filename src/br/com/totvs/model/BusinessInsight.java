package br.com.totvs.model;

public class BusinessInsight extends Insight {
    private double potentialValue;

    public BusinessInsight(){
    }
    public BusinessInsight(String message, int priority, double potentialValue){
        super(message, priority);
        this.potentialValue = potentialValue;
    }

    public double getPotentialValue() {
        return potentialValue;
    }

    @Override
    public String getMessage() {
        String valorFormatado = java.text.NumberFormat
                .getCurrencyInstance(new java.util.Locale("pt", "BR"))
                .format(this.potentialValue);
        return "[OPORTUNIDADE DE NEGOCIO] " + super.getMessage() + " | Valor Potencial: " + valorFormatado;
    }
}
