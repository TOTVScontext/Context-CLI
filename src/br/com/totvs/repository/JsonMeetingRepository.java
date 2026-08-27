package br.com.totvs.repository;

import br.com.totvs.domain.Conversation;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;

public class JsonMeetingRepository {
    private final Path caminhoJson;

    public JsonMeetingRepository(Path caminhoJson) {
        this.caminhoJson = caminhoJson;
    }

    public Optional<Conversation> buscarPorId(String idMeeting)
            throws IOException {
        if (idMeeting == null || idMeeting.isBlank()) {
            return Optional.empty();
        }

        String idProcurado = idMeeting.trim();

        try (BufferedReader reader = Files.newBufferedReader(
                caminhoJson,
                StandardCharsets.UTF_8
        )) {
            String linha;

            while ((linha = reader.readLine()) != null) {
                String idEncontrado = extrairCampoJson(
                        linha,
                        "ID_MEETING"
                );

                if (idProcurado.equals(idEncontrado)) {
                    String transcricao = extrairCampoJson(
                            linha,
                            "ANON_TRANSCRICAO"
                    );

                    if (transcricao == null || transcricao.isBlank()) {
                        return Optional.empty();
                    }

                    return Optional.of(new Conversation(
                            idEncontrado,
                            transcricao,
                            Arrays.asList("Vendedor", "Cliente")
                    ));
                }
            }
        }

        return Optional.empty();
    }

    private String extrairCampoJson(String json, String campo) {
        String marcador = "\"" + campo + "\"";
        int inicioCampo = json.indexOf(marcador);

        if (inicioCampo < 0) {
            return null;
        }

        int doisPontos = json.indexOf(':',
                inicioCampo + marcador.length());

        if (doisPontos < 0) {
            return null;
        }

        int inicioValor = doisPontos + 1;
        while (inicioValor < json.length()
                && Character.isWhitespace(json.charAt(inicioValor))) {
            inicioValor++;
        }

        if (inicioValor >= json.length()
                || json.charAt(inicioValor) != '"') {
            return null;
        }

        StringBuilder valor = new StringBuilder();
        boolean escapando = false;

        for (int i = inicioValor + 1; i < json.length(); i++) {
            char caractere = json.charAt(i);

            if (escapando) {
                switch (caractere) {
                    case 'n' -> valor.append('\n');
                    case 'r' -> valor.append('\r');
                    case 't' -> valor.append('\t');
                    case 'b' -> valor.append('\b');
                    case 'f' -> valor.append('\f');
                    case '"' -> valor.append('"');
                    case '\\' -> valor.append('\\');
                    default -> valor.append(caractere);
                }
                escapando = false;
            } else if (caractere == '\\') {
                escapando = true;
            } else if (caractere == '"') {
                return valor.toString();
            } else {
                valor.append(caractere);
            }
        }

        return null;
    }
}
