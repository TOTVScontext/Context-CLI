package br.com.totvs.dao;

import br.com.totvs.dto.Reuniao;
import java.util.ArrayList;

public interface ReuniaoDAO {
    String inserir(Reuniao reuniao);
    String alterar(Reuniao reuniao);
    String excluir(String idReuniao);
    ArrayList<Reuniao> listarTodos();
}
