package br.com.totvs.dto;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Representa o registro persistido de uma análise de reunião
 * (tabela {@code CONTEXT_REUNIAO}).
 */
public class Reuniao {
    private String idReuniao;
    private String titulo;
    private String transcricao;
    private String participantes;
    private String origemEntrada;
    private Timestamp dataAnalise;
    private Integer predRisco;
    private Integer predOportunidade;
    private String sentimento;
    private String produtosDetectados;
    private String modeloAnalise;
    private String classeModelo;
    private BigDecimal probabilidadeModelo;
    private String fonteDecisao;

    public Reuniao() {
    }

    public String getIdReuniao() {
        return idReuniao;
    }

    public void setIdReuniao(String idReuniao) {
        this.idReuniao = idReuniao;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getTranscricao() {
        return transcricao;
    }

    public void setTranscricao(String transcricao) {
        this.transcricao = transcricao;
    }

    public String getParticipantes() {
        return participantes;
    }

    public void setParticipantes(String participantes) {
        this.participantes = participantes;
    }

    public String getOrigemEntrada() {
        return origemEntrada;
    }

    public void setOrigemEntrada(String origemEntrada) {
        this.origemEntrada = origemEntrada;
    }

    public Timestamp getDataAnalise() {
        return dataAnalise;
    }

    public void setDataAnalise(Timestamp dataAnalise) {
        this.dataAnalise = dataAnalise;
    }

    public Integer getPredRisco() {
        return predRisco;
    }

    public void setPredRisco(Integer predRisco) {
        this.predRisco = predRisco;
    }

    public Integer getPredOportunidade() {
        return predOportunidade;
    }

    public void setPredOportunidade(Integer predOportunidade) {
        this.predOportunidade = predOportunidade;
    }

    public String getSentimento() {
        return sentimento;
    }

    public void setSentimento(String sentimento) {
        this.sentimento = sentimento;
    }

    public String getProdutosDetectados() {
        return produtosDetectados;
    }

    public void setProdutosDetectados(String produtosDetectados) {
        this.produtosDetectados = produtosDetectados;
    }

    public String getModeloAnalise() {
        return modeloAnalise;
    }

    public void setModeloAnalise(String modeloAnalise) {
        this.modeloAnalise = modeloAnalise;
    }

    public String getClasseModelo() {
        return classeModelo;
    }

    public void setClasseModelo(String classeModelo) {
        this.classeModelo = classeModelo;
    }

    public BigDecimal getProbabilidadeModelo() {
        return probabilidadeModelo;
    }

    public void setProbabilidadeModelo(BigDecimal probabilidadeModelo) {
        this.probabilidadeModelo = probabilidadeModelo;
    }

    public String getFonteDecisao() {
        return fonteDecisao;
    }

    public void setFonteDecisao(String fonteDecisao) {
        this.fonteDecisao = fonteDecisao;
    }
}
