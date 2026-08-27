import json
import sys
from pathlib import Path

import joblib


ROOT = Path(__file__).resolve().parent.parent
MODEL_PATH = ROOT / "ai" / "artifacts" / "modelo_risco.joblib"


def main():
    if len(sys.argv) > 1:
        texto = " ".join(sys.argv[1:])
    else:
        texto = sys.stdin.read().strip()

    if not texto:
        raise ValueError("Nenhuma transcricao foi informada")

    modelo = joblib.load(MODEL_PATH)
    previsao = int(modelo.predict([texto])[0])
    probabilidades = modelo.predict_proba([texto])[0]
    probabilidade_risco = float(probabilidades[1])

    resposta = {
        "modelo": "modelo_risco.joblib",
        "classe": "RISCO" if previsao == 1 else "SEM_RISCO",
        "risco": previsao == 1,
        "probabilidade_risco": round(probabilidade_risco, 4),
        "texto_recebido": texto,
    }

    print(json.dumps(resposta, ensure_ascii=False))


if __name__ == "__main__":
    main()
