package br.com.totvs.integration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class PythonModelClient {

    public String predict(String texto) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(
                "python",
                "ai/predict.py",
                texto
        );

        builder.directory(new File(System.getProperty("user.dir")));
        builder.redirectErrorStream(true);

        Process processo = builder.start();
        String resposta = new String(
                processo.getInputStream().readAllBytes(),
                StandardCharsets.UTF_8
        ).trim();

        int codigoSaida = processo.waitFor();

        if (codigoSaida != 0) {
            throw new IOException(
                    "O modelo Python terminou com codigo "
                            + codigoSaida + ": " + resposta
            );
        }

        return resposta;
    }
}
