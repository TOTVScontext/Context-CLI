package br.com.totvs.main;

import br.com.totvs.controller.CommandController;
import br.com.totvs.controller.CommandController.ComandoEntrada;
import br.com.totvs.view.ConsoleView;

import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * Ponto de entrada do CONTEXT CLI.
 *
 * <p>Responsabilidade única: iniciar a aplicação e manter o loop de sessão.
 * Toda a interação com o usuário é delegada à {@link ConsoleView} e toda a
 * coordenação de fluxo (parsing de comandos, chamadas a Model/Service/DAO)
 * é delegada ao {@link CommandController} — fluxo:
 * Main → View → Controller → Model/Service → DAO → Banco.</p>
 */
public class Main {

    public static void main(String[] args) {
        ConsoleView view = new ConsoleView();
        CommandController controller = new CommandController(view);

        Scanner scan = new Scanner(System.in);
        view.exibirBanner();

        boolean continuarSessao = true;

        while (continuarSessao) {
            try {
                String linha = view.lerLinha(scan);
                ComandoEntrada entrada = controller.parseComando(linha);
                controller.despachar(scan, entrada);
            } catch (CommandController.EncerrarSessaoException e) {
                continuarSessao = false;
            } catch (NoSuchElementException | IllegalStateException e) {
                continuarSessao = false;
            } catch (Exception e) {
                view.exibirErroInesperado(e.getMessage());
            }
        }

        view.exibirEncerramento();
        scan.close();
    }
}
