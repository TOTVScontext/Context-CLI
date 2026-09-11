package br.com.totvs.controller;

import br.com.totvs.dto.Reuniao;
import br.com.totvs.infrastructure.ReportGenerator;
import br.com.totvs.model.Analysis;
import br.com.totvs.model.Analyzer;
import br.com.totvs.model.Conversation;
import br.com.totvs.model.Insight;
import br.com.totvs.model.InsightService;
import br.com.totvs.service.ReuniaoPersistenceService;
import br.com.totvs.view.ConsoleView;

import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Camada Controller do CONTEXT CLI.
 *
 * <p>Recebe as ações originadas na {@link ConsoleView}, coordena o fluxo da
 * aplicação e chama o Model/Service/DAO conforme necessário
 * (Main → View → Controller → Model/Service → DAO → Banco). Não imprime
 * diretamente no terminal nem lê entrada bruta do usuário — toda
 * apresentação é delegada à View.</p>
 */
public class CommandController {

    private static final long TAMANHO_MAXIMO_BYTES = 5_000_000L;
    private static final int LIMITE_HISTORICO = 30;

    private static final Pattern PADRAO_PARTICIPANTE =
            Pattern.compile("(?m)^\\s*([A-Za-zÀ-ÿ][A-Za-zÀ-ÿ0-9 ._-]{1,40}?):\\s+\\S");

    private final ConsoleView view;
    private final ReuniaoPersistenceService persistenceService;

    /** Sinaliza ao loop principal (Main) que a sessão deve ser encerrada. */
    public static final class EncerrarSessaoException extends RuntimeException {
    }

    private record TranscricaoArquivo(String nomeArquivo, String nomeBase, String texto) {
    }

    private record ResultadoAnalise(Analysis analise, List<Insight> insights,
                                     String statusPersistencia, String avisoInsights) {
    }

    public record ComandoEntrada(String comando, String argumento) {
    }

    public CommandController(ConsoleView view) {
        this.view = view;
        this.persistenceService = new ReuniaoPersistenceService();
    }

    public int limiteHistorico() {
        return LIMITE_HISTORICO;
    }

    // ------------------------------------------------------------------
    // Parsing de comandos (entrada digitada pelo usuário via View)
    // ------------------------------------------------------------------

    public ComandoEntrada parseComando(String linha) {
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

    // ------------------------------------------------------------------
    // Roteamento de comandos
    // ------------------------------------------------------------------

    public void despachar(Scanner scan, ComandoEntrada entrada) {
        switch (entrada.comando()) {
            case "" -> {
                // linha em branco: nenhuma ação
            }
            case "__invalido__" -> view.exibirEntradaInvalida();
            case "analyze", "analisar" -> executarComandoAnalyze(scan, entrada.argumento());
            case "history", "historico", "histórico" -> executarComandoHistory();
            case "read", "ler" -> executarComandoRead(entrada.argumento());
            case "rename", "renomear" -> executarComandoRename(scan, entrada.argumento());
            case "delete", "excluir", "deletar" -> executarComandoDelete(scan, entrada.argumento());
            case "help", "ajuda", "?" -> view.exibirAjuda(LIMITE_HISTORICO);
            case "exit", "sair", "quit" -> throw new EncerrarSessaoException();
            default -> view.exibirComandoDesconhecido(entrada.comando());
        }
    }

    // ------------------------------------------------------------------
    // Resolução e leitura de arquivos de transcrição
    // ------------------------------------------------------------------

    private Path resolverCaminho(String argumento) {
        String semAspas = argumento.replaceAll("^[\"']|[\"']$", "");
        Path raiz = Path.of(System.getProperty("user.home"));
        Path bruto = Path.of(semAspas);
        return bruto.isAbsolute() ? bruto : raiz.resolve(bruto);
    }

    private String idAPartirDoCaminho(Path caminho) {
        String nomeArquivo = caminho.getFileName().toString();
        return nomeArquivo.contains(".")
                ? nomeArquivo.substring(0, nomeArquivo.lastIndexOf('.'))
                : nomeArquivo;
    }

    /**
     * Valida e carrega o conteúdo de um arquivo de transcrição a partir do
     * caminho informado após um comando. Em caso de problema, reporta o
     * erro via View e retorna {@code null}.
     */
    private TranscricaoArquivo carregarTranscricao(String argumento) {
        if (argumento == null || argumento.isBlank()) {
            view.informarErro("Informe o caminho do arquivo. Exemplo: /analyze downloads/transcricoes/meet08.json");
            return null;
        }

        Path caminho;
        try {
            caminho = resolverCaminho(argumento);
        } catch (InvalidPathException e) {
            view.informarErro("Caminho inválido: " + e.getMessage());
            return null;
        }

        if (!Files.exists(caminho)) {
            view.informarErro("Arquivo não encontrado: " + caminho.toAbsolutePath());
            return null;
        }

        if (Files.isDirectory(caminho)) {
            view.informarErro("O caminho informado é uma pasta. Informe o arquivo de transcrição.");
            return null;
        }

        if (!Files.isReadable(caminho)) {
            view.informarErro("Sem permissão de leitura para o arquivo informado.");
            return null;
        }

        long tamanho;
        try {
            tamanho = Files.size(caminho);
        } catch (IOException e) {
            view.informarErro("Não foi possível verificar o arquivo: " + e.getMessage());
            return null;
        }

        if (tamanho == 0) {
            view.informarErro("O arquivo está vazio.");
            return null;
        }

        if (tamanho > TAMANHO_MAXIMO_BYTES) {
            view.informarErro("Arquivo muito grande (limite de "
                    + (TAMANHO_MAXIMO_BYTES / 1_000_000) + "MB para transcrições).");
            return null;
        }

        String conteudo;
        try {
            conteudo = Files.readString(caminho, StandardCharsets.UTF_8);
        } catch (MalformedInputException e) {
            try {
                conteudo = Files.readString(caminho, StandardCharsets.ISO_8859_1);
                view.informarAviso("Aviso: o arquivo não estava em UTF-8; foi lido como ISO-8859-1.");
            } catch (IOException ex) {
                view.informarErro("Não foi possível ler o conteúdo do arquivo: " + ex.getMessage());
                return null;
            }
        } catch (IOException e) {
            view.informarErro("Erro ao ler o arquivo: " + e.getMessage());
            return null;
        }

        if (conteudo.isBlank()) {
            view.informarErro("O arquivo não contém texto válido para análise.");
            return null;
        }

        String nomeArquivo = caminho.getFileName().toString();
        String nomeBase = idAPartirDoCaminho(caminho);

        return new TranscricaoArquivo(nomeArquivo, nomeBase, conteudo.trim());
    }

    private List<String> extrairParticipantes(String texto) {
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

    /**
     * Deriva a origem da entrada a partir da extensão do arquivo analisado,
     * respeitando o domínio aceito pela constraint {@code CK_CONTEXT_ORIGEM}
     * da tabela {@code CONTEXT_REUNIAO} ({@code TEXTO}, {@code VOZ},
     * {@code CSV} ou {@code JSON}).
     */
    private String origemEntradaPorExtensao(String nomeArquivo) {
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

    private ResultadoAnalise executarPipelineDeAnalise(Conversation conversa, String nomeArquivo) {
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
            statusPersistencia = persistenceService.salvar(
                    conversa, analise, origemEntradaPorExtensao(nomeArquivo), "context v0.2.0");
        } catch (Exception e) {
            statusPersistencia = "Persistência indisponível (" + e.getMessage() + ")";
        }

        return new ResultadoAnalise(analise, alertas, statusPersistencia, avisoInsights);
    }

    // ------------------------------------------------------------------
    // Comando: /analyze
    // ------------------------------------------------------------------

    private void executarComandoAnalyze(Scanner scan, String argumento) {
        TranscricaoArquivo arquivo = carregarTranscricao(argumento);
        if (arquivo == null) {
            return;
        }

        List<String> participantes = extrairParticipantes(arquivo.texto());
        Conversation conversa = new Conversation(arquivo.nomeBase(), arquivo.texto(), participantes);

        view.exibirTranscricaoCarregada(arquivo.nomeArquivo());

        ResultadoAnalise resultado;
        try {
            resultado = view.executarComSpinner("Analisando reunião...",
                    () -> executarPipelineDeAnalise(conversa, arquivo.nomeArquivo()));
        } catch (Exception e) {
            view.informarErro("Falha ao executar o motor de análise: " + e.getMessage());
            view.informarAviso("A transcrição foi carregada, mas não pôde ser processada.");
            return;
        }

        Analysis analise = resultado.analise();
        List<Insight> alertas = resultado.insights();
        String resultadoPersistencia = resultado.statusPersistencia();

        view.exibirAvisoInsights(resultado.avisoInsights());
        view.exibirResultadoAnalise(arquivo.nomeArquivo(), arquivo.texto().length(),
                participantes, analise, alertas, resultadoPersistencia);

        if (view.confirmar(scan, "Deseja gerar o relatório em PDF? (s/n): ")) {
            try {
                String caminhoPdf = ReportGenerator.generate(conversa, analise, alertas, arquivo.nomeArquivo());
                view.exibirRelatorioPdfGerado(caminhoPdf);

                if (view.confirmar(scan, "\nAbrir agora? (s/n): ")) {
                    try {
                        java.awt.Desktop.getDesktop().open(new java.io.File(caminhoPdf));
                    } catch (Exception e) {
                        view.exibirFalhaAoAbrirPdf(e.getMessage());
                    }
                }
            } catch (IOException | RuntimeException e) {
                view.exibirErroRelatorioPdf(e.getMessage());
            }
        }
    }

    // ------------------------------------------------------------------
    // Comando: /history
    // ------------------------------------------------------------------

    private void executarComandoHistory() {
        List<Reuniao> reunioes;
        try {
            reunioes = view.executarComSpinner("Consultando histórico...",
                    () -> persistenceService.listarUltimos(LIMITE_HISTORICO));
        } catch (Exception e) {
            view.informarErro("Não foi possível consultar o histórico: " + e.getMessage());
            return;
        }

        view.exibirHistorico(reunioes, LIMITE_HISTORICO);
    }

    // ------------------------------------------------------------------
    // Validação de título (usada por /read, /rename e /delete)
    // ------------------------------------------------------------------

    /**
     * Um título válido é não vazio e não contém espaços — a unicidade é
     * verificada à parte, diretamente no banco, via
     * {@link ReuniaoPersistenceService#existeTitulo(String, String)}.
     */
    private boolean formatoDeTituloValido(String titulo) {
        return titulo != null && !titulo.isBlank() && !titulo.contains(" ");
    }

    // ------------------------------------------------------------------
    // Comando: /read
    // ------------------------------------------------------------------

    private void executarComandoRead(String argumento) {
        if (argumento == null || argumento.isBlank()) {
            view.informarErro("Informe o título da análise. Exemplo: /read reuniao_cliente_x");
            return;
        }

        if (!formatoDeTituloValido(argumento)) {
            view.avisarTituloComEspaco(argumento);
            return;
        }

        String titulo = argumento.trim();
        Optional<Reuniao> encontrada = persistenceService.buscarPorTitulo(titulo);
        if (encontrada.isEmpty()) {
            view.informarErro("Nenhuma análise encontrada com o título \"" + titulo + "\".");
            view.informarMuted("Use /history para consultar os títulos disponíveis.");
            return;
        }

        String transcricao = encontrada.get().getTranscricao();
        if (transcricao == null || transcricao.isBlank()) {
            view.informarAviso("A análise \"" + titulo + "\" não possui transcrição registrada no banco.");
            return;
        }

        view.exibirTranscricao(titulo, transcricao);
    }

    // ------------------------------------------------------------------
    // Comando: /rename
    // ------------------------------------------------------------------

    private void executarComandoRename(Scanner scan, String argumento) {
        if (argumento == null || argumento.isBlank()) {
            view.informarErro("Informe o título da análise. Exemplo: /rename reuniao_cliente_x");
            return;
        }

        if (!formatoDeTituloValido(argumento)) {
            view.avisarTituloComEspaco(argumento);
            return;
        }

        String tituloAtual = argumento.trim();
        Optional<Reuniao> existente = persistenceService.buscarPorTitulo(tituloAtual);
        if (existente.isEmpty()) {
            view.informarErro("Nenhuma análise encontrada com o título \"" + tituloAtual + "\".");
            view.informarMuted("Use /history para consultar os títulos disponíveis.");
            return;
        }

        String idReuniao = existente.get().getIdReuniao();
        view.exibirTituloAtual(tituloAtual);

        String novoTitulo = null;
        while (novoTitulo == null) {
            String entrada = view.lerNovoTitulo(scan);

            if (entrada.isEmpty()) {
                view.informarAviso("Operação cancelada: o título não pode ser vazio.");
                return;
            }
            if (entrada.contains(" ")) {
                view.avisarTituloComEspaco(entrada);
                continue;
            }
            if (entrada.equals(tituloAtual)) {
                view.informarAviso("O novo título é igual ao atual. Informe outro título (ou Enter para cancelar).");
                continue;
            }
            if (persistenceService.existeTitulo(entrada, idReuniao)) {
                view.exibirTituloJaExistente(entrada);
                continue;
            }

            novoTitulo = entrada;
        }

        String resultado = persistenceService.renomear(idReuniao, novoTitulo);
        view.exibirResultadoOperacao(resultado);
    }

    // ------------------------------------------------------------------
    // Comando: /delete
    // ------------------------------------------------------------------

    private void executarComandoDelete(Scanner scan, String argumento) {
        if (argumento == null || argumento.isBlank()) {
            view.informarErro("Informe o título da análise. Exemplo: /delete reuniao_cliente_x");
            return;
        }

        if (!formatoDeTituloValido(argumento)) {
            view.avisarTituloComEspaco(argumento);
            return;
        }

        String titulo = argumento.trim();
        Optional<Reuniao> existente = persistenceService.buscarPorTitulo(titulo);
        if (existente.isEmpty()) {
            view.informarErro("Nenhuma análise encontrada com o título \"" + titulo + "\".");
            view.informarMuted("Use /history para consultar os títulos disponíveis.");
            return;
        }

        String idReuniao = existente.get().getIdReuniao();
        boolean confirmado = view.confirmar(scan,
                ConsoleView.YELLOW + "Confirma a exclusão de \"" + titulo + "\"? Esta ação não pode ser desfeita. (s/n): " + ConsoleView.RESET);

        if (!confirmado) {
            view.informarMuted("Exclusão cancelada.");
            return;
        }

        String resultado = persistenceService.excluir(idReuniao);
        view.exibirResultadoOperacao(resultado);
    }
}
