package br.com.totvs.model;

public class Insight {
    protected String message;
    protected int priority;

    public Insight(){
    }
    public Insight(String message, int priority) {
        this.message = message;
        this.priority = priority;
    }

    public String getMessage() {
        return message;
    }

    public int getPriority() {
        return priority;
    }
}
