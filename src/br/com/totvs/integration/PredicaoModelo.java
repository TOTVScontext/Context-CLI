package br.com.totvs.integration;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PredicaoModelo {
    private String classe;
    private boolean risco;
    private double probabilidadeRisco;

    public PredicaoModelo(String classe,
                          boolean risco,
                          double probabilidadeRisco) {
        this.classe = classe;
        this.risco = risco;
        this.probabilidadeRisco = probabilidadeRisco;
    }

    public String getClasse() {
        return classe;
    }

    public boolean isRisco() {
        return risco;
    }

    public double getProbabilidadeRisco() {
        return probabilidadeRisco;
    }

    public static PredicaoModelo fromJson(String json) {
        String classe = extrairValor(json, "classe");
        String riscoTexto = extrairValor(json, "risco");
        String probabilidadeTexto = extrairValor(json, "probabilidade_risco");

        if (classe == null || riscoTexto == null || probabilidadeTexto == null) {
            throw new IllegalArgumentException(
                    "JSON do modelo nao possui os campos esperados"
            );
        }

        return new PredicaoModelo(
                classe,
                Boolean.parseBoolean(riscoTexto),
                Double.parseDouble(probabilidadeTexto)
        );
    }

    private static String extrairValor(String json, String campo) {
        String expressao = "\\\"" + Pattern.quote(campo)
                + "\\\"\\s*:\\s*(\\\"[^\\\"]*\\\"|true|false|-?\\d+(?:\\.\\d+)?)";

        Matcher matcher = Pattern.compile(expressao).matcher(json);

        if (!matcher.find()) {
            return null;
        }

        String valor = matcher.group(1);

        if (valor.startsWith("\"") && valor.endsWith("\"")) {
            return valor.substring(1, valor.length() - 1);
        }

        return valor;
    }
}
