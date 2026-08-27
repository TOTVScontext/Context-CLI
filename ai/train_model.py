import json
from pathlib import Path

import joblib
import pandas as pd
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import accuracy_score, classification_report, confusion_matrix
from sklearn.model_selection import train_test_split
from sklearn.pipeline import Pipeline


ROOT = Path(__file__).resolve().parent.parent
CSV_PATH = ROOT / "data" / "reunioes_transcricoes_mockado.csv"
ARTIFACTS_DIR = ROOT / "ai" / "artifacts"
MODEL_PATH = ARTIFACTS_DIR / "modelo_risco.joblib"
METRICS_PATH = ARTIFACTS_DIR / "metricas_modelo.json"

ARTIFACTS_DIR.mkdir(parents=True, exist_ok=True)

df = pd.read_csv(CSV_PATH, encoding="utf-8-sig")
df["ANON_TRANSCRICAO"] = df["ANON_TRANSCRICAO"].fillna("").astype(str)

df["RISCO"] = (df["NOTA_NPS"] <= 6).astype(int)

X = df["ANON_TRANSCRICAO"]
y = df["RISCO"]

X_train, X_test, y_train, y_test = train_test_split(
    X,
    y,
    test_size=0.20,
    random_state=42,
    stratify=y,
)

model = Pipeline([
    ("tfidf", TfidfVectorizer(
        lowercase=True,
        ngram_range=(1, 2),
        sublinear_tf=True,
    )),
    ("classifier", LogisticRegression(
        max_iter=2000,
        class_weight="balanced",
        random_state=42,
    )),
])

model.fit(X_train, y_train)
y_pred = model.predict(X_test)

metrics = {
    "model": "TfidfVectorizer + LogisticRegression",
    "target": "RISCO",
    "target_rule": "NOTA_NPS <= 6",
    "rows_total": int(len(df)),
    "rows_train": int(len(X_train)),
    "rows_test": int(len(X_test)),
    "class_distribution": {
        "risco": int(y.sum()),
        "sem_risco": int((y == 0).sum()),
    },
    "accuracy": float(accuracy_score(y_test, y_pred)),
    "confusion_matrix": confusion_matrix(y_test, y_pred).tolist(),
    "classification_report": classification_report(
        y_test,
        y_pred,
        target_names=["SEM_RISCO", "RISCO"],
        output_dict=True,
        zero_division=0,
    ),
}

joblib.dump(model, MODEL_PATH)
METRICS_PATH.write_text(
    json.dumps(metrics, indent=2, ensure_ascii=False),
    encoding="utf-8",
)

print(f"Modelo salvo em: {MODEL_PATH}")
print(f"Metricas salvas em: {METRICS_PATH}")
print(f"Acuracia: {metrics['accuracy']:.4f}")
print(f"Matriz de confusao: {metrics['confusion_matrix']}")
print("Distribuicao de classes:", metrics["class_distribution"])
