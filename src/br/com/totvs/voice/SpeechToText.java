package br.com.totvs.voice;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public class SpeechToText {
    private final String pythonExecutable;

    public SpeechToText() {
        this.pythonExecutable = "python";
    }

    public String transcrever(Path caminhoAudio)
            throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(
                pythonExecutable,
                "ai/transcribe_local.py",
                caminhoAudio.toString(),
                "--model",
                "base"
        );

        builder.directory(new File(System.getProperty("user.dir")));
        builder.redirectErrorStream(true);

        Process processo = builder.start();
        String saida = new String(
                processo.getInputStream().readAllBytes(),
                StandardCharsets.UTF_8
        );

        int codigoSaida = processo.waitFor();

        if (codigoSaida != 0) {
            throw new IOException(
                    "Whisper local terminou com codigo "
                            + codigoSaida + ": " + saida
            );
        }

        return extrairTranscricao(saida);
    }

    private String extrairTranscricao(String saida)
            throws IOException {
        String marcador = "Transcricao local:";
        int inicio = saida.indexOf(marcador);

        if (inicio < 0) {
            throw new IOException(
                    "A saida do Whisper nao possui transcricao: " + saida
            );
        }

        String transcricao = saida.substring(
                inicio + marcador.length()
        ).trim();

        if (transcricao.isBlank()) {
            throw new IOException("Whisper retornou transcricao vazia");
        }

        return transcricao;
    }
}
