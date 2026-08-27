package br.com.totvs.dao;

import br.com.totvs.dto.Reuniao;

import java.sql.Connection;
import java.util.ArrayList;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import java.sql.Timestamp;
import java.sql.Types;

import java.sql.ResultSet;


public class ReuniaoDAOImpl implements ReuniaoDAO{
    private Connection con;
    public ReuniaoDAOImpl(Connection con) {
        this.con = con;
    }

    @Override
    public String inserir(Reuniao reuniao) {
        String sql = "INSERT INTO CONTEXT_REUNIAO "
                + "(ID_REUNIAO, TRANSCRICAO, PARTICIPANTES, ORIGEM_ENTRADA, "
                + "DATA_ANALISE, PRED_RISCO, PRED_OPORTUNIDADE, SENTIMENTO, "
                + "PRODUTOS_DETECTADOS, MODELO_ANALISE, CLASSE_MODELO, "
                + "PROBABILIDADE_MODELO, FONTE_DECISAO) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        if (con == null) {
            return "Conexao nao estabelecida";
        }

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, reuniao.getIdReuniao());
            ps.setString(2, reuniao.getTranscricao());
            ps.setString(3, reuniao.getParticipantes());
            ps.setString(4, reuniao.getOrigemEntrada());

            Timestamp dataAnalise = reuniao.getDataAnalise();
            if (dataAnalise == null) {
                dataAnalise = new Timestamp(System.currentTimeMillis());
            }
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

            int linhasAfetadas = ps.executeUpdate();

            if (linhasAfetadas > 0) {
                return "Registro inserido com sucesso";
            }

            return "Nenhum registro foi inserido";
        } catch (SQLException e) {
            return "Erro de SQL ao inserir reuniao: " + e.getMessage();
        }
    }


    @Override
    public String alterar(Reuniao reuniao) {
        String sql = "UPDATE CONTEXT_REUNIAO SET "
                + "TRANSCRICAO = ?, "
                + "PARTICIPANTES = ?, "
                + "ORIGEM_ENTRADA = ?, "
                + "DATA_ANALISE = ?, "
                + "PRED_RISCO = ?, "
                + "PRED_OPORTUNIDADE = ?, "
                + "SENTIMENTO = ?, "
                + "PRODUTOS_DETECTADOS = ?, "
                + "MODELO_ANALISE = ? "
                + "WHERE ID_REUNIAO = ?";
        if (con == null) {
            return "Conexao nao estabelecida";
        }

        try  (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, reuniao.getTranscricao());
            ps.setString(2, reuniao.getParticipantes());
            ps.setString(3, reuniao.getOrigemEntrada());

            Timestamp dataAnalise = reuniao.getDataAnalise();
            if (dataAnalise == null) {
                dataAnalise = new Timestamp(System.currentTimeMillis());
            }
            ps.setTimestamp(4, dataAnalise);

            ps.setObject(5, reuniao.getPredRisco(), Types.NUMERIC);
            ps.setObject(6, reuniao.getPredOportunidade(), Types.NUMERIC);
            ps.setString(7, reuniao.getSentimento());
            ps.setString(8, reuniao.getProdutosDetectados());
            ps.setString(9, reuniao.getModeloAnalise());
            ps.setString(10, reuniao.getIdReuniao());

            int linhasAfetadas = ps.executeUpdate();
            if (linhasAfetadas > 0) {
                return "Registro alterado com sucesso";
            }

            return "Nenhum registro encontrado para alterar";
        } catch (SQLException e) {
            return "Erro de SQL ao alterar reuniao: " + e.getMessage();
        }
    }

    @Override
    public String excluir(String idReuniao) {
        String sql = "DELETE FROM CONTEXT_REUNIAO WHERE ID_REUNIAO = ?";

        if (con == null) {
            return "Conexao nao estabelecida";
        }

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, idReuniao);

            int linhasAfetadas = ps.executeUpdate();

            if (linhasAfetadas > 0) {
                return "Registro excluido com sucesso";
            }

            return "Nenhum registro encontrado para excluir";
        } catch (SQLException e) {
            return "Erro de SQL ao excluir reuniao: " + e.getMessage();
        }
    }

    @Override
    public ArrayList<Reuniao> listarTodos() {
        String sql = "SELECT ID_REUNIAO, TRANSCRICAO, PARTICIPANTES, "
                + "ORIGEM_ENTRADA, DATA_ANALISE, PRED_RISCO, "
                + "PRED_OPORTUNIDADE, SENTIMENTO, PRODUTOS_DETECTADOS, "
                + "MODELO_ANALISE "
                + "FROM CONTEXT_REUNIAO "
                + "ORDER BY DATA_ANALISE DESC";

        ArrayList<Reuniao> reunioes = new ArrayList<>();
        if (con == null) {
            System.out.println("Conexao nao estabelecida");
            return reunioes;
        }
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Reuniao reuniao = new Reuniao();

                reuniao.setIdReuniao(rs.getString("ID_REUNIAO"));
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

                reunioes.add(reuniao);
            }
        } catch (SQLException e) {
            System.out.println("Erro de SQL ao listar reunioes: " + e.getMessage());
        }

        return reunioes;
    }
}
