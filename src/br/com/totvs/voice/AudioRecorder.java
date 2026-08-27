package br.com.totvs.voice;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

public class AudioRecorder {
    private final AudioFormat formato;

    public AudioRecorder() {
        this.formato = new AudioFormat(
                16000.0f,
                16,
                1,
                true,
                false
        );
    }

    public Path gravar(Path caminhoArquivo,
                       Scanner scanner)
            throws Exception {
        Files.createDirectories(caminhoArquivo.getParent());

        DataLine.Info informacao = new DataLine.Info(
                TargetDataLine.class,
                formato
        );

        if (!AudioSystem.isLineSupported(informacao)) {
            throw new IllegalStateException(
                    "Microfone nao suportado neste computador"
            );
        }

        TargetDataLine linha =
                (TargetDataLine) AudioSystem.getLine(informacao);
        linha.open(formato);
        linha.start();

        Thread gravador = new Thread(() -> {
            try (AudioInputStream audioStream =
                         new AudioInputStream(linha)) {
                AudioSystem.write(
                        audioStream,
                        AudioFileFormat.Type.WAVE,
                        caminhoArquivo.toFile()
                );
            } catch (IOException e) {
                throw new RuntimeException(
                        "Erro ao salvar o audio: " + e.getMessage(),
                        e
                );
            }
        });

        gravador.start();

        System.out.println("[GRAVANDO]");
        System.out.println("Fale agora. Pressione ENTER para finalizar.");
        scanner.nextLine();

        linha.stop();
        linha.close();
        gravador.join();

        return caminhoArquivo;
    }
}
