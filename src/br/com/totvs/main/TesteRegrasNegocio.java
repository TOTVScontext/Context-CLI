package br.com.totvs.main;

import br.com.totvs.domain.Analysis;
import br.com.totvs.domain.InsightService;

public class TesteRegrasNegocio {

    private static void validar(String nomeTeste, boolean resultado) {
        if (resultado) {
            System.out.println("[PASSOU] " + nomeTeste);
        } else {
            System.out.println("[FALHOU] " + nomeTeste);
        }
    }

    public static void main(String[] args) {
        InsightService service = new InsightService(7.0);

        Analysis analiseComRisco = new Analysis(
                5.0, 3.0, 5.0,
                false, false, false, false, false
        );

        Analysis analiseSemRisco = new Analysis(
                5.0, 8.0, 5.0,
                false, false, false, false, false
        );

        validar(
                "Risco de churn — verdadeiro e falso",
                service.verificarRiscoChurn(analiseComRisco)
                        && !service.verificarRiscoChurn(analiseSemRisco)
        );

        Analysis analiseComProdutividade = new Analysis(
                9.0, 8.0, 5.0,
                false, false, false, false, false
        );

        Analysis analiseSemProdutividade = new Analysis(
                5.0, 8.0, 5.0,
                false, false, false, false, false
        );

        validar(
                "Oportunidade de upsell — verdadeiro e falso",
                service.verificarOportunidadeUpsell(analiseComProdutividade)
                        && !service.verificarOportunidadeUpsell(analiseSemProdutividade)
        );

        Analysis analiseComReclamacao = new Analysis(
                5.0, 8.0, 5.0,
                true, false, false, false, false
        );

        Analysis analiseSemReclamacao = new Analysis(
                5.0, 8.0, 5.0,
                false, false, false, false, false
        );

        validar(
                "Reclamacao — verdadeiro e falso",
                service.verificarReclamacao(analiseComReclamacao)
                        && !service.verificarReclamacao(analiseSemReclamacao)
        );

        Analysis analiseComBudget = new Analysis(
                5.0, 8.0, 5.0,
                false, true, false, false, false
        );

        Analysis analiseSemBudget = new Analysis(
                5.0, 8.0, 5.0,
                false, false, false, false, false
        );

        validar(
                "Budget — verdadeiro e falso",
                service.verificarBudget(analiseComBudget)
                        && !service.verificarBudget(analiseSemBudget)
        );
    }
}
