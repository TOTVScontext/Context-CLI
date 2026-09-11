package br.com.totvs.main;

import br.com.totvs.model.Conversation;
import br.com.totvs.repository.JsonMeetingRepository;
import br.com.totvs.voice.VoiceIdResolver;

import java.nio.file.Path;
import java.util.Optional;
import java.util.Scanner;

public class TesteVoiceId {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        JsonMeetingRepository repository = new JsonMeetingRepository(
                Path.of("data", "ANON_transcricao.json")
        );
        VoiceIdResolver resolver = new VoiceIdResolver(repository);

        try {
            String id = resolver.capturarId(scanner);
            Optional<Conversation> reuniao = repository.buscarPorId(id);

            System.out.println("ID validado no JSON: " + id);
            System.out.println("Transcricao encontrada: "
                    + reuniao.isPresent());

            if (reuniao.isPresent()) {
                String texto = reuniao.get().getText();
                System.out.println("Inicio da transcricao:");
                System.out.println(texto.substring(
                        0,
                        Math.min(300, texto.length())
                ));
            }
        } catch (Exception e) {
            System.out.println("Erro ao reconhecer ID: "
                    + e.getMessage());
        } finally {
            scanner.close();
        }
    }
}
