package br.com.totvs.service;

import br.com.totvs.dao.ConnectionFactory;
import br.com.totvs.dao.ReuniaoDAO;
import br.com.totvs.dao.ReuniaoDAOImpl;
import br.com.totvs.domain.Analysis;
import br.com.totvs.domain.Conversation;
import br.com.totvs.dto.Reuniao;
import br.com.totvs.integration.HybridRiskService;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Camada de serviço responsável por orquestrar a persistência das análises
 * de reunião no Oracle, isolando o restante da aplicação dos detalhes de
 * ciclo de vida de conexão JDBC e do DAO.
 */
public class ReuniaoPersistenceService {

    private final HybridRiskService hybridRiskService;

    public ReuniaoPersistenceService() {
        this.hybridRiskService = new HybridRiskService();
    }

    /**
     * Executa uma operação de acesso a dados abrindo e fechando a conexão
     * de forma automática. Erros de conexão são convertidos em uma
     * mensagem amigável ao invés de propagar exceções técnicas para a UI.
     */
    private String executarComConexao(Function<ReuniaoDAO, String> operacao) {
        Connection con = null;
        try {
            con = ConnectionFactory.abrirConexao();
            return operacao.apply(new ReuniaoDAOImpl(con));
        } catch (IllegalStateException e) {
            return "Persistencia indisponivel: " + e.getMessage();
        } catch (SQLException e) {
            return "Conexao nao estabelecida";
        } catch (RuntimeException e) {
            return "Persistencia indisponivel (erro inesperado): " + e.getMessage();
        } finally {
            ConnectionFactory.fecharConexao(con);
        }
    }

    public String salvar(Conversation conversation,
                         Analysis analysis,
                         String origemEntrada,
                         String modeloAnalise) {
        boolean risco;
        String fonteDecisao;
        String classeModelo;
        BigDecimal probabilidadeModelo;

        try {
            HybridRiskService.DecisaoRisco decisao =
                    hybridRiskService.avaliar(conversation, analysis);

            risco = decisao.isRisco();
            fonteDecisao = decisao.getFonte();
            classeModelo = decisao.getClasseModelo();
            probabilidadeModelo = BigDecimal.valueOf(decisao.getProbabilidadeModelo());
        } catch (IOException e) {
            risco = analysis.getSentiment() < 7.0;
            fonteDecisao = "FALLBACK_REGRAS_JAVA_ERRO_MODELO";
            classeModelo = "INDISPONIVEL";
            probabilidadeModelo = null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            risco = analysis.getSentiment() < 7.0;
            fonteDecisao = "FALLBACK_REGRAS_JAVA_INTERRUPCAO";
            classeModelo = "INDISPONIVEL";
            probabilidadeModelo = null;
        }

        boolean riscoFinal = risco;
        String fonteFinal = fonteDecisao;
        String classeFinal = classeModelo;
        BigDecimal probFinal = probabilidadeModelo;

        return executarComConexao(dao -> {
            Reuniao reuniao = new Reuniao();
            reuniao.setIdReuniao(conversation.getId());
            reuniao.setTitulo(tituloUnicoESemEspaco(dao, conversation.getId()));
            reuniao.setTranscricao(conversation.getText());
            reuniao.setParticipantes(String.join(", ", conversation.getParticipants()));
            reuniao.setOrigemEntrada(origemEntrada);
            reuniao.setDataAnalise(new Timestamp(System.currentTimeMillis()));
            reuniao.setPredRisco(riscoFinal ? 1 : 0);
            reuniao.setPredOportunidade(analysis.getProductivity() >= 8.0 ? 1 : 0);
            reuniao.setSentimento(analysis.isGood() ? "Positivo" : "Negativo");
            reuniao.setProdutosDetectados(
                    conversation.getText().toLowerCase().contains("rm") ? "RM" : null);
            reuniao.setModeloAnalise(modeloAnalise);
            reuniao.setClasseModelo(classeFinal);
            reuniao.setProbabilidadeModelo(probFinal);
            reuniao.setFonteDecisao(fonteFinal);

            return dao.inserir(reuniao);
        });
    }

    /** Lista as N análises mais recentes (usado pelo comando {@code /history}). */
    public List<Reuniao> listarUltimos(int limite) {
        Connection con = null;
        try {
            con = ConnectionFactory.abrirConexao();
            return new ReuniaoDAOImpl(con).listarUltimos(limite);
        } catch (SQLException | IllegalStateException e) {
            return List.of();
        } finally {
            ConnectionFactory.fecharConexao(con);
        }
    }

    /** Busca uma análise específica pelo identificador. */
    public Optional<Reuniao> buscarPorId(String idReuniao) {
        Connection con = null;
        try {
            con = ConnectionFactory.abrirConexao();
            return new ReuniaoDAOImpl(con).buscarPorId(idReuniao);
        } catch (SQLException | IllegalStateException e) {
            return Optional.empty();
        } finally {
            ConnectionFactory.fecharConexao(con);
        }
    }

    /**
     * Busca uma análise específica pelo título — chave usada pelos comandos
     * {@code /read}, {@code /rename} e {@code /delete}.
     */
    public Optional<Reuniao> buscarPorTitulo(String titulo) {
        Connection con = null;
        try {
            con = ConnectionFactory.abrirConexao();
            return new ReuniaoDAOImpl(con).buscarPorTitulo(titulo);
        } catch (SQLException | IllegalStateException e) {
            return Optional.empty();
        } finally {
            ConnectionFactory.fecharConexao(con);
        }
    }

    /** Verifica se já existe alguma análise com o título informado. */
    public boolean existeTitulo(String titulo) {
        return existeTitulo(titulo, null);
    }

    /**
     * Verifica se já existe alguma análise com o título informado, ignorando
     * o registro de identificador {@code idExcluir} (usado em /rename para não
     * acusar colisão do registro consigo mesmo).
     */
    public boolean existeTitulo(String titulo, String idExcluir) {
        Connection con = null;
        try {
            con = ConnectionFactory.abrirConexao();
            return new ReuniaoDAOImpl(con).existeTitulo(titulo, idExcluir);
        } catch (SQLException | IllegalStateException e) {
            return false;
        } finally {
            ConnectionFactory.fecharConexao(con);
        }
    }

    /** Renomeia (atualiza o título de exibição de) uma análise, pelo identificador interno. */
    public String renomear(String idReuniao, String novoTitulo) {
        return executarComConexao(dao -> dao.atualizarTitulo(idReuniao, novoTitulo));
    }

    /**
     * Renomeia uma análise localizando-a pelo título atual. Alteração é
     * aplicada diretamente no banco em uma única conexão.
     */
    public String renomearPorTitulo(String tituloAtual, String novoTitulo) {
        return executarComConexao(dao -> {
            Optional<Reuniao> existente = dao.buscarPorTitulo(tituloAtual);
            if (existente.isEmpty()) {
                return "Nenhuma análise encontrada com o título \"" + tituloAtual + "\"";
            }
            if (dao.existeTitulo(novoTitulo, existente.get().getIdReuniao())) {
                return "Já existe uma análise com o título \"" + novoTitulo + "\"";
            }
            return dao.atualizarTitulo(existente.get().getIdReuniao(), novoTitulo);
        });
    }

    /** Remove definitivamente uma análise persistida, pelo identificador interno. */
    public String excluir(String idReuniao) {
        return executarComConexao(dao -> dao.excluir(idReuniao));
    }

    /**
     * Remove definitivamente uma análise localizando-a pelo título. Alteração
     * é aplicada diretamente no banco em uma única conexão.
     */
    public String excluirPorTitulo(String titulo) {
        return executarComConexao(dao -> {
            Optional<Reuniao> existente = dao.buscarPorTitulo(titulo);
            if (existente.isEmpty()) {
                return "Nenhuma análise encontrada com o título \"" + titulo + "\"";
            }
            return dao.excluir(existente.get().getIdReuniao());
        });
    }

    /**
     * Deriva, a partir do identificador bruto da conversa, um título válido:
     * sem espaços (substituídos por "_") e único no banco — em caso de
     * colisão, agrega um sufixo numérico incremental.
     */
    private String tituloUnicoESemEspaco(ReuniaoDAO dao, String base) {
        String tituloBase = base == null ? "" : base.trim().replaceAll("\\s+", "_");
        if (tituloBase.isBlank()) {
            tituloBase = "analise";
        }

        String candidato = tituloBase;
        int sufixo = 2;
        while (dao.existeTitulo(candidato, null)) {
            candidato = tituloBase + "_" + sufixo;
            sufixo++;
        }
        return candidato;
    }
}
