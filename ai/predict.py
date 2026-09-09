"""Inferencia isolada do modelo de reunioes (Context-CLI).

Chamado pelo Java via PythonModelClient/ProcessBuilder, recebendo apenas o
texto da transcricao como argumento. Mantem os campos "classe", "risco" e
"probabilidade_risco" pelos quais PredicaoModelo.fromJson faz o parse, e
adiciona campos novos (ignorados pelo parser Java atual, mas uteis para
depuracao, relatorios e evolucao futura da integracao).
"""

import json
import sys
from pathlib import Path

import joblib


ROOT = Path(__file__).resolve().parent.parent
MODEL_PATH = ROOT / "ai" / "artifacts" / "modelo_risco.joblib"

CLASSES = ["DETRATOR", "NEUTRO", "PROMOTOR"]


def main():
    if len(sys.argv) > 1:
        texto = " ".join(sys.argv[1:])
    else:
        texto = sys.stdin.read().strip()

    if not texto:
        raise ValueError("Nenhuma transcricao foi informada")

    modelo = joblib.load(MODEL_PATH)
    probabilidades = modelo.predict_proba([texto])[0]
    indice_previsto = int(probabilidades.argmax())
    classe = CLASSES[indice_previsto]

    probabilidade_risco = float(probabilidades[CLASSES.index("DETRATOR")])
    probabilidade_neutro = float(probabilidades[CLASSES.index("NEUTRO")])
    probabilidade_oportunidade = float(probabilidades[CLASSES.index("PROMOTOR")])
    confianca = float(probabilidades[indice_previsto])

    resposta = {
        "modelo": "modelo_risco.joblib",
        "classe": classe,
        "risco": classe == "DETRATOR",
        "probabilidade_risco": round(probabilidade_risco, 4),
        "probabilidade_neutro": round(probabilidade_neutro, 4),
        "probabilidade_oportunidade": round(probabilidade_oportunidade, 4),
        "confianca_modelo": round(confianca, 4),
        "texto_recebido": texto,
    }

    print(json.dumps(resposta, ensure_ascii=False))


if __name__ == "__main__":
    main()
