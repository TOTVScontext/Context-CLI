package br.com.totvs.dao;

import br.com.totvs.dto.Reuniao;

import java.sql.Connection;
import java.util.ArrayList;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import java.sql.Timestamp;
import java.sql.Types;


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
                + "PRODUTOS_DETECTADOS, MODELO_ANALISE) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

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
        return "";
    }
    @Override
    public String excluir(String idReuniao) {
        return "";
    }
    @Override
    public ArrayList<Reuniao> listarTodos() {
        return null;
    }
}
