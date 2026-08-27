from pathlib import Path

import joblib
import pandas as pd
from sklearn.model_selection import train_test_split


ROOT = Path(__file__).resolve().parent.parent
CSV_PATH = ROOT / "data" / "reunioes_transcricoes_mockado.csv"
MODEL_PATH = ROOT / "ai" / "artifacts" / "modelo_risco.joblib"

df = pd.read_csv(CSV_PATH, encoding="utf-8-sig")
df["ANON_TRANSCRICAO"] = df["ANON_TRANSCRICAO"].fillna("").astype(str)
df["RISCO"] = (df["NOTA_NPS"] <= 6).astype(int)

_, textos_teste, _, _ = train_test_split(
    df["ANON_TRANSCRICAO"],
    df["RISCO"],
    test_size=0.20,
    random_state=42,
    stratify=df["RISCO"],
)

modelo = joblib.load(MODEL_PATH)
probabilidades = modelo.predict_proba(textos_teste)
predicoes = modelo.predict(textos_teste)

confiancas = probabilidades.max(axis=1)
posicao = confiancas.argmax()
indice = textos_teste.index[posicao]

texto = df.loc[indice, "ANON_TRANSCRICAO"]
probabilidade_risco = float(probabilidades[posicao][1])
classe = "RISCO" if int(predicoes[posicao]) == 1 else "SEM_RISCO"
confianca = float(confiancas[posicao])

print("ID_ORIGINAL:", df.loc[indice, "ID_MEETING"])
print("NPS_ORIGINAL:", df.loc[indice, "NOTA_NPS"])
print("CLASSE_MODELO:", classe)
print("PROBABILIDADE_RISCO:", round(probabilidade_risco, 4))
print("CONFIANCA_MODELO:", round(confianca, 4))
print("TRANSCRICAO_INICIO")
print(texto)
print("TRANSCRICAO_FIM")
