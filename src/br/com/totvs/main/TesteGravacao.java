package br.com.totvs.main;

import br.com.totvs.voice.AudioRecorder;

import java.nio.file.Path;
import java.util.Scanner;

public class TesteGravacao {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        AudioRecorder recorder = new AudioRecorder();
        Path arquivo = Path.of("audio", "gravacao-teste.wav");

        try {
            Path resultado = recorder.gravar(arquivo, scanner);
            System.out.println("Audio salvo em: " + resultado.toAbsolutePath());
        } catch (Exception e) {
            System.out.println("Erro ao gravar audio: " + e.getMessage());
        } finally {
            scanner.close();
        }
    }
}