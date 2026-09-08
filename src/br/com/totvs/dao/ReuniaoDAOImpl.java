package br.com.totvs.dao;

import br.com.totvs.dto.Reuniao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementação JDBC do CRUD de {@link Reuniao} sobre a tabela Oracle
 * {@code CONTEXT_REUNIAO}.
 * <p>
 * Todas as operações são defensivas quanto a uma conexão nula ou fechada,
 * utilizam {@code try-with-resources} para liberar {@link PreparedStatement}
 * e {@link ResultSet}, e nunca propagam {@link SQLException} — o resultado
 * de cada operação de escrita é comunicado por uma mensagem descritiva,
 * enquanto operações de leitura retornam coleções vazias/{@link Optional#empty()}
 * em caso de falha, registrando o erro em {@code System.err}.
 */
public class ReuniaoDAOImpl implements ReuniaoDAO {

    private static final String COLUNAS =
            "ID_REUNIAO, TITULO, TRANSCRICAO, PARTICIPANTES, ORIGEM_ENTRADA, "
                    + "DATA_ANALISE, PRED_RISCO, PRED_OPORTUNIDADE, SENTIMENTO, "
                    + "PRODUTOS_DETECTADOS, MODELO_ANALISE, CLASSE_MODELO, "
                    + "PROBABILIDADE_MODELO, FONTE_DECISAO";

    private final Connection con;

    public ReuniaoDAOImpl(Connection con) {
        this.con = con;
    }

    private boolean conexaoIndisponivel() {
        try {
            return con == null || con.isClosed();
        } catch (SQLException e) {
            return true;
        }
    }

    @Override
    public String inserir(Reuniao reuniao) {
        if (reuniao == null || reuniao.getIdReuniao() == null || reuniao.getIdReuniao().isBlank()) {
            return "Não é possível inserir: identificador da reunião não informado";
        }
        if (conexaoIndisponivel()) {
            return "Conexao nao estabelecida";
        }

        String sql = "INSERT INTO CONTEXT_REUNIAO (" + COLUNAS + ") "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, reuniao.getIdReuniao());
            ps.setString(2, tituloOuPadrao(reuniao));
            ps.setString(3, reuniao.getTranscricao());
            ps.setString(4, reuniao.getParticipantes());
            ps.setString(5, reuniao.getOrigemEntrada());

            Timestamp dataAnalise = reuniao.getDataAnalise() != null
                    ? reuniao.getDataAnalise()
                    : new Timestamp(System.currentTimeMillis());
            ps.setTimestamp(6, dataAnalise);

            ps.setObject(7, reuniao.getPredRisco(), Types.NUMERIC);
            ps.setObject(8, reuniao.getPredOportunidade(), Types.NUMERIC);
            ps.setString(9, reuniao.getSentimento());
            ps.setString(10, reuniao.getProdutosDetectados());
            ps.setString(11, reuniao.getModeloAnalise());
            ps.setString(12, reuniao.getClasseModelo());

            if (reuniao.getProbabilidadeModelo() == null) {
                ps.setNull(13, Types.NUMERIC);
            } else {
                ps.setBigDecimal(13, reuniao.getProbabilidadeModelo());
            }

            ps.setString(14, reuniao.getFonteDecisao());

            int linhasAfetadas = ps.executeUpdate();
            return linhasAfetadas > 0
                    ? "Registro inserido com sucesso"
                    : "Nenhum registro foi inserido";
        } catch (SQLException e) {
            if (isViolacaoChaveUnica(e)) {
                return "Já existe uma análise registrada com este identificador";
            }
            return "Erro de SQL ao inserir reuniao: " + e.getMessage();
        }
    }

    @Override
    public String alterar(Reuniao reuniao) {
        if (reuniao == null || reuniao.getIdReuniao() == null || reuniao.getIdReuniao().isBlank()) {
            return "Não é possível alterar: identificador da reunião não informado";
        }
        if (conexaoIndisponivel()) {
            return "Conexao nao estabelecida";
        }

        String sql = "UPDATE CONTEXT_REUNIAO SET "
                + "TITULO = ?, TRANSCRICAO = ?, PARTICIPANTES = ?, ORIGEM_ENTRADA = ?, "
                + "DATA_ANALISE = ?, PRED_RISCO = ?, PRED_OPORTUNIDADE = ?, SENTIMENTO = ?, "
                + "PRODUTOS_DETECTADOS = ?, MODELO_ANALISE = ?, CLASSE_MODELO = ?, "
                + "PROBABILIDADE_MODELO = ?, FONTE_DECISAO = ? "
                + "WHERE ID_REUNIAO = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, tituloOuPadrao(reuniao));
            ps.setString(2, reuniao.getTranscricao());
            ps.setString(3, reuniao.getParticipantes());
            ps.setString(4, reuniao.getOrigemEntrada());

            Timestamp dataAnalise = reuniao.getDataAnalise() != null
                    ? reuniao.getDataAnalise()
                    : new Timestamp(System.currentTimeMillis());
            ps.setTimestamp(5, dataAnalise);

            ps.setObject(6, reuniao.getPredRisco(), Types.NUMERIC);
            ps.setObject(7, reuniao.getPredOportunidade(), Types.NUMERIC);
            ps.setString(8, reuniao.getSentimento());
            ps.setString(9, reuniao.getProdutosDetectados());
            ps.setString(10, reuniao.getModeloAnalise());
            ps.setString(11, reuniao.getClasseModelo());

            if (reuniao.getProbabilidadeModelo() == null) {
                ps.setNull(12, Types.NUMERIC);
            } else {
                ps.setBigDecimal(12, reuniao.getProbabilidadeModelo());
            }

            ps.setString(13, reuniao.getFonteDecisao());
            ps.setString(14, reuniao.getIdReuniao());

            int linhasAfetadas = ps.executeUpdate();
            return linhasAfetadas > 0
                    ? "Registro alterado com sucesso"
                    : "Nenhum registro encontrado para alterar";
        } catch (SQLException e) {
            return "Erro de SQL ao alterar reuniao: " + e.getMessage();
        }
    }

    @Override
    public String atualizarTitulo(String idReuniao, String novoTitulo) {
        if (idReuniao == null || idReuniao.isBlank()) {
            return "Não é possível renomear: identificador da reunião não informado";
        }
        if (novoTitulo == null || novoTitulo.isBlank()) {
            return "Não é possível renomear: novo título não pode ser vazio";
        }
        if (conexaoIndisponivel()) {
            return "Conexao nao estabelecida";
        }

        String sql = "UPDATE CONTEXT_REUNIAO SET TITULO = ? WHERE ID_REUNIAO = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, novoTitulo.trim());
            ps.setString(2, idReuniao);

            int linhasAfetadas = ps.executeUpdate();
            return linhasAfetadas > 0
                    ? "Título atualizado com sucesso"
                    : "Nenhum registro encontrado para renomear";
        } catch (SQLException e) {
            return "Erro de SQL ao renomear reuniao: " + e.getMessage();
        }
    }

    @Override
    public String excluir(String idReuniao) {
        if (idReuniao == null || idReuniao.isBlank()) {
            return "Não é possível excluir: identificador da reunião não informado";
        }
        if (conexaoIndisponivel()) {
            return "Conexao nao estabelecida";
        }

        String sql = "DELETE FROM CONTEXT_REUNIAO WHERE ID_REUNIAO = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, idReuniao);

            int linhasAfetadas = ps.executeUpdate();
            return linhasAfetadas > 0
                    ? "Registro excluido com sucesso"
                    : "Nenhum registro encontrado para excluir";
        } catch (SQLException e) {
            return "Erro de SQL ao excluir reuniao: " + e.getMessage();
        }
    }

    @Override
    public Optional<Reuniao> buscarPorId(String idReuniao) {
        if (idReuniao == null || idReuniao.isBlank() || conexaoIndisponivel()) {
            return Optional.empty();
        }

        String sql = "SELECT " + COLUNAS + " FROM CONTEXT_REUNIAO WHERE ID_REUNIAO = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, idReuniao);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapearLinha(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            System.err.println("Erro de SQL ao buscar reuniao: " + e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<Reuniao> buscarPorTitulo(String titulo) {
        if (titulo == null || titulo.isBlank() || conexaoIndisponivel()) {
            return Optional.empty();
        }

        String sql = "SELECT " + COLUNAS + " FROM CONTEXT_REUNIAO WHERE TITULO = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, titulo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapearLinha(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            System.err.println("Erro de SQL ao buscar reuniao por titulo: " + e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public boolean existeTitulo(String titulo, String idExcluir) {
        if (titulo == null || titulo.isBlank() || conexaoIndisponivel()) {
            return false;
        }

        String sql = "SELECT 1 FROM CONTEXT_REUNIAO WHERE TITULO = ? "
                + "AND (? IS NULL OR ID_REUNIAO <> ?)";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, titulo);
            ps.setString(2, idExcluir);
            ps.setString(3, idExcluir);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("Erro de SQL ao verificar titulo existente: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean existe(String idReuniao) {
        if (idReuniao == null || idReuniao.isBlank() || conexaoIndisponivel()) {
            return false;
        }

        String sql = "SELECT 1 FROM CONTEXT_REUNIAO WHERE ID_REUNIAO = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, idReuniao);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("Erro de SQL ao verificar existencia da reuniao: " + e.getMessage());
            return false;
        }
    }

    @Override
    public List<Reuniao> listarTodos() {
        return listarComLimite(null);
    }

    @Override
    public List<Reuniao> listarUltimos(int limite) {
        return listarComLimite(Math.max(limite, 0));
    }

    private List<Reuniao> listarComLimite(Integer limite) {
        List<Reuniao> reunioes = new ArrayList<>();

        if (conexaoIndisponivel()) {
            System.err.println("Conexao nao estabelecida");
            return reunioes;
        }

        String sql = "SELECT " + COLUNAS + " FROM CONTEXT_REUNIAO ORDER BY DATA_ANALISE DESC"
                + (limite != null ? " FETCH FIRST ? ROWS ONLY" : "");

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            if (limite != null) {
                ps.setInt(1, limite);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    reunioes.add(mapearLinha(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro de SQL ao listar reunioes: " + e.getMessage());
        }

        return reunioes;
    }

    @Override
    public long contar() {
        if (conexaoIndisponivel()) {
            return 0L;
        }

        String sql = "SELECT COUNT(*) FROM CONTEXT_REUNIAO";

        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0L;
        } catch (SQLException e) {
            System.err.println("Erro de SQL ao contar reunioes: " + e.getMessage());
            return 0L;
        }
    }

    private Reuniao mapearLinha(ResultSet rs) throws SQLException {
        Reuniao reuniao = new Reuniao();

        reuniao.setIdReuniao(rs.getString("ID_REUNIAO"));
        reuniao.setTitulo(rs.getString("TITULO"));
        reuniao.setTranscricao(rs.getString("TRANSCRICAO"));
        reuniao.setParticipantes(rs.getString("PARTICIPANTES"));
        reuniao.setOrigemEntrada(rs.getString("ORIGEM_ENTRADA"));
        reuniao.setDataAnalise(rs.getTimestamp("DATA_ANALISE"));

        int predRisco = rs.getInt("PRED_RISCO");
        reuniao.setPredRisco(rs.wasNull() ? null : predRisco);

        int predOportunidade = rs.getInt("PRED_OPORTUNIDADE");
        reuniao.setPredOportunidade(rs.wasNull() ? null : predOportunidade);

        reuniao.setSentimento(rs.getString("SENTIMENTO"));
        reuniao.setProdutosDetectados(rs.getString("PRODUTOS_DETECTADOS"));
        reuniao.setModeloAnalise(rs.getString("MODELO_ANALISE"));
        reuniao.setClasseModelo(rs.getString("CLASSE_MODELO"));
        reuniao.setProbabilidadeModelo(rs.getBigDecimal("PROBABILIDADE_MODELO"));
        reuniao.setFonteDecisao(rs.getString("FONTE_DECISAO"));

        return reuniao;
    }

    private String tituloOuPadrao(Reuniao reuniao) {
        return (reuniao.getTitulo() == null || reuniao.getTitulo().isBlank())
                ? reuniao.getIdReuniao()
                : reuniao.getTitulo().trim();
    }

    private boolean isViolacaoChaveUnica(SQLException e) {
        // ORA-00001: unique constraint violated
        return e.getErrorCode() == 1;
    }
}
