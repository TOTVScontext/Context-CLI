package br.com.totvs.infrastructure;

import br.com.totvs.model.Analysis;
import br.com.totvs.model.Conversation;
import br.com.totvs.model.Insight;
import br.com.totvs.infrastructure.pdf.PdfBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class ReportGenerator {

    private static final float[] AZUL_TOTVS = {0f, 0.19f, 0.53f};
    private static final float[] AZUL_CLARO = {0f, 0.31f, 0.78f};
    private static final float[] CINZA_LINHA = {0.91f, 0.93f, 0.95f};
    private static final float[] BRANCO = {1f, 1f, 1f};
    private static final float[] PRETO = {0.10f, 0.10f, 0.10f};
    private static final float[] CINZA_TEXTO = {0.20f, 0.20f, 0.20f};
    private static final float[] VERMELHO = {0.75f, 0.22f, 0.17f};
    private static final float[] VERDE = {0.10f, 0.48f, 0.29f};
    private static final float[] AMARELO = {0.72f, 0.47f, 0.05f};
    private static final float[] FUNDO_RISCO = {1f, 0.94f, 0.94f};
    private static final float[] FUNDO_OPO = {0.94f, 1f, 0.96f};
    private static final float[] FUNDO_INFO = {1f, 0.98f, 0.94f};

    private ReportGenerator() {
    }

    public static Path resolveOutputDirectory() {
        String userHome = System.getProperty("user.home", ".");
        return Path.of(userHome, "Downloads", "ContextCLI", "PDF");
    }

    public static String buildFileName(String origemArquivo) {
        String base = sanitizarNomeArquivo(origemArquivo);
        return "analise_" + base + ".pdf";
    }

    private static String sanitizarNomeArquivo(String nome) {
        if (nome == null || nome.isBlank()) {
            return "transcricao";
        }
        String semExtensao = nome.contains(".")
                ? nome.substring(0, nome.lastIndexOf('.'))
                : nome;
        String limpo = semExtensao.trim().replaceAll("[^A-Za-z0-9_\\-]+", "_");
        return limpo.isBlank() ? "transcricao" : limpo;
    }

    public static String generate(Conversation conversa, Analysis analise,
                                  List<Insight> insights, String origemArquivo) throws IOException {

        Path diretorioSaida = resolveOutputDirectory();
        Files.createDirectories(diretorioSaida);

        Path destino = diretorioSaida.resolve(buildFileName(origemArquivo));

        byte[] pdfBytes = montarPdf(conversa, analise, insights);
        Files.write(destino, pdfBytes);

        return destino.toAbsolutePath().toString();
    }

    private static byte[] montarPdf(Conversation conversa, Analysis analise, List<Insight> insights) throws IOException {
        String idReuniao = conversa.getId();
        String transcricao = conversa.getText();
        String participantes = String.join(", ", conversa.getParticipants());
        String dataHora = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

        List<String> riscos = insights.stream().map(Insight::getMessage)
                .filter(m -> m.startsWith("[ALERTA")).collect(Collectors.toList());
        List<String> oportunidades = insights.stream().map(Insight::getMessage)
                .filter(m -> m.startsWith("[OPORTUNIDADE")).collect(Collectors.toList());
        List<String> informacoes = insights.stream().map(Insight::getMessage)
                .filter(m -> !m.startsWith("[ALERTA") && !m.startsWith("[OPORTUNIDADE"))
                .collect(Collectors.toList());

        String rodape = "Gerado em " + dataHora + "  |  Reuniao: " + idReuniao
                + "  |  TOTVS Context  |  Uso interno e confidencial";

        PdfBuilder pdf = new PdfBuilder(rodape);

        pdf.text("TOTVS CONTEXT", 26, true, AZUL_TOTVS[0], AZUL_TOTVS[1], AZUL_TOTVS[2]);
        pdf.text("Relatorio de Inteligencia de Interacoes Corporativas", 12, false,
                AZUL_CLARO[0], AZUL_CLARO[1], AZUL_CLARO[2]);
        pdf.divider(AZUL_CLARO[0], AZUL_CLARO[1], AZUL_CLARO[2]);
        pdf.spacing(6);

        pdf.text("Identificacao da Reuniao", 14, true, AZUL_TOTVS[0], AZUL_TOTVS[1], AZUL_TOTVS[2]);
        pdf.spacing(4);
        float[] wIdent = {160f, CONTENT_WIDTH() - 160f};
        String empresa = analise.getCompanyName() != null ? analise.getCompanyName() : "Nao identificada na transcricao";
        String duracao = analise.getMeetingDurationMinutes() != null
                ? analise.getMeetingDurationMinutes() + " minutos" + (analise.isDurationEstimated() ? " (estimada)" : " (calculada)")
                : "Nao foi possivel estimar";
        linhaDupla(pdf, "ID da Reuniao", idReuniao, wIdent, AZUL_TOTVS, BRANCO, BRANCO, PRETO);
        linhaDupla(pdf, "Data e Hora", dataHora, wIdent, CINZA_LINHA, PRETO, BRANCO, PRETO);
        linhaDupla(pdf, "Empresa/Cliente", empresa, wIdent, AZUL_TOTVS, BRANCO, BRANCO, PRETO);
        linhaDupla(pdf, "Duracao da Reuniao", duracao, wIdent, CINZA_LINHA, PRETO, BRANCO, PRETO);
        linhaDupla(pdf, "Participantes", participantes, wIdent, AZUL_TOTVS, BRANCO, BRANCO, PRETO);
        linhaDupla(pdf, "Total de Insights", insights.size() + " sinais identificados", wIdent, CINZA_LINHA, PRETO, BRANCO, PRETO);
        pdf.spacing(16);

        pdf.text("1. Transcricao da Reuniao", 14, true, AZUL_TOTVS[0], AZUL_TOTVS[1], AZUL_TOTVS[2]);
        pdf.divider(AZUL_CLARO[0], AZUL_CLARO[1], AZUL_CLARO[2]);
        pdf.paragraph(transcricao, 10, false, CINZA_TEXTO[0], CINZA_TEXTO[1], CINZA_TEXTO[2]);
        pdf.spacing(16);

        pdf.text("2. Metricas da Analise", 14, true, AZUL_TOTVS[0], AZUL_TOTVS[1], AZUL_TOTVS[2]);
        pdf.divider(AZUL_CLARO[0], AZUL_CLARO[1], AZUL_CLARO[2]);
        pdf.paragraph("Indicadores extraidos pelo motor cognitivo TOTVS Context NLP v1.0.", 9.5f, false, 0.4f, 0.4f, 0.4f);
        pdf.spacing(4);
        float[] wMetric = {CONTENT_WIDTH() * 0.5f, CONTENT_WIDTH() * 0.2f, CONTENT_WIDTH() * 0.3f};
        linhaTripla(pdf, "Indicador", "Nota / 10", "Status", wMetric, AZUL_TOTVS, BRANCO, true);
        linhaMetrica(pdf, "Produtividade", analise.getProductivity(), BRANCO, wMetric);
        linhaMetrica(pdf, "Sentimento", analise.getSentiment(), CINZA_LINHA, wMetric);
        linhaMetrica(pdf, "Resolucao", analise.getResolution(), BRANCO, wMetric);
        pdf.spacing(8);
        String valorMencionado = analise.getBudgetValueDetected() != null
                ? java.text.NumberFormat.getCurrencyInstance(new java.util.Locale("pt", "BR")).format(analise.getBudgetValueDetected())
                : "Nenhum valor citado na transcricao";
        linhaDupla(pdf, "Probabilidade de Churn", String.format(java.util.Locale.of("pt", "BR"), "%.1f%%", analise.getChurnProbability()),
                wIdent, CINZA_LINHA, PRETO, BRANCO, PRETO);
        linhaDupla(pdf, "Valor Mencionado na Conversa", valorMencionado, wIdent, AZUL_TOTVS, BRANCO, BRANCO, PRETO);
        pdf.spacing(16);

        pdf.text("3. Insights Identificados", 14, true, AZUL_TOTVS[0], AZUL_TOTVS[1], AZUL_TOTVS[2]);
        pdf.divider(AZUL_CLARO[0], AZUL_CLARO[1], AZUL_CLARO[2]);

        if (!riscos.isEmpty()) {
            pdf.text("3.1 Alertas de Risco", 11, true, AZUL_CLARO[0], AZUL_CLARO[1], AZUL_CLARO[2]);
            for (String r : riscos) {
                pdf.callout("!", r, VERMELHO, FUNDO_RISCO, PRETO);
            }
            pdf.spacing(8);
        }
        if (!oportunidades.isEmpty()) {
            pdf.text("3.2 Oportunidades de Negocio", 11, true, AZUL_CLARO[0], AZUL_CLARO[1], AZUL_CLARO[2]);
            for (String o : oportunidades) {
                pdf.callout("+", o, VERDE, FUNDO_OPO, PRETO);
            }
            pdf.spacing(8);
        }
        if (!informacoes.isEmpty()) {
            pdf.text("3.3 Informacoes Adicionais", 11, true, AZUL_CLARO[0], AZUL_CLARO[1], AZUL_CLARO[2]);
            for (String i : informacoes) {
                pdf.callout("i", i, AMARELO, FUNDO_INFO, PRETO);
            }
            pdf.spacing(8);
        }
        if (riscos.isEmpty() && oportunidades.isEmpty() && informacoes.isEmpty()) {
            pdf.paragraph("Nenhum insight relevante foi identificado nesta reuniao.", 10, false,
                    CINZA_TEXTO[0], CINZA_TEXTO[1], CINZA_TEXTO[2]);
        }
        pdf.spacing(8);

        pdf.text("4. Recomendacoes para a Equipe Comercial", 14, true, AZUL_TOTVS[0], AZUL_TOTVS[1], AZUL_TOTVS[2]);
        pdf.divider(AZUL_CLARO[0], AZUL_CLARO[1], AZUL_CLARO[2]);
        boolean nenhumaRecomendacao = true;
        if (!riscos.isEmpty()) {
            pdf.paragraph("- Acionar equipe de retencao imediatamente para tratar risco de churn identificado.",
                    10, false, CINZA_TEXTO[0], CINZA_TEXTO[1], CINZA_TEXTO[2]);
            nenhumaRecomendacao = false;
        }
        if (!oportunidades.isEmpty()) {
            pdf.paragraph("- Preparar proposta comercial detalhada com base no sinal de upsell detectado.",
                    10, false, CINZA_TEXTO[0], CINZA_TEXTO[1], CINZA_TEXTO[2]);
            nenhumaRecomendacao = false;
        }
        if (informacoes.stream().anyMatch(i -> i.contains("Budget"))) {
            pdf.paragraph("- Registrar o valor de budget mencionado no CRM antes da proxima interacao.",
                    10, false, CINZA_TEXTO[0], CINZA_TEXTO[1], CINZA_TEXTO[2]);
            nenhumaRecomendacao = false;
        }
        if (informacoes.stream().anyMatch(i -> i.contains("Decisor"))) {
            pdf.paragraph("- Direcionar comunicacao ao decisor identificado com foco em ROI.",
                    10, false, CINZA_TEXTO[0], CINZA_TEXTO[1], CINZA_TEXTO[2]);
            nenhumaRecomendacao = false;
        }
        if (informacoes.stream().anyMatch(i -> i.contains("suporte"))) {
            pdf.paragraph("- Abrir ticket proativo de suporte para tratar reclamacao tecnica relatada.",
                    10, false, CINZA_TEXTO[0], CINZA_TEXTO[1], CINZA_TEXTO[2]);
            nenhumaRecomendacao = false;
        }
        if (informacoes.stream().anyMatch(i -> i.contains("confianca"))) {
            pdf.paragraph("- Aproveitar o sinal de confianca para avancar no fechamento da proposta.",
                    10, false, CINZA_TEXTO[0], CINZA_TEXTO[1], CINZA_TEXTO[2]);
            nenhumaRecomendacao = false;
        }
        if (nenhumaRecomendacao) {
            pdf.paragraph("- Manter acompanhamento padrao da conta; nenhuma acao critica identificada.",
                    10, false, CINZA_TEXTO[0], CINZA_TEXTO[1], CINZA_TEXTO[2]);
        }
        pdf.spacing(16);

        pdf.text("5. Classificacao Final da Reuniao", 14, true, AZUL_TOTVS[0], AZUL_TOTVS[1], AZUL_TOTVS[2]);
        pdf.divider(AZUL_CLARO[0], AZUL_CLARO[1], AZUL_CLARO[2]);

        boolean saudeBoa = analise.getSentiment() >= 7;
        String saudeLabel = saudeBoa ? "POSITIVO" : "REQUER ATENCAO";
        float[] saudeFill = saudeBoa ? FUNDO_OPO : FUNDO_RISCO;
        float[] saudeCor = saudeBoa ? VERDE : VERMELHO;

        String vendaLabel = !oportunidades.isEmpty() ? "ALTO" : "MODERADO";
        float[] vendaCor = !oportunidades.isEmpty() ? VERDE : AMARELO;

        String urgLabel = !riscos.isEmpty() ? "IMEDIATA" : "NORMAL";
        float[] urgFill = !riscos.isEmpty() ? FUNDO_RISCO : FUNDO_OPO;
        float[] urgCor = !riscos.isEmpty() ? VERMELHO : VERDE;

        linhaClassificacao(pdf, "Saude Geral do Cliente", saudeLabel, wIdent, AZUL_TOTVS, BRANCO, saudeFill, saudeCor);
        linhaClassificacao(pdf, "Potencial de Venda", vendaLabel, wIdent, CINZA_LINHA, PRETO, CINZA_LINHA, vendaCor);
        linhaClassificacao(pdf, "Urgencia de Acao", urgLabel, wIdent, AZUL_TOTVS, BRANCO, urgFill, urgCor);

        return pdf.build();
    }

    private static float CONTENT_WIDTH() {
        return PdfBuilder.CONTENT_WIDTH;
    }

    private static void linhaDupla(PdfBuilder pdf, String c1, String c2, float[] widths,
                                   float[] bg1, float[] fg1, float[] bg2, float[] fg2) {
        pdf.tableRow(new String[]{c1, c2}, widths, 22f, 9.5f, true,
                new float[][]{bg1, bg2}, new float[][]{fg1, fg2});
    }

    private static void linhaClassificacao(PdfBuilder pdf, String c1, String c2, float[] widths,
                                           float[] bg1, float[] fg1, float[] bg2, float[] fg2) {
        pdf.tableRow(new String[]{c1, c2}, widths, 24f, 10f, true,
                new float[][]{bg1, bg2}, new float[][]{fg1, fg2});
    }

    private static void linhaTripla(PdfBuilder pdf, String c1, String c2, String c3, float[] widths,
                                    float[] bg, float[] fg, boolean bold) {
        pdf.tableRow(new String[]{c1, c2, c3}, widths, 22f, 9.5f, bold,
                new float[][]{bg, bg, bg}, new float[][]{fg, fg, fg});
    }

    private static void linhaMetrica(PdfBuilder pdf, String label, double valor, float[] fill, float[] widths) {
        boolean bom = valor >= 7.0;
        String nota = String.format("%.1f", valor);
        String status = bom ? "Adequado" : "Requer atencao";
        float[] cor = bom ? VERDE : VERMELHO;
        pdf.tableRow(new String[]{label, nota, status}, widths, 20f, 9.5f, false,
                new float[][]{fill, fill, fill}, new float[][]{PRETO, cor, cor});
    }
}
