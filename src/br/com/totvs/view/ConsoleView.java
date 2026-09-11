package br.com.totvs.view;

import br.com.totvs.dto.Reuniao;
import br.com.totvs.model.Analysis;
import br.com.totvs.model.Insight;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Camada View do CONTEXT CLI.
 *
 * <p>Responsável exclusivamente pela interação com o usuário via terminal:
 * exibição do banner, prompts, leitura de linhas digitadas, formatação de
 * cores/tabelas e apresentação dos resultados produzidos pelo Controller.
 * Não contém regra de negócio, acesso a dados ou orquestração de fluxo —
 * essas responsabilidades pertencem ao {@code CommandController} e às
 * camadas de Model/Service/DAO.</p>
 */
public class ConsoleView {

    public static final String RESET = "\u001B[0m";
    public static final String PRIMARY = "\u001B[38;2;228;177;80m";
    public static final String SECONDARY = "\u001B[38;2;230;208;165m";
    public static final String MUTED = "\u001B[2m\u001B[38;2;150;130;90m";
    public static final String BORDER = "\u001B[38;2;110;95;65m";
    public static final String GREEN = "\u001B[38;5;114m";
    public static final String RED = "\u001B[38;5;210m";
    public static final String YELLOW = "\u001B[38;5;222m";

    private static final Pattern PADRAO_LARGURA_CONSOLE =
            Pattern.compile("(?i)colunas:\\s*(\\d+)|columns:\\s*(\\d+)");

    private static final int LARGURA_MINIMA = 40;
    private static final int LARGURA_PADRAO = 100;
    private static final long TIMEOUT_COMANDO_LARGURA_MS = 500L;
    private static final long CACHE_LARGURA_TTL_MS = 500L;

    private static final String[] QUADROS_SPINNER = {"⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏"};

    private volatile int larguraEmCache = LARGURA_PADRAO;
    private volatile long instanteUltimaLeituraLargura = 0L;

    // ------------------------------------------------------------------
    // Largura do terminal / divisores
    // ------------------------------------------------------------------

    private int larguraTerminal() {
        long agora = System.currentTimeMillis();
        if (agora - instanteUltimaLeituraLargura < CACHE_LARGURA_TTL_MS) {
            return larguraEmCache;
        }

        Integer largura = larguraViaVariavelDeAmbiente();
        if (largura == null) {
            largura = larguraViaComandoDoSistemaOperacional();
        }

        larguraEmCache = largura != null ? largura : LARGURA_PADRAO;
        instanteUltimaLeituraLargura = agora;
        return larguraEmCache;
    }

    private Integer larguraViaVariavelDeAmbiente() {
        String colunas = System.getenv("COLUMNS");
        if (colunas == null || colunas.isBlank()) {
            return null;
        }
        try {
            int valor = Integer.parseInt(colunas.trim());
            return valor > 0 ? Math.max(LARGURA_MINIMA, valor) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer larguraViaComandoDoSistemaOperacional() {
        boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");
        ProcessBuilder pb = windows
                ? new ProcessBuilder("cmd.exe", "/c", "mode con")
                : new ProcessBuilder("sh", "-c", "tput cols 2>/dev/tty");
        pb.redirectErrorStream(true);

        Process processo = null;
        try {
            processo = pb.start();

            String saida;
            try (InputStream entrada = processo.getInputStream()) {
                saida = new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
            }

            boolean finalizouATempo = processo.waitFor(TIMEOUT_COMANDO_LARGURA_MS, TimeUnit.MILLISECONDS);
            if (!finalizouATempo) {
                return null;
            }

            return windows ? extrairLarguraDeSaidaModeCon(saida) : extrairLarguraDeSaidaTputCols(saida);
        } catch (IOException e) {
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } finally {
            if (processo != null) {
                processo.destroyForcibly();
            }
        }
    }

    private Integer extrairLarguraDeSaidaModeCon(String saida) {
        Matcher m = PADRAO_LARGURA_CONSOLE.matcher(saida);
        if (!m.find()) {
            return null;
        }
        String valor = m.group(1) != null ? m.group(1) : m.group(2);
        try {
            return Math.max(LARGURA_MINIMA, Integer.parseInt(valor));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer extrairLarguraDeSaidaTputCols(String saida) {
        try {
            return Math.max(LARGURA_MINIMA, Integer.parseInt(saida.trim()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String divisorLinha() {
        return "─".repeat(larguraTerminal());
    }

    private int larguraUtil() {
        return larguraTerminal();
    }

    // ------------------------------------------------------------------
    // Spinner de progresso
    // ------------------------------------------------------------------

    /**
     * Executa uma tarefa exibindo um spinner animado enquanto ela roda.
     * Usado pelo Controller para operações potencialmente lentas
     * (análise de conversa, consulta ao histórico etc.).
     */
    public <T> T executarComSpinner(String mensagem, Callable<T> tarefa) throws Exception {
        AtomicBoolean rodando = new AtomicBoolean(true);
        Thread spinner = new Thread(() -> {
            int i = 0;
            while (rodando.get()) {
                System.out.print("\r" + PRIMARY + QUADROS_SPINNER[i % QUADROS_SPINNER.length] + RESET
                        + " " + MUTED + mensagem + RESET);
                i++;
                try {
                    Thread.sleep(80);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });
        spinner.setDaemon(true);
        spinner.start();

        try {
            return tarefa.call();
        } finally {
            rodando.set(false);
            try {
                spinner.join(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.out.print("\r" + " ".repeat(mensagem.length() + 4) + "\r");
        }
    }

    // ------------------------------------------------------------------
    // Banner inicial
    // ------------------------------------------------------------------

    private String[] welcomeBox(String product, String accent, String cream, String border, String reset) {
        String prefix = "Bem-vindo ao ";
        int rawLen = (prefix + product).length();
        int pad = 2;
        int width = rawLen + pad * 2;

        String top = border + "╭" + "─".repeat(width) + "╮" + reset;
        String mid = border + "│" + reset
                + " ".repeat(pad)
                + cream + prefix + reset
                + accent + product + reset
                + " ".repeat(pad)
                + border + "│" + reset;
        String bottom = border + "╰" + "─".repeat(width) + "╯" + reset;

        return new String[]{top, mid, bottom};
    }

    public void exibirBanner() {
        System.out.println();
        for (String line : welcomeBox("CONTEXT CLI", PRIMARY, SECONDARY, BORDER, RESET)) {
            System.out.println(line);
        }

        System.out.print(
                "\n" +
                        "\u001B[38;2;255;200;90m████████╗\u001B[38;2;250;192;86m ██████╗\u001B[38;2;245;184;82m ████████╗\u001B[38;2;240;176;78m██╗   ██╗\u001B[38;2;235;168;74m███████╗\n" +
                        "\u001B[38;2;250;192;86m╚══██╔══╝\u001B[38;2;245;184;82m██╔═══██╗\u001B[38;2;240;176;78m╚══██╔══╝\u001B[38;2;235;168;74m██║   ██║\u001B[38;2;230;160;70m██╔════╝\n" +
                        "\u001B[38;2;245;184;82m   ██║\u001B[38;2;240;176;78m   ██║   ██║\u001B[38;2;235;168;74m   ██║\u001B[38;2;230;160;70m   ██║   ██║\u001B[38;2;225;152;66m███████╗\n" +
                        "\u001B[38;2;240;176;78m   ██║\u001B[38;2;235;168;74m   ██║   ██║\u001B[38;2;230;160;70m   ██║\u001B[38;2;225;152;66m   ╚██╗ ██╔╝\u001B[38;2;220;144;62m╚════██║\n" +
                        "\u001B[38;2;235;168;74m   ██║\u001B[38;2;230;160;70m   ╚██████╔╝\u001B[38;2;225;152;66m   ██║\u001B[38;2;220;144;62m    ╚████╔╝ \u001B[38;2;215;136;58m███████║\n" +
                        "\u001B[38;2;230;160;70m   ╚═╝\u001B[38;2;225;152;66m    ╚═════╝ \u001B[38;2;220;144;62m   ╚═╝\u001B[38;2;215;136;58m     ╚═══╝  \u001B[38;2;210;128;54m╚══════╝\n" +
                        "\u001B[38;2;225;152;66m ██████╗\u001B[38;2;220;144;62m ██████╗ ███╗   ██╗████████╗███████╗██╗  ██╗████████╗\n" +
                        "\u001B[38;2;220;144;62m██╔════╝\u001B[38;2;215;136;58m██╔═══██╗████╗  ██║╚══██╔══╝██╔════╝╚██╗██╔╝╚══██╔══╝\n" +
                        "\u001B[38;2;215;136;58m██║\u001B[38;2;210;128;54m     ██║   ██║██╔██╗ ██║   ██║   █████╗   ╚███╔╝    ██║\n" +
                        "\u001B[38;2;210;128;54m██║\u001B[38;2;205;120;50m     ██║   ██║██║╚██╗██║   ██║   ██╔══╝   ██╔██╗    ██║\n" +
                        "\u001B[38;2;205;120;50m╚██████╗\u001B[38;2;200;112;46m╚██████╔╝██║ ╚████║   ██║   ███████╗██╔╝ ██╗   ██║\n" +
                        "\u001B[38;2;200;112;46m ╚═════╝\u001B[38;2;195;104;42m ╚═════╝ ╚═╝  ╚═══╝   ╚═╝   ╚══════╝╚═╝  ╚═╝   ╚═╝\u001B[0m\n"
                        + "\n"
        );

        System.out.println(
                " ".repeat(9) + PRIMARY + "✦" + RESET
                        + " " + SECONDARY + "Inteligência de Interações Corporativas" + RESET
                        + " " + PRIMARY + "✦" + RESET
        );
        System.out.println();
        System.out.println(
                " ".repeat(9) + MUTED
                        + "Digite " + RESET + PRIMARY + "/help" + RESET + MUTED
                        + " para ver os comandos disponíveis"
                        + RESET
        );
    }

    // ------------------------------------------------------------------
    // Leitura de comandos
    // ------------------------------------------------------------------

    private void imprimirPrompt() {
        String divisor = divisorLinha();
        System.out.print("\n" + MUTED + divisor + RESET + "\n" + MUTED + " > " + RESET);
    }

    /**
     * Exibe o prompt e lê a próxima linha digitada pelo usuário, já
     * removendo espaços nas extremidades. A interpretação da linha em
     * comando/argumento é responsabilidade do Controller.
     */
    public String lerLinha(Scanner scan) {
        imprimirPrompt();
        return scan.nextLine().trim();
    }

    public boolean confirmar(Scanner scan, String pergunta) {
        System.out.print(pergunta);
        String resposta = scan.nextLine().trim().toLowerCase();
        return resposta.equals("s") || resposta.equals("sim");
    }

    public void exibirAjuda(int limiteHistorico) {
        String divisor = divisorLinha();
        System.out.println("\n" + MUTED + divisor + RESET);
        System.out.println(PRIMARY + "COMANDOS DISPONÍVEIS" + RESET);
        System.out.println(MUTED + divisor + RESET);
        imprimirLinhaAjuda("/analyze <caminho>", "Analisa uma transcrição.");
        imprimirLinhaAjuda("/history", "Lista as últimas " + limiteHistorico + " análises realizadas.");
        imprimirLinhaAjuda("/read <titulo>", "Exibe a transcrição da análise.");
        imprimirLinhaAjuda("/rename <titulo>", "Altera o título da análise.");
        imprimirLinhaAjuda("/delete <titulo>", "Remove a análise pelo título.");
        imprimirLinhaAjuda("/help", "Exibe esta lista de comandos.");
        imprimirLinhaAjuda("/exit", "Encerra o CONTEXT CLI.");
        System.out.println(MUTED + divisor + RESET);
        System.out.println(MUTED + "Exemplo: /analyze documents/transcricoes/meeting_001.json" + RESET);
        System.out.println(MUTED + "O <titulo> em /read, /rename e /delete é único, sem espaços (ex: reuniao_cliente_x)." + RESET);
    }

    private void imprimirLinhaAjuda(String comando, String descricao) {
        System.out.printf("  %s%-24s%s %s%n", SECONDARY, comando, RESET, MUTED + descricao + RESET);
    }

    public void exibirComandoDesconhecido(String comando) {
        System.out.println(RED + "Comando desconhecido: /" + comando + RESET);
        System.out.println(MUTED + "Digite /help para ver os comandos disponíveis." + RESET);
    }

    public void exibirEntradaInvalida() {
        System.out.println(RED + "Entrada inválida." + RESET
                + MUTED + " Todo comando deve começar com \"/\" — digite /help para a lista completa." + RESET);
    }

    // ------------------------------------------------------------------
    // Mensagens genéricas
    // ------------------------------------------------------------------

    public void informarErro(String mensagem) {
        System.out.println(RED + mensagem + RESET);
    }

    public void informarAviso(String mensagem) {
        System.out.println(YELLOW + mensagem + RESET);
    }

    public void informarSucesso(String mensagem) {
        System.out.println(GREEN + mensagem + RESET);
    }

    public void informarMuted(String mensagem) {
        System.out.println(MUTED + mensagem + RESET);
    }

    public void avisarTituloComEspaco(String tituloInformado) {
        System.out.println(RED + tituloInformado + RESET);
        System.out.println(YELLOW
                + "O título não pode conter espaços. Use um único token (ex: reuniao_cliente_x) e tente novamente."
                + RESET);
    }

    public void exibirErroInesperado(String mensagem) {
        System.out.println("\n" + RED + "Erro inesperado: " + mensagem + RESET);
        System.out.println(YELLOW + "O CONTEXT CLI continuará em execução." + RESET);
    }

    public void exibirEncerramento() {
        System.out.println("\n" + MUTED + "Encerrando o CONTEXT CLI. Até a próxima!" + RESET + "\n");
    }

    // ------------------------------------------------------------------
    // Formatação de status/interpretação (usadas na exibição da análise)
    // ------------------------------------------------------------------

    private String clean(String msg) {
        return msg
                .replaceAll("\\[ALERTA.*?\\]\\s*", "")
                .replaceAll("\\[OPORTUNIDADE.*?\\]\\s*", "")
                .trim();
    }

    private String formatarStatusPersistencia(String resultado) {
        if (resultado == null) {
            return RED + "Status desconhecido" + RESET;
        }
        if (resultado.equalsIgnoreCase("Conexao nao estabelecida")) {
            return YELLOW + "Não persistida" + RESET + MUTED
                    + " — falha ao conectar ao banco Oracle (driver ausente, rede/VPN indisponível ou credenciais inválidas). "
                    + "A análise foi concluída normalmente." + RESET;
        }
        if (resultado.toLowerCase().startsWith("persistencia indisponivel")
                || resultado.toLowerCase().startsWith("persistência indisponível")) {
            return YELLOW + "Não persistida" + RESET + MUTED + " — " + resultado + RESET;
        }
        if (resultado.toLowerCase().contains("erro de sql")
                || resultado.toLowerCase().contains("não é possível")) {
            return RED + "Falha ao persistir" + RESET + MUTED + " — " + resultado + RESET;
        }
        if (resultado.toLowerCase().contains("sucesso")) {
            return GREEN + "Registro salvo com sucesso" + RESET + MUTED + " — " + resultado + RESET;
        }
        return YELLOW + "Não persistida" + RESET + MUTED + " — " + resultado + RESET;
    }

    private String interpretarProdutividade(double nota) {
        if (nota >= 8.5) {
            return "Interesse comercial claro, sem sinais de recusa — reunião altamente produtiva.";
        }
        if (nota <= 3.0) {
            return "Recusa de compra ou frustração explícita — reunião pouco produtiva, requer atenção.";
        }
        return "Sem sinais fortes de interesse ou recusa — reunião neutra.";
    }

    private String interpretarSentimento(double nota) {
        if (nota >= 7.0) {
            return "Predomínio de termos positivos (satisfação, recomendação, confiança).";
        }
        if (nota <= 4.0) {
            return "Predomínio de termos negativos (insatisfação, frustração, reclamação).";
        }
        return "Tom neutro — sem predominância clara de sentimento positivo ou negativo.";
    }

    private String interpretarResolucao(double nota) {
        if (nota >= 7.0) {
            return "Indícios de problema resolvido durante a conversa.";
        }
        if (nota <= 4.0) {
            return "Indícios de problema em aberto ou sem solução até o momento.";
        }
        return "Sem evidência clara de resolução ou pendência.";
    }

    // ------------------------------------------------------------------
    // Exibição do resultado de /analyze
    // ------------------------------------------------------------------

    public void exibirTranscricaoCarregada(String nomeArquivo) {
        System.out.println("\n" + SECONDARY + "Transcrição carregada: " + RESET + nomeArquivo);
    }

    public void exibirAvisoInsights(String aviso) {
        if (aviso != null) {
            System.out.println(YELLOW + aviso + RESET);
        }
    }

    /**
     * Exibe o relatório completo de uma análise (metadados, métricas,
     * sinais, insights, risco/oportunidade e resumo).
     */
    public void exibirResultadoAnalise(String nomeArquivo, int tamanhoTexto, List<String> participantes,
                                        Analysis analise, List<Insight> alertas, String resultadoPersistencia) {

        String divisorResultado = divisorLinha();
        System.out.println("\n\n" + MUTED + divisorResultado + "\n" + PRIMARY + "✦" + RESET
                + " Análise finalizada!\n" + MUTED + divisorResultado);
        System.out.println(MUTED + "Persistência: " + RESET + formatarStatusPersistencia(resultadoPersistencia));

        java.util.function.Function<Double, String> bar = (value) -> {
            int total = 10;
            int filled = (int) Math.round(value);
            StringBuilder b = new StringBuilder();
            for (int i = 0; i < total; i++) {
                b.append(i < filled ? "█" : "░");
            }
            return b.toString();
        };

        DateTimeFormatter formatoData = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

        System.out.println(PRIMARY + "\n\nANALYSIS · " + nomeArquivo + RESET);
        System.out.println(MUTED + divisorResultado + RESET);

        System.out.println();
        System.out.println(SECONDARY + "METADADOS" + RESET);
        System.out.printf("  %-18s %s%n", "Analisado em", java.time.LocalDateTime.now().format(formatoData));
        System.out.printf("  %-18s %s%n", "Motor de análise", "context v0.2.0");
        System.out.printf("  %-18s %s%n", "Participantes", String.join(", ", participantes));
        System.out.printf("  %-18s %s%n", "Empresa/Cliente",
                analise.getCompanyName() != null ? analise.getCompanyName() : MUTED + "não identificada na transcrição" + RESET);
        if (analise.getMeetingDurationMinutes() != null) {
            String origemDuracao = analise.isDurationEstimated()
                    ? " " + MUTED + "(estimada pela extensão do texto)" + RESET
                    : " " + MUTED + "(calculada a partir dos horários da transcrição)" + RESET;
            System.out.printf("  %-18s %d min%s%n", "Duração da reunião", analise.getMeetingDurationMinutes(), origemDuracao);
        } else {
            System.out.printf("  %-18s %s%n", "Duração da reunião", MUTED + "não foi possível estimar" + RESET);
        }
        System.out.printf("  %-18s %d caracteres%n", "Tamanho da transcrição", tamanhoTexto);

        System.out.println();
        System.out.println(SECONDARY + "METRICS" + RESET);
        System.out.printf("  %-18s %5.1f/10   %s%n", "Produtividade", analise.getProductivity(), bar.apply(analise.getProductivity()));
        System.out.println("  " + MUTED + interpretarProdutividade(analise.getProductivity()) + RESET);
        System.out.printf("  %-18s %5.1f/10   %s   %s%n", "Sentimento", analise.getSentiment(),
                bar.apply(analise.getSentiment()),
                analise.getSentiment() >= 7 ? GREEN + "positivo" + RESET : RED + "negativo" + RESET);
        System.out.println("  " + MUTED + interpretarSentimento(analise.getSentiment()) + RESET);
        System.out.printf("  %-18s %5.1f/10   %s%n", "Resolucao", analise.getResolution(), bar.apply(analise.getResolution()));
        System.out.println("  " + MUTED + interpretarResolucao(analise.getResolution()) + RESET);

        System.out.println();
        System.out.println(SECONDARY + "SINAIS IDENTIFICADOS" + RESET);
        boolean algumSinal = false;
        if (analise.isHasComplaint()) {
            System.out.println("  " + RED + "▪ Reclamação" + RESET + MUTED + " — termos de insatisfação encontrados na conversa" + RESET);
            algumSinal = true;
        }
        if (analise.isHasBudget()) {
            System.out.println("  " + YELLOW + "▪ Orçamento/valor" + RESET + MUTED + " — cliente mencionou valores, preço ou investimento" + RESET);
            algumSinal = true;
        }
        if (analise.isHasPersona()) {
            System.out.println("  " + YELLOW + "▪ Decisor presente" + RESET + MUTED + " — CFO, diretor, gestor ou discussão de ROI identificada" + RESET);
            algumSinal = true;
        }
        if (analise.isHasMixedSentiment()) {
            System.out.println("  " + YELLOW + "▪ Sentimento misto" + RESET + MUTED + " — sinais positivos e negativos na mesma conversa" + RESET);
            algumSinal = true;
        }
        if (analise.isHasTrust()) {
            System.out.println("  " + GREEN + "▪ Confiança expressa" + RESET + MUTED + " — cliente demonstrou confiança no vendedor/produto" + RESET);
            algumSinal = true;
        }
        if (!algumSinal) {
            System.out.println("  " + MUTED + "Nenhum sinal adicional identificado nesta transcrição" + RESET);
        }

        System.out.println();
        System.out.println(SECONDARY + "INSIGHTS (" + alertas.size() + ")" + RESET);

        List<Insight> alertasOrdenados = new ArrayList<>(alertas);
        alertasOrdenados.sort((a, b2) -> Integer.compare(a.getPriority(), b2.getPriority()));

        int risco = 0, negocio = 0, info = 0;
        for (Insight alerta : alertasOrdenados) {
            String msg = alerta.getMessage();
            String tagPrioridade = "[P" + alerta.getPriority() + "] ";
            if (msg.startsWith("[ALERTA")) {
                risco++;
                System.out.println("  " + RED + "⚠ " + tagPrioridade + clean(msg) + RESET);
            } else if (msg.startsWith("[OPORTUNIDADE")) {
                negocio++;
                System.out.println("  " + GREEN + "↑ " + tagPrioridade + clean(msg) + RESET);
            } else {
                info++;
                System.out.println("  " + MUTED + "• " + tagPrioridade + msg + RESET);
            }
        }
        if (alertasOrdenados.isEmpty()) {
            System.out.println("  " + MUTED + "Nenhum insight adicional gerado para esta transcrição" + RESET);
        }

        System.out.println();
        System.out.println(SECONDARY + "RISCO E OPORTUNIDADE (dados reais da transcrição)" + RESET);
        System.out.printf("  %-24s %s%n", "Probabilidade de churn",
                String.format("%.1f%%", analise.getChurnProbability()));
        if (analise.getBudgetValueDetected() != null) {
            System.out.printf("  %-24s %s%n", "Valor mencionado",
                    java.text.NumberFormat.getCurrencyInstance(new java.util.Locale("pt", "BR"))
                            .format(analise.getBudgetValueDetected()));
        } else {
            System.out.printf("  %-24s %s%n", "Valor mencionado", MUTED + "nenhum valor citado na conversa" + RESET);
        }

        System.out.println();
        System.out.println(SECONDARY + "SUMMARY" + RESET);
        System.out.printf("  %-16s %s%d%s%n", "Riscos", RED, risco, RESET);
        System.out.printf("  %-16s %s%d%s%n", "Oportunidades", GREEN, negocio, RESET);
        System.out.printf("  %-16s %s%d%s%n", "Informacoes", YELLOW, info, RESET);

        System.out.println();
        System.out.println(MUTED + divisorResultado + RESET);
    }

    public void exibirRelatorioPdfGerado(String caminhoPdf) {
        System.out.println();
        System.out.println(GREEN + "Relatório PDF gerado com sucesso" + RESET);
        System.out.println(MUTED + caminhoPdf + RESET);
    }

    public void exibirErroRelatorioPdf(String mensagem) {
        System.out.println(RED + "Erro ao gerar o relatório PDF: " + mensagem + RESET);
    }

    public void exibirFalhaAoAbrirPdf(String mensagem) {
        System.out.println(YELLOW + "Não foi possível abrir automaticamente: " + mensagem + RESET);
    }

    // ------------------------------------------------------------------
    // Exibição de /history
    // ------------------------------------------------------------------

    public void exibirHistorico(List<Reuniao> reunioes, int limiteHistorico) {
        String divisor = divisorLinha();
        System.out.println("\n" + MUTED + divisor + RESET);
        System.out.println(PRIMARY + "HISTÓRICO · últimas " + limiteHistorico + " análises" + RESET);
        System.out.println(MUTED + divisor + RESET);

        if (reunioes.isEmpty()) {
            System.out.println(MUTED + "Nenhuma análise encontrada. Utilize /analyze <caminho> para começar." + RESET);
            return;
        }

        DateTimeFormatter formatoData = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        System.out.printf("  %-3s %-28s %-17s %-11s %-6s%n", "#", "TÍTULO", "DATA", "SENTIMENTO", "RISCO");
        System.out.println("  " + MUTED + "-".repeat(Math.min(larguraUtil() - 2, 78)) + RESET);

        int posicao = 1;
        for (Reuniao r : reunioes) {
            String titulo = r.getTitulo() != null ? r.getTitulo() : r.getIdReuniao();
            if (titulo.length() > 28) {
                titulo = titulo.substring(0, 25) + "...";
            }
            String data = r.getDataAnalise() != null
                    ? r.getDataAnalise().toLocalDateTime().format(formatoData)
                    : "-";
            String sentimento = r.getSentimento() != null ? r.getSentimento() : "-";
            String risco = Integer.valueOf(1).equals(r.getPredRisco()) ? RED + "sim" + RESET : GREEN + "não" + RESET;

            System.out.printf("  %-3d %-28s %-17s %-11s %s%n", posicao++, titulo, data, sentimento, risco);
        }

        System.out.println(MUTED + divisor + RESET);
        System.out.println(MUTED + "Use /rename <titulo> ou /delete <titulo> para gerenciar uma análise." + RESET);
    }

    // ------------------------------------------------------------------
    // Exibição de /read
    // ------------------------------------------------------------------

    public void exibirTranscricao(String titulo, String transcricao) {
        String[] linhas = transcricao.split("\\r?\\n", -1);

        String divisor = divisorLinha();
        System.out.println("\n" + MUTED + divisor + RESET);
        System.out.println(PRIMARY + "TRANSCRIÇÃO · " + titulo + RESET);
        System.out.println(MUTED + divisor + RESET);

        int larguraNumero = String.valueOf(linhas.length).length();
        int numero = 1;
        for (String linha : linhas) {
            System.out.printf("  %s%" + larguraNumero + "d%s  %s%n", MUTED, numero++, RESET, linha);
        }

        System.out.println(MUTED + divisor + RESET);
        System.out.println(MUTED + linhas.length + " linha(s) exibidas." + RESET);
    }

    // ------------------------------------------------------------------
    // Exibição de /rename e /delete
    // ------------------------------------------------------------------

    public void exibirTituloAtual(String tituloAtual) {
        System.out.println(MUTED + "Título atual: " + RESET + tituloAtual);
    }

    public String lerNovoTitulo(Scanner scan) {
        System.out.print(SECONDARY + "Novo título (sem espaços): " + RESET);
        return scan.nextLine().trim();
    }

    public void exibirTituloJaExistente(String entrada) {
        System.out.println(RED + entrada + RESET);
        System.out.println(YELLOW + "Já existe uma análise com esse título. Escolha outro." + RESET);
    }

    public void exibirResultadoOperacao(String resultado) {
        if (resultado.toLowerCase().contains("sucesso")) {
            System.out.println(GREEN + resultado + RESET);
        } else {
            System.out.println(YELLOW + resultado + RESET);
        }
    }
}
