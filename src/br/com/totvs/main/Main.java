package br.com.totvs.main;

import br.com.totvs.domain.*;
import br.com.totvs.dto.Reuniao;
import br.com.totvs.infrastructure.ReportGenerator;
import br.com.totvs.service.ReuniaoPersistenceService;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    private static final long TAMANHO_MAXIMO_BYTES = 5_000_000L;
    private static final int LIMITE_HISTORICO = 30;

    private static final String RESET = "\u001B[0m";
    private static final String PRIMARY = "\u001B[38;2;228;177;80m";
    private static final String SECONDARY = "\u001B[38;2;230;208;165m";
    private static final String MUTED = "\u001B[2m\u001B[38;2;150;130;90m";
    private static final String BORDER = "\u001B[38;2;110;95;65m";
    private static final String GREEN = "\u001B[38;5;114m";
    private static final String RED = "\u001B[38;5;210m";
    private static final String YELLOW = "\u001B[38;5;222m";

    private static final Pattern PADRAO_PARTICIPANTE =
            Pattern.compile("(?m)^\\s*([A-Za-zÀ-ÿ][A-Za-zÀ-ÿ0-9 ._-]{1,40}?):\\s+\\S");

    private static final Pattern PADRAO_LARGURA_CONSOLE =
            Pattern.compile("(?i)colunas:\\s*(\\d+)|columns:\\s*(\\d+)");

    private static final int LARGURA_MINIMA = 40;
    private static final int LARGURA_PADRAO = 100;
    private static final long TIMEOUT_COMANDO_LARGURA_MS = 500L;
    private static final long CACHE_LARGURA_TTL_MS = 500L;

    private static final String PROMPT = "context-cli";

    private static volatile int larguraEmCache = LARGURA_PADRAO;
    private static volatile long instanteUltimaLeituraLargura = 0L;

    private static final ReuniaoPersistenceService PERSISTENCE_SERVICE = new ReuniaoPersistenceService();

    private record TranscricaoArquivo(String nomeArquivo, String nomeBase, String texto) {
    }

    private record ResultadoAnalise(Analysis analise, List<Insight> insights,
                                    String statusPersistencia, String avisoInsights) {
    }

    private record ComandoEntrada(String comando, String argumento) {
    }

    // ------------------------------------------------------------------
    // Utilitários de terminal (largura, spinner, banner)
    // ------------------------------------------------------------------

    private static int larguraTerminal() {
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

    private static Integer larguraViaVariavelDeAmbiente() {
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

    private static Integer larguraViaComandoDoSistemaOperacional() {
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

    private static Integer extrairLarguraDeSaidaModeCon(String saida) {
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

    private static Integer extrairLarguraDeSaidaTputCols(String saida) {
        try {
            return Math.max(LARGURA_MINIMA, Integer.parseInt(saida.trim()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String divisorLinha() {
        return "─".repeat(larguraTerminal());
    }

    private static final String[] QUADROS_SPINNER = {"⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏"};

    private static <T> T executarComSpinner(String mensagem, java.util.concurrent.Callable<T> tarefa) throws Exception {
        java.util.concurrent.atomic.AtomicBoolean rodando = new java.util.concurrent.atomic.AtomicBoolean(true);
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

    private static String clean(String msg) {
        return msg
                .replaceAll("\\[ALERTA.*?\\]\\s*", "")
                .replaceAll("\\[OPORTUNIDADE.*?\\]\\s*", "")
                .trim();
    }

    private static String[] welcomeBox(String product, String accent, String cream, String border, String reset) {
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

    private static void printBanner() {
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
    // Leitura e parsing de comandos
    // ------------------------------------------------------------------

    private static void imprimirPrompt() {
        String divisor = divisorLinha();
        System.out.print("\n" + MUTED + divisor + RESET + "\n" + MUTED + " > " + RESET);
    }

    private static ComandoEntrada lerComando(Scanner scan) {
        imprimirPrompt();
        String linha = scan.nextLine().trim();

        if (linha.isEmpty()) {
            return new ComandoEntrada("", "");
        }

        if (!linha.startsWith("/")) {
            return new ComandoEntrada("__invalido__", linha);
        }

        String semBarra = linha.substring(1);
        int espaco = semBarra.indexOf(' ');

        String comando = (espaco == -1 ? semBarra : semBarra.substring(0, espaco)).toLowerCase().trim();
        String argumento = espaco == -1 ? "" : semBarra.substring(espaco + 1).trim();

        return new ComandoEntrada(comando, argumento);
    }

    private static void imprimirAjuda() {
        String divisor = divisorLinha();
        System.out.println("\n" + MUTED + divisor + RESET);
        System.out.println(PRIMARY + "COMANDOS DISPONÍVEIS" + RESET);
        System.out.println(MUTED + divisor + RESET);
        imprimirLinhaAjuda("/analyze <caminho>", "Analisa uma transcrição.");
        imprimirLinhaAjuda("/history", "Lista as últimas " + LIMITE_HISTORICO + " análises realizadas.");
        imprimirLinhaAjuda("/read <titulo>", "Exibe a transcrição da análise.");
        imprimirLinhaAjuda("/rename <titulo>", "Altera o título da análise.");
        imprimirLinhaAjuda("/delete <titulo>", "Remove a análise pelo título.");
        imprimirLinhaAjuda("/help", "Exibe esta lista de comandos.");
        imprimirLinhaAjuda("/exit", "Encerra o CONTEXT CLI.");
        System.out.println(MUTED + divisor + RESET);
        System.out.println(MUTED + "Exemplo: /analyze documents/transcricoes/meeting_001.json" + RESET);
        System.out.println(MUTED + "O <titulo> em /read, /rename e /delete é único, sem espaços (ex: reuniao_cliente_x)." + RESET);
    }

    private static void imprimirLinhaAjuda(String comando, String descricao) {
        System.out.printf("  %s%-24s%s %s%n", SECONDARY, comando, RESET, MUTED + descricao + RESET);
    }

    private static void imprimirComandoDesconhecido(String comando) {
        System.out.println(RED + "Comando desconhecido: /" + comando + RESET);
        System.out.println(MUTED + "Digite /help para ver os comandos disponíveis." + RESET);
    }

    private static void imprimirEntradaInvalida() {
        System.out.println(RED + "Entrada inválida." + RESET
                + MUTED + " Todo comando deve começar com \"/\" — digite /help para a lista completa." + RESET);
    }

    // ------------------------------------------------------------------
    // Resolução e leitura de arquivos de transcrição
    // ------------------------------------------------------------------

    private static Path resolverCaminho(String argumento) {
        String semAspas = argumento.replaceAll("^[\"']|[\"']$", "");
        Path raiz = Path.of(System.getProperty("user.home"));
        Path bruto = Path.of(semAspas);
        return bruto.isAbsolute() ? bruto : raiz.resolve(bruto);
    }

    private static String idAPartirDoCaminho(Path caminho) {
        String nomeArquivo = caminho.getFileName().toString();
        return nomeArquivo.contains(".")
                ? nomeArquivo.substring(0, nomeArquivo.lastIndexOf('.'))
                : nomeArquivo;
    }

    /**
     * Valida e carrega o conteúdo de um arquivo de transcrição a partir do
     * caminho informado após um comando. Em caso de problema, imprime uma
     * mensagem de erro descritiva e retorna {@code null}.
     */
    private static TranscricaoArquivo carregarTranscricao(String argumento) {
        if (argumento == null || argumento.isBlank()) {
            System.out.println(RED + "Informe o caminho do arquivo. Exemplo: /analyze downloads/transcricoes/meet08.json" + RESET);
            return null;
        }

        Path caminho;
        try {
            caminho = resolverCaminho(argumento);
        } catch (InvalidPathException e) {
            System.out.println(RED + "Caminho inválido: " + e.getMessage() + RESET);
            return null;
        }

        if (!Files.exists(caminho)) {
            System.out.println(RED + "Arquivo não encontrado: " + caminho.toAbsolutePath() + RESET);
            return null;
        }

        if (Files.isDirectory(caminho)) {
            System.out.println(RED + "O caminho informado é uma pasta. Informe o arquivo de transcrição." + RESET);
            return null;
        }

        if (!Files.isReadable(caminho)) {
            System.out.println(RED + "Sem permissão de leitura para o arquivo informado." + RESET);
            return null;
        }

        long tamanho;
        try {
            tamanho = Files.size(caminho);
        } catch (IOException e) {
            System.out.println(RED + "Não foi possível verificar o arquivo: " + e.getMessage() + RESET);
            return null;
        }

        if (tamanho == 0) {
            System.out.println(RED + "O arquivo está vazio." + RESET);
            return null;
        }

        if (tamanho > TAMANHO_MAXIMO_BYTES) {
            System.out.println(RED + "Arquivo muito grande (limite de "
                    + (TAMANHO_MAXIMO_BYTES / 1_000_000) + "MB para transcrições)." + RESET);
            return null;
        }

        String conteudo;
        try {
            conteudo = Files.readString(caminho, StandardCharsets.UTF_8);
        } catch (MalformedInputException e) {
            try {
                conteudo = Files.readString(caminho, StandardCharsets.ISO_8859_1);
                System.out.println(YELLOW + "Aviso: o arquivo não estava em UTF-8; foi lido como ISO-8859-1." + RESET);
            } catch (IOException ex) {
                System.out.println(RED + "Não foi possível ler o conteúdo do arquivo: " + ex.getMessage() + RESET);
                return null;
            }
        } catch (IOException e) {
            System.out.println(RED + "Erro ao ler o arquivo: " + e.getMessage() + RESET);
            return null;
        }

        if (conteudo.isBlank()) {
            System.out.println(RED + "O arquivo não contém texto válido para análise." + RESET);
            return null;
        }

        String nomeArquivo = caminho.getFileName().toString();
        String nomeBase = idAPartirDoCaminho(caminho);

        return new TranscricaoArquivo(nomeArquivo, nomeBase, conteudo.trim());
    }

    private static List<String> extrairParticipantes(String texto) {
        LinkedHashSet<String> encontrados = new LinkedHashSet<>();
        Matcher matcher = PADRAO_PARTICIPANTE.matcher(texto);

        while (matcher.find() && encontrados.size() < 6) {
            encontrados.add(matcher.group(1).trim());
        }

        if (encontrados.isEmpty()) {
            return List.of("Participante 1", "Participante 2");
        }

        return new ArrayList<>(encontrados);
    }

    private static boolean confirmar(Scanner scan, String pergunta) {
        System.out.print(pergunta);
        String resposta = scan.nextLine().trim().toLowerCase();
        return resposta.equals("s") || resposta.equals("sim");
    }

    private static String formatarStatusPersistencia(String resultado) {
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

    private static String interpretarProdutividade(double nota) {
        if (nota >= 8.5) {
            return "Interesse comercial claro, sem sinais de recusa — reunião altamente produtiva.";
        }
        if (nota <= 3.0) {
            return "Recusa de compra ou frustração explícita — reunião pouco produtiva, requer atenção.";
        }
        return "Sem sinais fortes de interesse ou recusa — reunião neutra.";
    }

    private static String interpretarSentimento(double nota) {
        if (nota >= 7.0) {
            return "Predomínio de termos positivos (satisfação, recomendação, confiança).";
        }
        if (nota <= 4.0) {
            return "Predomínio de termos negativos (insatisfação, frustração, reclamação).";
        }
        return "Tom neutro — sem predominância clara de sentimento positivo ou negativo.";
    }

    private static String interpretarResolucao(double nota) {
        if (nota >= 7.0) {
            return "Indícios de problema resolvido durante a conversa.";
        }
        if (nota <= 4.0) {
            return "Indícios de problema em aberto ou sem solução até o momento.";
        }
        return "Sem evidência clara de resolução ou pendência.";
    }

    /**
     * Deriva a origem da entrada a partir da extensão do arquivo analisado,
     * respeitando o domínio aceito pela constraint {@code CK_CONTEXT_ORIGEM}
     * da tabela {@code CONTEXT_REUNIAO} ({@code TEXTO}, {@code VOZ},
     * {@code CSV} ou {@code JSON}).
     */
    private static String origemEntradaPorExtensao(String nomeArquivo) {
        if (nomeArquivo == null) {
            return "TEXTO";
        }
        String extensao = nomeArquivo.contains(".")
                ? nomeArquivo.substring(nomeArquivo.lastIndexOf('.') + 1).toUpperCase()
                : "";
        return switch (extensao) {
            case "JSON" -> "JSON";
            case "CSV" -> "CSV";
            default -> "TEXTO";
        };
    }

    private static ResultadoAnalise executarPipelineDeAnalise(Conversation conversa, String nomeArquivo) {
        Analyzer nlp = new Analyzer("context v0.2.0");
        Analysis analise = nlp.analyze(conversa);

        List<Insight> alertas;
        String avisoInsights = null;
        try {
            InsightService service = new InsightService(7.0);
            alertas = service.generate(analise);
        } catch (Exception e) {
            alertas = List.of();
            avisoInsights = "Não foi possível gerar os insights: " + e.getMessage();
        }

        String statusPersistencia;
        try {
            statusPersistencia = PERSISTENCE_SERVICE.salvar(
                    conversa, analise, origemEntradaPorExtensao(nomeArquivo), "context v0.2.0");
        } catch (Exception e) {
            statusPersistencia = "Persistência indisponível (" + e.getMessage() + ")";
        }

        return new ResultadoAnalise(analise, alertas, statusPersistencia, avisoInsights);
    }

    // ------------------------------------------------------------------
    // Comando: /analyze
    // ------------------------------------------------------------------

    private static void executarComandoAnalyze(Scanner scan, String argumento) {
        TranscricaoArquivo arquivo = carregarTranscricao(argumento);
        if (arquivo == null) {
            return;
        }

        List<String> participantes = extrairParticipantes(arquivo.texto());
        Conversation conversa = new Conversation(arquivo.nomeBase(), arquivo.texto(), participantes);

        System.out.println("\n" + SECONDARY + "Transcrição carregada: " + RESET + arquivo.nomeArquivo());

        ResultadoAnalise resultado;
        try {
            resultado = executarComSpinner("Analisando reunião...",
                    () -> executarPipelineDeAnalise(conversa, arquivo.nomeArquivo()));
        } catch (Exception e) {
            System.out.println(RED + "Falha ao executar o motor de análise: " + e.getMessage() + RESET);
            System.out.println(YELLOW + "A transcrição foi carregada, mas não pôde ser processada." + RESET);
            return;
        }

        Analysis analise = resultado.analise();
        List<Insight> alertas = resultado.insights();
        String resultadoPersistencia = resultado.statusPersistencia();

        if (resultado.avisoInsights() != null) {
            System.out.println(YELLOW + resultado.avisoInsights() + RESET);
        }

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

        System.out.println(PRIMARY + "\n\nANALYSIS · " + arquivo.nomeArquivo() + RESET);
        System.out.println(MUTED + divisorResultado + RESET);

        System.out.println();
        System.out.println(SECONDARY + "METADADOS" + RESET);
        System.out.printf("  %-18s %s%n", "Analisado em", LocalDateTime.now().format(formatoData));
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
        System.out.printf("  %-18s %d caracteres%n", "Tamanho da transcrição", arquivo.texto().length());

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

        if (confirmar(scan, "Deseja gerar o relatório em PDF? (s/n): ")) {
            System.out.println();
            try {
                String caminhoPdf = ReportGenerator.generate(conversa, analise, alertas, arquivo.nomeArquivo());
                System.out.println(GREEN + "Relatório PDF gerado com sucesso" + RESET);
                System.out.println(MUTED + caminhoPdf + RESET);

                if (confirmar(scan, "\nAbrir agora? (s/n): ")) {
                    try {
                        java.awt.Desktop.getDesktop().open(new java.io.File(caminhoPdf));
                    } catch (Exception e) {
                        System.out.println(YELLOW + "Não foi possível abrir automaticamente: " + e.getMessage() + RESET);
                    }
                }
            } catch (IOException | RuntimeException e) {
                System.out.println(RED + "Erro ao gerar o relatório PDF: " + e.getMessage() + RESET);
            }
        }
    }

    // ------------------------------------------------------------------
    // Comando: /history
    // ------------------------------------------------------------------

    private static void executarComandoHistory() {
        List<Reuniao> reunioes;
        try {
            reunioes = executarComSpinner("Consultando histórico...",
                    () -> PERSISTENCE_SERVICE.listarUltimos(LIMITE_HISTORICO));
        } catch (Exception e) {
            System.out.println(RED + "Não foi possível consultar o histórico: " + e.getMessage() + RESET);
            return;
        }

        String divisor = divisorLinha();
        System.out.println("\n" + MUTED + divisor + RESET);
        System.out.println(PRIMARY + "HISTÓRICO · últimas " + LIMITE_HISTORICO + " análises" + RESET);
        System.out.println(MUTED + divisor + RESET);

        if (reunioes.isEmpty()) {
            System.out.println(MUTED + "Nenhuma análise encontrada. Utilize /analyze <caminho> para começar." + RESET);
            return;
        }

        DateTimeFormatter formatoData = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        System.out.printf("  %-3s %-28s %-17s %-11s %-6s%n", "#", "TÍTULO", "DATA", "SENTIMENTO", "RISCO");
        System.out.println("  " + MUTED + "-".repeat(Math.min(larguraTerminal() - 2, 78)) + RESET);

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
    // Validação de título (usada por /read, /rename e /delete)
    // ------------------------------------------------------------------

    /**
     * Um título válido é não vazio e não contém espaços — a unicidade é
     * verificada à parte, diretamente no banco, via
     * {@link ReuniaoPersistenceService#existeTitulo(String, String)}.
     */
    private static boolean formatoDeTituloValido(String titulo) {
        return titulo != null && !titulo.isBlank() && !titulo.contains(" ");
    }

    /**
     * Imprime o título informado em vermelho, evidenciando o problema, e
     * a orientação para corrigi-lo. Usada sempre que o usuário informa um
     * título contendo espaço.
     */
    private static void avisarTituloComEspaco(String tituloInformado) {
        System.out.println(RED + tituloInformado + RESET);
        System.out.println(YELLOW
                + "O título não pode conter espaços. Use um único token (ex: reuniao_cliente_x) e tente novamente."
                + RESET);
    }

    // ------------------------------------------------------------------
    // Comando: /read
    // ------------------------------------------------------------------

    private static void executarComandoRead(String argumento) {
        if (argumento == null || argumento.isBlank()) {
            System.out.println(RED + "Informe o título da análise. Exemplo: /read reuniao_cliente_x" + RESET);
            return;
        }

        if (!formatoDeTituloValido(argumento)) {
            avisarTituloComEspaco(argumento);
            return;
        }

        String titulo = argumento.trim();
        Optional<Reuniao> encontrada = PERSISTENCE_SERVICE.buscarPorTitulo(titulo);
        if (encontrada.isEmpty()) {
            System.out.println(RED + "Nenhuma análise encontrada com o título \"" + titulo + "\"." + RESET);
            System.out.println(MUTED + "Use /history para consultar os títulos disponíveis." + RESET);
            return;
        }

        String transcricao = encontrada.get().getTranscricao();
        if (transcricao == null || transcricao.isBlank()) {
            System.out.println(YELLOW + "A análise \"" + titulo + "\" não possui transcrição registrada no banco." + RESET);
            return;
        }

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
    // Comando: /rename
    // ------------------------------------------------------------------

    private static void executarComandoRename(Scanner scan, String argumento) {
        if (argumento == null || argumento.isBlank()) {
            System.out.println(RED + "Informe o título da análise. Exemplo: /rename reuniao_cliente_x" + RESET);
            return;
        }

        if (!formatoDeTituloValido(argumento)) {
            avisarTituloComEspaco(argumento);
            return;
        }

        String tituloAtual = argumento.trim();
        Optional<Reuniao> existente = PERSISTENCE_SERVICE.buscarPorTitulo(tituloAtual);
        if (existente.isEmpty()) {
            System.out.println(RED + "Nenhuma análise encontrada com o título \"" + tituloAtual + "\"." + RESET);
            System.out.println(MUTED + "Use /history para consultar os títulos disponíveis." + RESET);
            return;
        }

        String idReuniao = existente.get().getIdReuniao();
        System.out.println(MUTED + "Título atual: " + RESET + tituloAtual);

        String novoTitulo = null;
        while (novoTitulo == null) {
            System.out.print(SECONDARY + "Novo título (sem espaços): " + RESET);
            String entrada = scan.nextLine().trim();

            if (entrada.isEmpty()) {
                System.out.println(YELLOW + "Operação cancelada: o título não pode ser vazio." + RESET);
                return;
            }
            if (entrada.contains(" ")) {
                avisarTituloComEspaco(entrada);
                continue;
            }
            if (entrada.equals(tituloAtual)) {
                System.out.println(YELLOW + "O novo título é igual ao atual. Informe outro título (ou Enter para cancelar)." + RESET);
                continue;
            }
            if (PERSISTENCE_SERVICE.existeTitulo(entrada, idReuniao)) {
                System.out.println(RED + entrada + RESET);
                System.out.println(YELLOW + "Já existe uma análise com esse título. Escolha outro." + RESET);
                continue;
            }

            novoTitulo = entrada;
        }

        String resultado = PERSISTENCE_SERVICE.renomear(idReuniao, novoTitulo);
        if (resultado.toLowerCase().contains("sucesso")) {
            System.out.println(GREEN + resultado + RESET);
        } else {
            System.out.println(YELLOW + resultado + RESET);
        }
    }

    // ------------------------------------------------------------------
    // Comando: /delete
    // ------------------------------------------------------------------

    private static void executarComandoDelete(Scanner scan, String argumento) {
        if (argumento == null || argumento.isBlank()) {
            System.out.println(RED + "Informe o título da análise. Exemplo: /delete reuniao_cliente_x" + RESET);
            return;
        }

        if (!formatoDeTituloValido(argumento)) {
            avisarTituloComEspaco(argumento);
            return;
        }

        String titulo = argumento.trim();
        Optional<Reuniao> existente = PERSISTENCE_SERVICE.buscarPorTitulo(titulo);
        if (existente.isEmpty()) {
            System.out.println(RED + "Nenhuma análise encontrada com o título \"" + titulo + "\"." + RESET);
            System.out.println(MUTED + "Use /history para consultar os títulos disponíveis." + RESET);
            return;
        }

        String idReuniao = existente.get().getIdReuniao();
        boolean confirmado = confirmar(scan,
                YELLOW + "Confirma a exclusão de \"" + titulo + "\"? Esta ação não pode ser desfeita. (s/n): " + RESET);

        if (!confirmado) {
            System.out.println(MUTED + "Exclusão cancelada." + RESET);
            return;
        }

        String resultado = PERSISTENCE_SERVICE.excluir(idReuniao);
        if (resultado.toLowerCase().contains("sucesso")) {
            System.out.println(GREEN + resultado + RESET);
        } else {
            System.out.println(YELLOW + resultado + RESET);
        }
    }

    // ------------------------------------------------------------------
    // Loop principal
    // ------------------------------------------------------------------

    private static final class EncerrarSessaoException extends RuntimeException {
    }

    private static void despachar(Scanner scan, ComandoEntrada entrada) {
        switch (entrada.comando()) {
            case "" -> {
                // linha em branco: nenhuma ação
            }
            case "__invalido__" -> imprimirEntradaInvalida();
            case "analyze", "analisar" -> executarComandoAnalyze(scan, entrada.argumento());
            case "history", "historico", "histórico" -> executarComandoHistory();
            case "read", "ler" -> executarComandoRead(entrada.argumento());
            case "rename", "renomear" -> executarComandoRename(scan, entrada.argumento());
            case "delete", "excluir", "deletar" -> executarComandoDelete(scan, entrada.argumento());
            case "help", "ajuda", "?" -> imprimirAjuda();
            case "exit", "sair", "quit" -> throw new EncerrarSessaoException();
            default -> imprimirComandoDesconhecido(entrada.comando());
        }
    }

    public static void main(String[] args) {
        Scanner scan = new Scanner(System.in);
        printBanner();

        boolean continuarSessao = true;

        while (continuarSessao) {
            try {
                ComandoEntrada entrada = lerComando(scan);
                despachar(scan, entrada);
            } catch (EncerrarSessaoException e) {
                continuarSessao = false;
            } catch (NoSuchElementException | IllegalStateException e) {
                continuarSessao = false;
            } catch (Exception e) {
                System.out.println("\n" + RED + "Erro inesperado: " + e.getMessage() + RESET);
                System.out.println(YELLOW + "O CONTEXT CLI continuará em execução." + RESET);
            }
        }

        System.out.println("\n" + MUTED + "Encerrando o CONTEXT CLI. Até a próxima!" + RESET + "\n");
        scan.close();
    }
}