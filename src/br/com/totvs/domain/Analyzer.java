package br.com.totvs.domain;

import java.text.Normalizer;

public class Analyzer {
    private final String nlpVersion;

    public Analyzer(String nlpVersion) {
        this.nlpVersion = nlpVersion;
    }

    public Analysis analyze(Conversation conversation) {
        String texto = normalizar(conversation.getText());

        double notaProdutividade = calcularProdutividade(texto);
        double notaSentimento = calcularSentimento(texto);
        double notaResolucao = calcularResolucao(texto);

        boolean temReclamacao = contemAlgum(texto,
                "odeio",
                "odio",
                "ruim",
                "pessimo",
                "frustrado",
                "problema",
                "lento",
                "trava",
                "cancelar",
                "cancelamento",
                "nao vou comprar",
                "nao quero comprar",
                "reclamacao"
        );

        boolean temBudget = contemAlgum(texto,
                "mil",
                "r$",
                "investimento",
                "valor",
                "preco",
                "orcamento",
                "budget"
        );

        boolean temPersona = contemAlgum(texto,
                "cfo",
                "diretor",
                "gestor",
                "roi"
        );

        boolean temSentimentoMisto =
                contemAlgum(texto, "satisfeito", "gosto", "gostei")
                        && contemAlgum(texto,
                        "frustrado",
                        "sofrendo",
                        "problema"
                );

        boolean temConfianca = contemAlgum(texto,
                "confianca",
                "confio",
                "acredito"
        );

        return new Analysis(
                notaProdutividade,
                notaSentimento,
                notaResolucao,
                temReclamacao,
                temBudget,
                temPersona,
                temSentimentoMisto,
                temConfianca
        );
    }

    private String normalizar(String texto) {
        if (texto == null) {
            return "";
        }

        return Normalizer.normalize(
                        texto,
                        Normalizer.Form.NFD
                )
                .replaceAll("[^\\p{ASCII}]", "")
                .toLowerCase()
                .trim();
    }

    private double calcularSentimento(String texto) {
        boolean negativo = contemAlgum(texto,
                "odeio",
                "odio",
                "ruim",
                "pessimo",
                "frustrado",
                "insatisfeito",
                "problema",
                "lento",
                "trava",
                "cancelar",
                "cancelamento",
                "nao gostei",
                "nao vou comprar",
                "nao quero comprar"
        );

        if (negativo) {
            return 3.0;
        }

        boolean positivo = contemAlgum(texto,
                "gostei",
                "gostou",
                "excelente",
                "satisfeito",
                "recomendo",
                "confianca",
                "quero comprar",
                "interesse"
        );

        if (positivo) {
            return 8.0;
        }

        return 5.0;
    }

    private double calcularProdutividade(String texto) {
        boolean negacaoDeCompra = contemAlgum(texto,
                "nao vou comprar",
                "nao quero comprar",
                "nao comprarei",
                "cancelar"
        );

        boolean interesseComercial = contemAlgum(texto,
                "comprar",
                "proposta",
                "interesse",
                "gostei",
                "gostou",
                "recomendo"
        );

        if (interesseComercial && !negacaoDeCompra) {
            return 9.5;
        }

        if (negacaoDeCompra || contemAlgum(texto,
                "odeio",
                "pessimo",
                "ruim",
                "frustrado"
        )) {
            return 2.5;
        }

        return 5.0;
    }

    private double calcularResolucao(String texto) {
        if (contemAlgum(texto,
                "resolvido",
                "solucionado",
                "funcionou",
                "conseguimos",
                "deu certo"
        )) {
            return 8.0;
        }

        if (contemAlgum(texto,
                "nao resolveu",
                "sem solucao",
                "problema",
                "lento",
                "trava",
                "cancelar",
                "cancelamento"
        )) {
            return 3.0;
        }

        return 5.0;
    }

    private boolean contemAlgum(String texto, String... termos) {
        for (String termo : termos) {
            if (texto.contains(termo)) {
                return true;
            }
        }
        return false;
    }
}
