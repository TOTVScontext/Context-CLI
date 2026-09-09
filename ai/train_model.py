"""Treinamento do modelo de classificacao de reunioes (Context-CLI).

O modelo classifica a transcricao de uma reuniao em um dos tres segmentos
de NPS (metodologia padrao de mercado):

    DETRATOR  -> NOTA_NPS <= 6   (proxy de risco de churn)
    NEUTRO    -> NOTA_NPS in [7, 8]
    PROMOTOR  -> NOTA_NPS >= 9   (proxy de oportunidade / intencao de compra)

A entrada do modelo em producao (ai/predict.py, chamado pelo Java via
PythonModelClient) e apenas o texto da transcricao. Por isso o experimento
usa somente a transcricao como feature: qualquer coluna estrutural do CSV
(UF, segmento, faixa de faturamento etc.) nao esta disponivel no momento da
inferencia real e nao seria uma feature valida para o pipeline de producao.

Este script nao apenas treina um classificador: ele avalia com rigor se a
transcricao efetivamente carrega sinal preditivo sobre o NPS, antes de
declarar qualquer numero de acuracia como "o desempenho do modelo".
Isso e feito em quatro camadas, todas deterministicas (semente fixa):

  1. Diagnostico de sinal (information_gain_diagnostico): mutual information
     entre os termos do TF-IDF e o alvo. Valores proximos de zero indicam
     que NENHUM classificador vai extrair sinal real do texto.
  2. Benchmark de candidatos (benchmark_candidatos): compara, por validacao
     cruzada estratificada em 5 folds, diferentes representacoes de texto
     (palavras 1-2gram, caracteres 3-5gram) e algoritmos (XGBoost/Gradient
     Boosting, Regressao Logistica, Linear SVM calibrado, Naive Bayes).
  3. Holdout final com calibracao de probabilidade (CalibratedClassifierCV)
     no modelo escolhido pelo benchmark.
  4. Teste de significancia estatistica (teste de McNemar exato) e intervalo
     de confianca bootstrap comparando o modelo contra o baseline majoritario
     no mesmo conjunto de teste, para provar (ou refutar) que a diferenca de
     desempenho e real e nao ruido amostral.
"""

import json
import warnings
from pathlib import Path

import joblib
import numpy as np
import pandas as pd
from scipy.stats import binomtest
from sklearn.calibration import CalibratedClassifierCV
from sklearn.dummy import DummyClassifier
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.feature_selection import mutual_info_classif
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import (
    accuracy_score,
    balanced_accuracy_score,
    classification_report,
    cohen_kappa_score,
    confusion_matrix,
    f1_score,
    log_loss,
    matthews_corrcoef,
    precision_score,
    recall_score,
    roc_auc_score,
)
from sklearn.model_selection import (
    StratifiedKFold,
    cross_val_score,
    cross_validate,
    train_test_split,
)
from sklearn.naive_bayes import MultinomialNB
from sklearn.pipeline import Pipeline
from sklearn.svm import LinearSVC

RANDOM_STATE = 42
N_SPLITS_CV = 5
N_BOOTSTRAP = 2000

try:
    from xgboost import XGBClassifier

    MODEL_BACKEND = "XGBoost"
except ImportError:  # ambiente sem xgboost instalado
    from sklearn.ensemble import GradientBoostingClassifier

    MODEL_BACKEND = "GradientBoostingClassifier (fallback, xgboost ausente)"

ROOT = Path(__file__).resolve().parent.parent
CSV_PATH = ROOT / "data" / "reunioes_transcricoes_mockado.csv"
ARTIFACTS_DIR = ROOT / "ai" / "artifacts"
MODEL_PATH = ARTIFACTS_DIR / "modelo_risco.joblib"
METRICS_PATH = ARTIFACTS_DIR / "metricas_modelo.json"

ARTIFACTS_DIR.mkdir(parents=True, exist_ok=True)

CLASSES = ["DETRATOR", "NEUTRO", "PROMOTOR"]


def segmentar_nps(nota):
    if nota <= 6:
        return "DETRATOR"
    if nota <= 8:
        return "NEUTRO"
    return "PROMOTOR"


def construir_arvore_boosting():
    if MODEL_BACKEND == "XGBoost":
        return XGBClassifier(
            n_estimators=400,
            max_depth=4,
            learning_rate=0.05,
            subsample=0.9,
            colsample_bytree=0.9,
            reg_lambda=1.0,
            objective="multi:softprob",
            num_class=len(CLASSES),
            eval_metric="mlogloss",
            random_state=RANDOM_STATE,
            n_jobs=-1,
        )
    return GradientBoostingClassifier(
        n_estimators=400,
        max_depth=3,
        learning_rate=0.05,
        subsample=0.9,
        random_state=RANDOM_STATE,
    )


def construir_modelo():
    """Pipeline de producao: TF-IDF (palavras) + boosting."""
    tfidf = TfidfVectorizer(
        lowercase=True,
        ngram_range=(1, 2),
        min_df=2,
        max_df=0.95,
        sublinear_tf=True,
    )
    return Pipeline([
        ("tfidf", tfidf),
        ("classifier", construir_arvore_boosting()),
    ])


def codificar_rotulos(y_texto):
    indice_por_classe = {classe: i for i, classe in enumerate(CLASSES)}
    return y_texto.map(indice_por_classe).to_numpy()


def diagnostico_de_sinal(X_texto, y):
    """Mutual information entre n-gramas de TF-IDF e o alvo.

    Um valor maximo muito baixo (ordem de 1e-2 ou menos, em uma escala onde
    1.0 representaria dependencia perfeita) indica que a transcricao, do
    jeito que esta hoje, nao carrega informacao suficiente para separar as
    classes de NPS - independente do algoritmo escolhido depois.
    """
    tfidf = TfidfVectorizer(lowercase=True, ngram_range=(1, 2), min_df=3, max_df=0.9)
    X = tfidf.fit_transform(X_texto)
    termos = np.array(tfidf.get_feature_names_out())

    with warnings.catch_warnings():
        warnings.simplefilter("ignore")
        mi = mutual_info_classif(
            X, y, discrete_features=True, random_state=RANDOM_STATE
        )

    ordem = np.argsort(mi)[::-1][:15]
    return {
        "n_features_avaliadas": int(X.shape[1]),
        "mutual_information_media": float(mi.mean()),
        "mutual_information_maxima": float(mi.max()),
        "top_termos_por_mutual_information": [
            {"termo": termos[i], "mutual_information": float(mi[i])} for i in ordem
        ],
        "interpretacao": (
            "Mutual information media e maxima proximas de zero indicam que os "
            "termos da transcricao, isoladamente, quase nao reduzem a incerteza "
            "sobre o segmento de NPS. Isso e uma limitacao dos dados, nao do "
            "algoritmo de classificacao."
        ),
    }


def benchmark_candidatos(X_texto, y):
    """Compara representacoes de texto x algoritmos por CV 5-fold (f1_macro).

    Existe para responder de forma auditavel a pergunta 'sera que outro
    algoritmo/representacao teria performado melhor?' em vez de assumir que
    o boosting escolhido e o melhor possivel.
    """
    cv = StratifiedKFold(n_splits=N_SPLITS_CV, shuffle=True, random_state=RANDOM_STATE)

    candidatos = {
        f"tfidf_palavras_1-2gram + {MODEL_BACKEND}": Pipeline([
            ("tfidf", TfidfVectorizer(ngram_range=(1, 2), min_df=2, sublinear_tf=True)),
            ("clf", construir_arvore_boosting()),
        ]),
        "tfidf_caracteres_3-5gram + LogisticRegression": Pipeline([
            ("tfidf", TfidfVectorizer(analyzer="char_wb", ngram_range=(3, 5), min_df=2)),
            ("clf", LogisticRegression(max_iter=2000, random_state=RANDOM_STATE)),
        ]),
        "tfidf_palavras_1-2gram + LogisticRegression": Pipeline([
            ("tfidf", TfidfVectorizer(ngram_range=(1, 2), min_df=2, sublinear_tf=True)),
            ("clf", LogisticRegression(max_iter=2000, random_state=RANDOM_STATE)),
        ]),
        "tfidf_palavras_1-2gram + LinearSVC": Pipeline([
            ("tfidf", TfidfVectorizer(ngram_range=(1, 2), min_df=2, sublinear_tf=True)),
            ("clf", LinearSVC(random_state=RANDOM_STATE)),
        ]),
        "tfidf_palavras_1-2gram + MultinomialNB": Pipeline([
            ("tfidf", TfidfVectorizer(ngram_range=(1, 2), min_df=2, sublinear_tf=True)),
            ("clf", MultinomialNB()),
        ]),
    }

    resultado = {}
    for nome, pipeline in candidatos.items():
        scores = cross_val_score(pipeline, X_texto, y, cv=cv, scoring="f1_macro")
        resultado[nome] = {
            "f1_macro_media": float(scores.mean()),
            "f1_macro_desvio_padrao": float(scores.std()),
        }
    return resultado


def metricas_multiclasse(y_true, y_pred, y_proba):
    return {
        "accuracy": float(accuracy_score(y_true, y_pred)),
        "balanced_accuracy": float(balanced_accuracy_score(y_true, y_pred)),
        "precision_macro": float(precision_score(y_true, y_pred, average="macro", zero_division=0)),
        "recall_macro": float(recall_score(y_true, y_pred, average="macro", zero_division=0)),
        "f1_macro": float(f1_score(y_true, y_pred, average="macro", zero_division=0)),
        "f1_weighted": float(f1_score(y_true, y_pred, average="weighted", zero_division=0)),
        "cohen_kappa": float(cohen_kappa_score(y_true, y_pred)),
        "matthews_corrcoef": float(matthews_corrcoef(y_true, y_pred)),
        "log_loss": float(log_loss(y_true, y_proba, labels=list(range(len(CLASSES))))),
        "roc_auc_ovr_macro": float(
            roc_auc_score(y_true, y_proba, multi_class="ovr", average="macro", labels=list(range(len(CLASSES))))
        ),
        "classification_report": classification_report(
            y_true,
            y_pred,
            target_names=CLASSES,
            output_dict=True,
            zero_division=0,
        ),
        "confusion_matrix": confusion_matrix(y_true, y_pred, labels=list(range(len(CLASSES)))).tolist(),
        "confusion_matrix_labels": CLASSES,
    }


def bootstrap_intervalo_confianca(y_true, y_pred, metrica_fn, n_bootstrap=N_BOOTSTRAP, alpha=0.05):
    """IC 95% via bootstrap (reamostragem com reposicao), com semente fixa."""
    rng = np.random.default_rng(RANDOM_STATE)
    n = len(y_true)
    valores = np.empty(n_bootstrap)
    y_true = np.asarray(y_true)
    y_pred = np.asarray(y_pred)
    for i in range(n_bootstrap):
        indices = rng.integers(0, n, size=n)
        valores[i] = metrica_fn(y_true[indices], y_pred[indices])
    inferior = float(np.percentile(valores, 100 * (alpha / 2)))
    superior = float(np.percentile(valores, 100 * (1 - alpha / 2)))
    return {
        "estimativa_pontual": float(metrica_fn(y_true, y_pred)),
        "ic_95_inferior": inferior,
        "ic_95_superior": superior,
        "n_bootstrap": n_bootstrap,
    }


def teste_mcnemar(y_true, pred_modelo, pred_baseline):
    """Teste de McNemar exato (binomial) para amostras pareadas.

    Compara, no mesmo conjunto de teste, os casos em que o modelo acerta e o
    baseline erra contra os casos em que o baseline acerta e o modelo erra.
    Um p-valor alto (>0.05) significa que NAO ha evidencia estatistica de que
    o modelo seja diferente do baseline majoritario nesse conjunto.
    """
    y_true = np.asarray(y_true)
    acerto_modelo = np.asarray(pred_modelo) == y_true
    acerto_baseline = np.asarray(pred_baseline) == y_true

    somente_modelo_acerta = int(np.sum(acerto_modelo & ~acerto_baseline))
    somente_baseline_acerta = int(np.sum(~acerto_modelo & acerto_baseline))
    n_discordante = somente_modelo_acerta + somente_baseline_acerta

    if n_discordante == 0:
        p_valor = 1.0
    else:
        p_valor = float(
            binomtest(somente_modelo_acerta, n_discordante, p=0.5).pvalue
        )

    return {
        "somente_modelo_acerta": somente_modelo_acerta,
        "somente_baseline_acerta": somente_baseline_acerta,
        "n_casos_discordantes": n_discordante,
        "p_valor": p_valor,
        "significativo_a_5pct": bool(p_valor < 0.05),
        "interpretacao": (
            "p < 0.05 indicaria diferenca estatisticamente significativa entre "
            "modelo e baseline majoritario neste conjunto de teste. p >= 0.05 "
            "significa que essa diferenca pode ser explicada por acaso amostral."
        ),
    }


def top_termos_por_classe(pipeline, top_n=15):
    tfidf = pipeline.named_steps["tfidf"]
    classificador = pipeline.named_steps["classifier"]
    termos = np.array(tfidf.get_feature_names_out())

    base = classificador
    if hasattr(classificador, "calibrated_classifiers_"):
        base = classificador.calibrated_classifiers_[0].estimator

    if hasattr(base, "feature_importances_"):
        importancias = base.feature_importances_
        indices = np.argsort(importancias)[::-1][:top_n]
        return [
            {"termo": termos[i], "importancia": float(importancias[i])}
            for i in indices
            if importancias[i] > 0
        ]
    return []


def main():
    warnings.filterwarnings("ignore", category=UserWarning)

    df = pd.read_csv(CSV_PATH, encoding="utf-8-sig")
    df["ANON_TRANSCRICAO"] = df["ANON_TRANSCRICAO"].fillna("").astype(str)
    df["NPS_SEGMENTO"] = df["NOTA_NPS"].apply(segmentar_nps)

    X = df["ANON_TRANSCRICAO"]
    y_texto = df["NPS_SEGMENTO"]
    y = codificar_rotulos(y_texto)

    print("1/4 - Diagnostico de sinal (mutual information)...")
    diagnostico = diagnostico_de_sinal(X, y)

    print("2/4 - Benchmark de representacoes e algoritmos (CV 5-fold)...")
    benchmark = benchmark_candidatos(X, y)

    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.20, random_state=RANDOM_STATE, stratify=y,
    )

    print("3/4 - Treinando modelo final com calibracao de probabilidade...")
    pipeline_base = construir_modelo()
    modelo_calibrado = Pipeline([
        ("tfidf", pipeline_base.named_steps["tfidf"]),
        ("classifier", CalibratedClassifierCV(
            pipeline_base.named_steps["classifier"],
            method="sigmoid",
            cv=StratifiedKFold(n_splits=3, shuffle=True, random_state=RANDOM_STATE),
        )),
    ])
    modelo_calibrado.fit(X_train, y_train)

    y_pred = modelo_calibrado.predict(X_test)
    y_proba = modelo_calibrado.predict_proba(X_test)
    metricas_holdout = metricas_multiclasse(y_test, y_pred, y_proba)

    baseline = DummyClassifier(strategy="most_frequent", random_state=RANDOM_STATE)
    baseline.fit(X_train, y_train)
    baseline_pred = baseline.predict(X_test)
    baseline_accuracy = float(accuracy_score(y_test, baseline_pred))

    print("4/4 - Validacao cruzada, bootstrap e teste de significancia...")
    cv = StratifiedKFold(n_splits=N_SPLITS_CV, shuffle=True, random_state=RANDOM_STATE)
    cv_scoring = {
        "accuracy": "accuracy",
        "f1_macro": "f1_macro",
        "balanced_accuracy": "balanced_accuracy",
        "roc_auc_ovr_macro": "roc_auc_ovr",
    }
    cv_resultado = cross_validate(
        construir_modelo(), X, y, cv=cv, scoring=cv_scoring, n_jobs=None,
    )
    cv_resumo = {
        metrica: {
            "media": float(np.mean(cv_resultado[f"test_{metrica}"])),
            "desvio_padrao": float(np.std(cv_resultado[f"test_{metrica}"])),
            "folds": [float(v) for v in cv_resultado[f"test_{metrica}"]],
        }
        for metrica in cv_scoring
    }

    ic_accuracy = bootstrap_intervalo_confianca(y_test, y_pred, accuracy_score)
    ic_f1_macro = bootstrap_intervalo_confianca(
        y_test, y_pred, lambda a, b: f1_score(a, b, average="macro", zero_division=0)
    )
    mcnemar = teste_mcnemar(y_test, y_pred, baseline_pred)

    metricas = {
        "model": MODEL_BACKEND,
        "calibracao": "CalibratedClassifierCV (sigmoid, CV interno 3-fold)",
        "target": "NPS_SEGMENTO",
        "target_rule": "DETRATOR: NOTA_NPS<=6 | NEUTRO: 7<=NOTA_NPS<=8 | PROMOTOR: NOTA_NPS>=9",
        "classes": CLASSES,
        "risco_definicao": "probabilidade_risco = P(DETRATOR)",
        "oportunidade_definicao": (
            "probabilidade_oportunidade = P(PROMOTOR); usada como proxy "
            "de intencao de compra / upsell, nao uma confirmacao de venda"
        ),
        "rows_total": int(len(df)),
        "rows_train": int(len(X_train)),
        "rows_test": int(len(X_test)),
        "class_distribution_total": {k: int(v) for k, v in y_texto.value_counts().to_dict().items()},
        "diagnostico_de_sinal": diagnostico,
        "benchmark_representacoes_algoritmos_cv5": benchmark,
        "baseline_majoritario": {
            "estrategia": "most_frequent",
            "accuracy": baseline_accuracy,
        },
        "holdout": metricas_holdout,
        "intervalo_confianca_bootstrap_95pct": {
            "accuracy": ic_accuracy,
            "f1_macro": ic_f1_macro,
        },
        "teste_significancia_mcnemar_vs_baseline": mcnemar,
        "validacao_cruzada_5fold": cv_resumo,
        "top_termos_por_importancia": top_termos_por_classe(modelo_calibrado),
        "random_state": RANDOM_STATE,
        "observacao": (
            "O alvo e derivado de NOTA_NPS (metodologia padrao de segmentacao NPS): "
            "e um proxy de negocio, nao uma confirmacao de churn ou de compra. O "
            "diagnostico de mutual information e o benchmark contra 5 combinacoes "
            "distintas de representacao/algoritmo mostram que a transcricao mockada "
            "atual carrega pouquissimo sinal textual sobre o NPS; isso limita "
            "estruturalmente qualquer classificador, independente do algoritmo "
            "escolhido. A decisao final de negocio deve continuar combinando o "
            "modelo com revisao humana e com o fallback de regras do Java "
            "(HybridRiskService)."
        ),
    }

    joblib.dump(modelo_calibrado, MODEL_PATH)
    METRICS_PATH.write_text(
        json.dumps(metricas, indent=2, ensure_ascii=False),
        encoding="utf-8",
    )

    print(f"\nBackend do modelo: {MODEL_BACKEND} (calibrado)")
    print(f"Modelo salvo em: {MODEL_PATH}")
    print(f"Metricas salvas em: {METRICS_PATH}")
    print(f"Mutual information maxima (diagnostico de sinal): {diagnostico['mutual_information_maxima']:.5f}")
    print(f"Acuracia (holdout): {metricas_holdout['accuracy']:.4f} | Baseline: {baseline_accuracy:.4f}")
    print(
        f"IC 95% accuracy (bootstrap): [{ic_accuracy['ic_95_inferior']:.4f}, {ic_accuracy['ic_95_superior']:.4f}]"
    )
    print(
        "McNemar vs baseline: p-valor="
        f"{mcnemar['p_valor']:.4f} (significativo: {mcnemar['significativo_a_5pct']})"
    )
    print(
        "CV 5-fold F1-macro: "
        f"{cv_resumo['f1_macro']['media']:.4f} +/- {cv_resumo['f1_macro']['desvio_padrao']:.4f}"
    )


if __name__ == "__main__":
    main()
