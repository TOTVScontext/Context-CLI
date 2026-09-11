package br.com.totvs.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Motor de análise textual (NLP baseado em léxico) do CONTEXT CLI.
 * <p>
 * Todas as métricas são calculadas a partir de contagens reais de termos e de
 * padrões (timestamps, valores monetários, nomes de empresa) encontrados na
 * própria transcrição. Nenhum valor de saída é pré-definido: quando um sinal
 * não é encontrado no texto, o campo correspondente fica nulo/zero em vez de
 * receber um valor de exemplo.
 */
public class Analyzer {
    private final String nlpVersion;

    public Analyzer(String nlpVersion) {
        this.nlpVersion = nlpVersion;
    }

    // Termos usados nos léxicos de contagem. Cada ocorrência pesa no score
    // final, então a nota reflete a intensidade do sinal no texto e não
    // apenas sua presença binária.
    private static final String[] TERMOS_NEGATIVOS = {
            "odeio", "odio", "ruim", "pessimo", "horrivel", "frustrado", "frustrada",
            "insatisfeito", "insatisfeita", "problema", "lento", "trava", "travando",
            "cancelar", "cancelamento", "nao gostei", "nao vou comprar", "nao quero comprar",
            "reclamacao", "decepcionado", "decepcionada", "demora", "demorado", "falha", "erro"
    };

    private static final String[] TERMOS_POSITIVOS = {
            "gostei", "gostou", "excelente", "otimo", "otima", "satisfeito", "satisfeita",
            "recomendo", "confianca", "confio", "quero comprar", "interesse", "interessante",
            "gostaria de avancar", "ficamos satisfeitos", "superou", "eficiente", "rapido", "rapida"
    };

    private static final String[] TERMOS_RESOLUCAO_POSITIVA = {
            "resolvido", "solucionado", "funcionou", "conseguimos", "deu certo", "resolveu"
    };

    private static final String[] TERMOS_RESOLUCAO_NEGATIVA = {
            "nao resolveu", "sem solucao", "nao foi resolvido", "continua com problema",
            "ainda esta com problema", "nao funcionou"
    };

    private static final String[] TERMOS_INTERESSE_COMERCIAL = {
            "comprar", "proposta", "interesse", "gostei", "gostou", "recomendo", "fechar negocio",
            "avancar com a proposta", "orcamento", "contrato"
    };

    private static final String[] TERMOS_NEGACAO_COMPRA = {
            "nao vou comprar", "nao quero comprar", "nao comprarei", "cancelar", "desistir", "desistimos"
    };

    private static final String[] TERMOS_RECLAMACAO = {
            "odeio", "odio", "ruim", "pessimo", "frustrado", "frustrada", "problema", "lento",
            "trava", "cancelar", "cancelamento", "nao vou comprar", "nao quero comprar", "reclamacao"
    };

    private static final String[] TERMOS_BUDGET = {
            "mil", "r$", "investimento", "valor", "preco", "orcamento", "budget"
    };

    private static final String[] TERMOS_PERSONA = {
            "cfo", "ceo", "cto", "diretor", "diretora", "gestor", "gestora", "gerente", "roi"
    };

    private static final String[] TERMOS_CONFIANCA = {
            "confianca", "confio", "acredito", "certeza"
    };

    // ------------------------------------------------------------------
    // Extração de metadados reais da reunião
    // ------------------------------------------------------------------

    private static final Pattern PADRAO_TIMESTAMP =
            Pattern.compile("\\[?(\\d{1,2}):(\\d{2})(?::(\\d{2}))?\\]?");

    // Ex.: "R$ 12.345,67", "R$12000", "R$ 45 mil"
    private static final Pattern PADRAO_VALOR_REAIS =
            Pattern.compile("(?i)r\\$\\s*([\\d.,]+)\\s*(mil|milh[oõ]es)?");

    // Ex.: "orcamento de 80 mil", "investimento de 250 mil reais"
    private static final Pattern PADRAO_VALOR_MIL =
            Pattern.compile("(?i)(\\d+(?:[.,]\\d+)?)\\s*mil(?:\\s*reais)?");

    // Nome de empresa citado explicitamente na conversa, ex.:
    // "aqui da TOTVS", "represento a Acme Ltda", "cliente: Grupo Fictus"
    private static final Pattern PADRAO_EMPRESA = Pattern.compile(
            "(?i)(?:empresa|companhia|represento a|representando a|da empresa|cliente da|somos da|aqui da)\\s+" +
                    "([A-ZÀ-Ý][\\wÀ-ÿ&.\\-]*(?:\\s+[A-ZÀ-Ý][\\wÀ-ÿ&.\\-]*){0,3})"
    );

    private static final double PALAVRAS_POR_MINUTO_FALA = 130.0;

    public Analysis analyze(Conversation conversation) {
        String textoOriginal = conversation.getText() == null ? "" : conversation.getText();
        String texto = normalizar(textoOriginal);

        int negativos = contarOcorrencias(texto, TERMOS_NEGATIVOS);
        int positivos = contarOcorrencias(texto, TERMOS_POSITIVOS);

        double notaProdutividade = calcularProdutividade(texto);
        double notaSentimento = calcularSentimento(texto, positivos, negativos);
        double notaResolucao = calcularResolucao(texto);

        boolean temReclamacao = contemAlgum(texto, TERMOS_RECLAMACAO);
        boolean temBudget = contemAlgum(texto, TERMOS_BUDGET);
        boolean temPersona = contemAlgum(texto, TERMOS_PERSONA);
        boolean temSentimentoMisto =
                contemAlgum(texto, "satisfeito", "gosto", "gostei")
                        && contemAlgum(texto, "frustrado", "sofrendo", "problema");
        boolean temConfianca = contemAlgum(texto, TERMOS_CONFIANCA);

        String empresa = extrairEmpresa(textoOriginal);
        BigDecimal valorDetectado = extrairValorMencionado(textoOriginal);
        int[] duracao = extrairDuracaoMinutos(textoOriginal);
        Integer duracaoMinutos = duracao[0] >= 0 ? duracao[0] : null;
        boolean duracaoEstimada = duracao[1] == 1;

        return new Analysis(
                notaProdutividade,
                notaSentimento,
                notaResolucao,
                temReclamacao,
                temBudget,
                temPersona,
                temSentimentoMisto,
                temConfianca,
                empresa,
                duracaoMinutos,
                duracaoEstimada,
                valorDetectado,
                contarOcorrencias(texto, TERMOS_RECLAMACAO),
                positivos,
                negativos
        );
    }

    public String getNlpVersion() {
        return nlpVersion;
    }

    // ------------------------------------------------------------------
    // Métricas contínuas (baseadas em contagem, não em bucket fixo)
    // ------------------------------------------------------------------

    private double calcularSentimento(String texto, int positivos, int negativos) {
        double nota = 5.0 + (positivos * 0.9) - (negativos * 1.1);
        return arredondar(clamp(nota, 0.0, 10.0));
    }

    private double calcularProdutividade(String texto) {
        int interesse = contarOcorrencias(texto, TERMOS_INTERESSE_COMERCIAL);
        int recusa = contarOcorrencias(texto, TERMOS_NEGACAO_COMPRA);

        double nota = 5.0 + (interesse * 1.1) - (recusa * 2.0);
        return arredondar(clamp(nota, 0.0, 10.0));
    }

    private double calcularResolucao(String texto) {
        int positivos = contarOcorrencias(texto, TERMOS_RESOLUCAO_POSITIVA);
        int negativos = contarOcorrencias(texto, TERMOS_RESOLUCAO_NEGATIVA);

        if (positivos == 0 && negativos == 0) {
            return 5.0;
        }

        double nota = 5.0 + (positivos * 1.5) - (negativos * 1.8);
        return arredondar(clamp(nota, 0.0, 10.0));
    }

    // ------------------------------------------------------------------
    // Extração de empresa, valor de budget e duração
    // ------------------------------------------------------------------

    private String extrairEmpresa(String textoOriginal) {
        if (textoOriginal == null || textoOriginal.isBlank()) {
            return null;
        }
        Matcher m = PADRAO_EMPRESA.matcher(textoOriginal);
        if (m.find()) {
            String nome = m.group(1).trim().replaceAll("[.,;:]+$", "");
            if (!nome.isBlank()) {
                return nome;
            }
        }
        return null;
    }

    private BigDecimal extrairValorMencionado(String textoOriginal) {
        if (textoOriginal == null || textoOriginal.isBlank()) {
            return null;
        }

        BigDecimal maior = null;

        Matcher mReais = PADRAO_VALOR_REAIS.matcher(textoOriginal);
        while (mReais.find()) {
            BigDecimal valor = parseValorMonetario(mReais.group(1), mReais.group(2));
            if (valor != null && (maior == null || valor.compareTo(maior) > 0)) {
                maior = valor;
            }
        }

        Matcher mMil = PADRAO_VALOR_MIL.matcher(textoOriginal);
        while (mMil.find()) {
            try {
                BigDecimal base = new BigDecimal(mMil.group(1).replace(",", "."));
                BigDecimal valor = base.multiply(BigDecimal.valueOf(1000));
                if (maior == null || valor.compareTo(maior) > 0) {
                    maior = valor;
                }
            } catch (NumberFormatException ignored) {
                // termo não numérico próximo de "mil" (ex.: "mil desculpas") — ignora
            }
        }

        return maior == null ? null : maior.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal parseValorMonetario(String bruto, String multiplicador) {
        if (bruto == null) {
            return null;
        }
        String limpo = bruto.trim();
        // Formatos aceitos: 12.345,67 (BR) ou 12345.67 (US)
        if (limpo.matches("\\d{1,3}(\\.\\d{3})*,\\d{1,2}")) {
            limpo = limpo.replace(".", "").replace(",", ".");
        } else {
            limpo = limpo.replace(",", "");
        }
        try {
            BigDecimal valor = new BigDecimal(limpo);
            if (multiplicador != null) {
                String m = multiplicador.toLowerCase();
                if (m.startsWith("mil")) {
                    valor = valor.multiply(BigDecimal.valueOf(1000));
                } else if (m.startsWith("milh")) {
                    valor = valor.multiply(BigDecimal.valueOf(1_000_000));
                }
            }
            return valor;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Retorna {@code [duracaoMinutos, estimada]} onde {@code estimada} é
     * {@code 1} quando a duração não pôde ser lida diretamente de timestamps
     * da transcrição e foi estimada pelo tamanho do texto (velocidade média
     * de fala), ou {@code 0} quando veio de timestamps reais.
     * {@code duracaoMinutos == -1} indica que nenhuma duração pôde ser
     * calculada (transcrição vazia).
     */
    private int[] extrairDuracaoMinutos(String textoOriginal) {
        if (textoOriginal == null || textoOriginal.isBlank()) {
            return new int[]{-1, 0};
        }

        Matcher m = PADRAO_TIMESTAMP.matcher(textoOriginal);
        Integer primeiro = null;
        Integer ultimo = null;
        int totalMarcacoes = 0;
        while (m.find()) {
            int h = 0, min, s = 0;
            // Se o padrão capturou 3 grupos numéricos, o primeiro é hora.
            if (m.group(3) != null) {
                h = Integer.parseInt(m.group(1));
                min = Integer.parseInt(m.group(2));
                s = Integer.parseInt(m.group(3));
            } else {
                min = Integer.parseInt(m.group(1));
                s = Integer.parseInt(m.group(2));
            }
            int totalSegundos = h * 3600 + min * 60 + s;
            if (primeiro == null) {
                primeiro = totalSegundos;
            }
            ultimo = totalSegundos;
            totalMarcacoes++;
        }

        // Só confia nos timestamps como marcação real da reunião quando há
        // várias ocorrências no padrão esperado (uma por fala/linha) — evita
        // tratar uma hora citada isoladamente na conversa como timestamp.
        if (totalMarcacoes >= 3 && primeiro != null && ultimo != null && ultimo > primeiro) {
            int minutos = Math.max(1, Math.round((ultimo - primeiro) / 60.0f));
            return new int[]{minutos, 0};
        }

        // Sem timestamps utilizáveis: estima pela quantidade de palavras da
        // transcrição, assumindo uma cadência média de fala em português.
        int palavras = textoOriginal.trim().isEmpty() ? 0 : textoOriginal.trim().split("\\s+").length;
        if (palavras == 0) {
            return new int[]{-1, 0};
        }
        int minutosEstimados = Math.max(1, (int) Math.round(palavras / PALAVRAS_POR_MINUTO_FALA));
        return new int[]{minutosEstimados, 1};
    }

    // ------------------------------------------------------------------
    // Utilitários
    // ------------------------------------------------------------------

    private String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "")
                .toLowerCase()
                .trim();
    }

    private boolean contemAlgum(String texto, String... termos) {
        for (String termo : termos) {
            if (texto.contains(termo)) {
                return true;
            }
        }
        return false;
    }

    private int contarOcorrencias(String texto, String... termos) {
        int total = 0;
        for (String termo : termos) {
            int indice = texto.indexOf(termo);
            while (indice >= 0) {
                total++;
                indice = texto.indexOf(termo, indice + termo.length());
            }
        }
        return total;
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    private static double arredondar(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
