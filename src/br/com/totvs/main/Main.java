package br.com.totvs.main;

import br.com.totvs.domain.*;
import br.com.totvs.infrastructure.ReportGenerator;
import br.com.totvs.service.ReuniaoPersistenceService;


import java.io.IOException;
import java.nio.file.Path;

import br.com.totvs.repository.JsonMeetingRepository;
import br.com.totvs.voice.VoiceIdResolver;

import java.nio.file.Path;
import java.util.Optional;


import java.util.Scanner;
import java.util.List;


public class Main {
    private static String clean(String msg) {
        return msg
                .replaceAll("\\[ALERTA.*?\\]\\s*", "")
                .replaceAll("\\[OPORTUNIDADE.*?\\]\\s*", "")
                .trim();
    }

    private static String lerTranscricao(Scanner scan) {
        System.out.print(
                "\n" + "─".repeat(150)
                        + "\nTranscrição\n"
                        + "─".repeat(150) + "\n> "
        );
        return scan.nextLine();
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


    public static void main(String[] args) throws IOException {
        final String RESET = "\u001B[0m";
        final String PRIMARY = "\u001B[38;2;228;177;80m";
        final String SECONDARY = "\u001B[38;2;230;208;165m";
        final String MUTED = "\u001B[2m\u001B[38;2;150;130;90m";
        final String BORDER = "\u001B[38;2;110;95;65m";

        final String GREEN = "\u001B[38;5;114m";
        final String RED = "\u001B[38;5;210m";
        final String YELLOW = "\u001B[38;5;222m";

        Scanner scan = new Scanner(System.in);

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
                        + "Pronto — informe abaixo os dados da reunião para começar"
                        + RESET
        );

        JsonMeetingRepository repository = new JsonMeetingRepository(
                Path.of("data", "ANON_transcricao.json")
        );

        System.out.println(
                "\n" + MUTED + "─".repeat(150) + RESET
                        + "\nComo deseja informar a reunião?\n"
                        + "1 - Digitar ID\n"
                        + "2 - Falar ID\n"
                        + MUTED + "─".repeat(150) + RESET
        );
        System.out.print(PRIMARY + "Escolha uma opção: " + RESET);
        String opcaoId = scan.nextLine().trim();

        String idConversa;
        String origemEntrada;

        if ("2".equals(opcaoId)
                || "falar".equalsIgnoreCase(opcaoId)) {
            try {
                VoiceIdResolver resolver =
                        new VoiceIdResolver(repository);
                idConversa = resolver.capturarId(scan);
                origemEntrada = "VOZ";
            } catch (Exception e) {
                System.out.println(
                        YELLOW + "Não foi possível reconhecer o ID: "
                                + e.getMessage() + RESET
                );
                System.out.println(
                        "Digite um ID existente para continuar."
                );
                System.out.print("ID da Reunião: ");
                idConversa = scan.nextLine().trim();
                origemEntrada = "JSON";
            }
        } else {
            System.out.print(
                    "\n" + MUTED + "─".repeat(150) + RESET
                            + "\nID da Reunião\n"
                            + MUTED + "─".repeat(150) + PRIMARY
                            + "\n> " + RESET
            );
            idConversa = scan.nextLine().trim();
            origemEntrada = "JSON";
        }

        Optional<Conversation> resultado =
                repository.buscarPorId(idConversa);

        if (resultado.isEmpty()) {
            System.out.println(
                    RED + "Nenhuma reunião encontrada para o ID: "
                            + idConversa + RESET
            );
            scan.close();
            return;
        }

        Conversation conversa = resultado.get();
        String textoConversa = conversa.getText();

        System.out.println(
                "\n" + SECONDARY + "Transcrição encontrada:" + RESET
        );
        System.out.println(textoConversa.substring(
                0,
                Math.min(500, textoConversa.length())
        ));
        System.out.println(
                MUTED + "[Transcrição completa carregada do JSON]"
                        + RESET
        );
        System.out.println(
                "Pressione ENTER para continuar com a análise..."
        );
        scan.nextLine();



        System.out.println("\n\n"+MUTED+"─".repeat(150)+"\n"+PRIMARY+"✦"+RESET+" Análise finalizada!\n"+MUTED+"─".repeat(150));

        Analyzer nlp = new Analyzer("context v0.1.8");
        Analysis analise = nlp.analyze(conversa);

        InsightService service = new InsightService(7.0);
        List<Insight> alertas = service.generate(analise);

        ReuniaoPersistenceService persistenceService = new ReuniaoPersistenceService();
        String resultadoPersistencia = persistenceService.salvar(
                conversa,
                analise,
                origemEntrada,
                "context v0.1.8"
        );
        System.out.println("Persistencia: " + resultadoPersistencia);

        java.util.function.Function<Double, String> bar = (value) -> {
            int total = 10;
            int filled = (int) Math.round(value);
            StringBuilder b = new StringBuilder();
            for (int i = 0; i < total; i++) {
                b.append(i < filled ? "█" : "░");
            }
            return b.toString();
        };

        // HEADER
        System.out.println(PRIMARY + "\n\nANALYSIS · CONVERSA " + idConversa + RESET);
        System.out.println(MUTED + "─".repeat(150) + RESET);

        // METRICS
        System.out.println();
        System.out.println(SECONDARY + "METRICS" + RESET);

        System.out.printf("  %-18s %5.1f/10   %s%n",
                "Produtividade",
                analise.getProductivity(),
                bar.apply(analise.getProductivity()));

        System.out.printf("  %-18s %5.1f/10   %s   %s%n",
                "Sentimento",
                analise.getSentiment(),
                bar.apply(analise.getSentiment()),
                analise.getSentiment() >= 7 ? GREEN + "positivo" + RESET : RED + "negativo" + RESET);

        System.out.printf("  %-18s %5.1f/10   %s%n",
                "Resolucao",
                analise.getResolution(),
                bar.apply(analise.getResolution()));

        // INSIGHTS
        System.out.println();
        System.out.println(SECONDARY + "INSIGHTS (" + alertas.size() + ")" + RESET);

        int risco = 0, negocio = 0, info = 0;

        for (Insight alerta : alertas) {
            String msg = alerta.getMessage();

            if (msg.startsWith("[ALERTA")) {
                risco++;
                System.out.println("  " + RED + "⚠ " + clean(msg) + RESET);
            } else if (msg.startsWith("[OPORTUNIDADE")) {
                negocio++;
                System.out.println("  " + GREEN + "↑ " + clean(msg) + RESET);
            } else {
                info++;
                System.out.println("  " + MUTED + "• " + msg + RESET);
            }
        }

        // SUMMARY
        System.out.println();
        System.out.println(SECONDARY + "SUMMARY" + RESET);

        System.out.printf("  %-16s %s%d%s%n", "Riscos", RED, risco, RESET);
        System.out.printf("  %-16s %s%d%s%n", "Oportunidades", GREEN, negocio, RESET);
        System.out.printf("  %-16s %s%d%s%n", "Informacoes", YELLOW, info, RESET);

        // FOOTER
        System.out.println();
        System.out.println(MUTED + "─".repeat(150) + RESET);

        // INPUT
        System.out.print("Gerar relatorio detalhado? (s/n): ");
        String resposta = scan.nextLine().trim().toLowerCase();

        if (resposta.equals("s")) {
            System.out.println();

            String caminho = ReportGenerator.generate(conversa, analise, alertas, scan);

            if (caminho != null) {
                System.out.println(GREEN + "Relatorio gerado com sucesso" + RESET);
                System.out.println(MUTED + caminho + RESET);

                System.out.print("\nAbrir agora? (s/n): ");
                if (scan.nextLine().trim().equalsIgnoreCase("s")) {
                    try {
                        java.awt.Desktop.getDesktop().open(new java.io.File(caminho));
                    } catch (Exception e) {
                        System.out.println(YELLOW + "Nao foi possivel abrir automaticamente" + RESET);
                    }
                }
            } else {
                System.out.println(RED + "Erro ao gerar relatorio" + RESET);
            }
        }

        System.out.println();
        scan.close();
    }
}