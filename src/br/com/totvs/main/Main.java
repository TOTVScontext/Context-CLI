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


    public static void main(String[] args) throws IOException {
        final String RESET = "\u001B[0m";
        final String PRIMARY = "\u001B[38;5;111m";
        final String SECONDARY = "\u001B[38;5;110m";
        final String MUTED = "\u001B[38;5;245m";

        final String GREEN = "\u001B[38;5;114m";
        final String RED = "\u001B[38;5;210m";
        final String YELLOW = "\u001B[38;5;222m";

        Scanner scan = new Scanner(System.in);
        System.out.print(
                        "\n" +
                        "\u001B[38;2;80;160;255m████████╗\u001B[38;2;75;150;250m ██████╗\u001B[38;2;70;140;245m ████████╗\u001B[38;2;65;130;240m██╗   ██╗\u001B[38;2;60;120;235m███████╗\n" +
                        "\u001B[38;2;75;150;250m╚══██╔══╝\u001B[38;2;70;140;245m██╔═══██╗\u001B[38;2;65;130;240m╚══██╔══╝\u001B[38;2;60;120;235m██║   ██║\u001B[38;2;55;110;230m██╔════╝\n" +
                        "\u001B[38;2;70;140;245m   ██║\u001B[38;2;65;130;240m   ██║   ██║\u001B[38;2;60;120;235m   ██║\u001B[38;2;55;110;230m   ██║   ██║\u001B[38;2;50;100;225m███████╗\n" +
                        "\u001B[38;2;65;130;240m   ██║\u001B[38;2;60;120;235m   ██║   ██║\u001B[38;2;55;110;230m   ██║\u001B[38;2;50;100;225m   ╚██╗ ██╔╝\u001B[38;2;45;90;220m╚════██║\n" +
                        "\u001B[38;2;60;120;235m   ██║\u001B[38;2;55;110;230m   ╚██████╔╝\u001B[38;2;50;100;225m   ██║\u001B[38;2;45;90;220m    ╚████╔╝ \u001B[38;2;40;80;215m███████║\n" +
                        "\u001B[38;2;55;110;230m   ╚═╝\u001B[38;2;50;100;225m    ╚═════╝ \u001B[38;2;45;90;220m   ╚═╝\u001B[38;2;40;80;215m     ╚═══╝  \u001B[38;2;35;70;210m╚══════╝\n" +
                        "\u001B[38;2;50;100;225m ██████╗\u001B[38;2;45;90;220m ██████╗ ███╗   ██╗████████╗███████╗██╗  ██╗████████╗\n" +
                        "\u001B[38;2;45;90;220m██╔════╝\u001B[38;2;40;80;215m██╔═══██╗████╗  ██║╚══██╔══╝██╔════╝╚██╗██╔╝╚══██╔══╝\n" +
                        "\u001B[38;2;40;80;215m██║\u001B[38;2;35;70;210m     ██║   ██║██╔██╗ ██║   ██║   █████╗   ╚███╔╝    ██║\n" +
                        "\u001B[38;2;35;70;210m██║\u001B[38;2;30;60;205m     ██║   ██║██║╚██╗██║   ██║   ██╔══╝   ██╔██╗    ██║\n" +
                        "\u001B[38;2;30;60;205m╚██████╗\u001B[38;2;25;50;150m╚██████╔╝██║ ╚████║   ██║   ███████╗██╔╝ ██╗   ██║\n" +
                        "\u001B[38;2;25;50;150m ╚═════╝\u001B[38;2;20;40;195m ╚═════╝ ╚═╝  ╚═══╝   ╚═╝   ╚══════╝╚═╝  ╚═╝   ╚═╝\u001B[0m\n"
        );
        System.out.println("\n"+" ".repeat(9)+PRIMARY+"✦"+RESET+" Inteligência de Interações Corporativas "+PRIMARY+"✦"+RESET+"\n"+" ".repeat(9));

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