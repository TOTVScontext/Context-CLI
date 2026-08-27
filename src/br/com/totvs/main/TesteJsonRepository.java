package br.com.totvs.main;

import br.com.totvs.domain.Conversation;
import br.com.totvs.repository.JsonMeetingRepository;

import java.nio.file.Path;
import java.util.Optional;

public class TesteJsonRepository {
    public static void main(String[] args) {
        String id = args.length > 0 ? args[0] : "1000493";
        Path caminhoJson = Path.of("data", "ANON_transcricao.json");
        JsonMeetingRepository repository =
                new JsonMeetingRepository(caminhoJson);

        try {
            Optional<Conversation> resultado =
                    repository.buscarPorId(id);

            if (resultado.isEmpty()) {
                System.out.println(
                        "Reuniao nao encontrada para o ID: " + id
                );
                return;
            }

            Conversation conversation = resultado.get();
            String texto = conversation.getText();

            System.out.println("ID encontrado: "
                    + conversation.getId());
            System.out.println("Tamanho da transcricao: "
                    + texto.length());
            System.out.println("Inicio da transcricao:");
            System.out.println(texto.substring(
                    0,
                    Math.min(500, texto.length())
            ));
        } catch (Exception e) {
            System.out.println(
                    "Erro ao ler reuniao do JSON: " + e.getMessage()
            );
        }
    }
}
