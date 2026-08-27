package br.com.totvs.voice;

import br.com.totvs.repository.JsonMeetingRepository;

import java.nio.file.Path;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;

public class VoiceIdResolver {
    private final AudioRecorder audioRecorder;
    private final SpeechToText speechToText;
    private final JsonMeetingRepository repository;
    private final Map<String, String> numeros;

    public VoiceIdResolver(JsonMeetingRepository repository) {
        this.audioRecorder = new AudioRecorder();
        this.speechToText = new SpeechToText();
        this.repository = repository;
        this.numeros = criarMapaNumeros();
    }

    public String capturarId(Scanner scanner) throws Exception {
        Path caminhoAudio = Path.of(
                "audio",
                "id-voz-" + System.currentTimeMillis() + ".wav"
        );

        audioRecorder.gravar(caminhoAudio, scanner);

        String textoReconhecido = speechToText.transcrever(caminhoAudio);
        System.out.println("Texto reconhecido para ID:");
        System.out.println(textoReconhecido);

        String id = normalizarId(textoReconhecido);

        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException(
                    "Nao foi possivel identificar os digitos do ID"
            );
        }

        Optional<?> reuniao = repository.buscarPorId(id);

        if (reuniao.isEmpty()) {
            throw new IllegalArgumentException(
                    "O ID reconhecido nao existe no JSON: " + id
            );
        }

        return id;
    }

    private String normalizarId(String texto) {
        String normalizado = Normalizer.normalize(
                        texto == null ? "" : texto,
                        Normalizer.Form.NFD
                )
                .replaceAll("[^\\p{ASCII}]", "")
                .toLowerCase()
                .trim();

        String somenteDigitos = normalizado.replaceAll("\\D", "");

        if (!somenteDigitos.isBlank()) {
            return somenteDigitos;
        }

        StringBuilder id = new StringBuilder();
        String[] palavras = normalizado.split("\\s+");

        for (String palavra : palavras) {
            if (palavra.equals("e")) {
                continue;
            }

            String digito = numeros.get(palavra);

            if (digito == null) {
                return null;
            }

            id.append(digito);
        }

        return id.toString();
    }

    private Map<String, String> criarMapaNumeros() {
        Map<String, String> mapa = new HashMap<>();
        mapa.put("zero", "0");
        mapa.put("um", "1");
        mapa.put("dois", "2");
        mapa.put("tres", "3");
        mapa.put("quatro", "4");
        mapa.put("cinco", "5");
        mapa.put("seis", "6");
        mapa.put("sete", "7");
        mapa.put("oito", "8");
        mapa.put("nove", "9");
        return mapa;
    }
}
