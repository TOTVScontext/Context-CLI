package br.com.totvs.dao;

import br.com.totvs.dto.Reuniao;

import java.util.List;
import java.util.Optional;

/**
 * Contrato de acesso a dados (CRUD completo) para o agregado {@link Reuniao}.
 */
public interface ReuniaoDAO {

    /** Insere uma nova análise de reunião. */
    String inserir(Reuniao reuniao);

    /** Atualiza todos os campos de uma análise já existente. */
    String alterar(Reuniao reuniao);

    /** Atualiza somente o título (nome de exibição) de uma análise. */
    String atualizarTitulo(String idReuniao, String novoTitulo);

    /** Remove uma análise pelo identificador. */
    String excluir(String idReuniao);

    /** Busca uma análise específica pelo identificador. */
    Optional<Reuniao> buscarPorId(String idReuniao);

    /** Busca uma análise específica pelo título (chave usada por /read, /rename e /delete). */
    Optional<Reuniao> buscarPorTitulo(String titulo);

    /** Verifica a existência de uma análise pelo identificador. */
    boolean existe(String idReuniao);

    /**
     * Verifica se já existe uma análise com o título informado.
     *
     * @param titulo    título a verificar
     * @param idExcluir identificador a ignorar na checagem (usado em /rename,
     *                  para não acusar colisão do registro consigo mesmo); pode ser {@code null}
     */
    boolean existeTitulo(String titulo, String idExcluir);

    /** Lista todas as análises persistidas, da mais recente para a mais antiga. */
    List<Reuniao> listarTodos();

    /** Lista as N análises mais recentes, da mais recente para a mais antiga. */
    List<Reuniao> listarUltimos(int limite);

    /** Retorna a quantidade total de análises persistidas. */
    long contar();
}
