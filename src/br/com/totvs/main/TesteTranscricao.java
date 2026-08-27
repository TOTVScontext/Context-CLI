package br.com.totvs.main;

import br.com.totvs.voice.SpeechToText;

import java.nio.file.Path;

public class TesteTranscricao {
    public static void main(String[] args) {
        SpeechToText speechToText = new SpeechToText();
        Path audio = Path.of("audio", "gravacao-teste.wav");

        try {
            String texto = speechToText.transcrever(audio);
            System.out.println("Transcricao recebida:");
            System.out.println(texto);
        } catch (Exception e) {
            System.out.println(
                    "Erro ao transcrever audio: " + e.getMessage()
            );
        }
    }
}
