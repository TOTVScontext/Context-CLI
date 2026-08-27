import json
from pathlib import Path

import pandas as pd


ROOT = Path(__file__).resolve().parent.parent
CSV_PATH = ROOT / "data" / "reunioes_transcricoes_mockado.csv"
JSON_PATH = ROOT / "data" / "ANON_transcricao.json"


def read_json_lines(path):
    registros = []
    with path.open(encoding="utf-8") as arquivo:
        for numero_linha, linha in enumerate(arquivo, start=1):
            if linha.strip():
                try:
                    registros.append(json.loads(linha))
                except json.JSONDecodeError as erro:
                    print(f"Erro no JSON na linha {numero_linha}: {erro}")
                    raise
    return registros


csv = pd.read_csv(CSV_PATH, encoding="utf-8-sig")
json_df = pd.DataFrame(read_json_lines(JSON_PATH))

print("=== CSV DE TREINAMENTO ===")
print(f"linhas: {len(csv)}")
print(f"colunas: {list(csv.columns)}")
print(f"NPS nulo: {csv['NOTA_NPS'].isna().sum()}")
print(f"distribuicao NPS: {csv['NOTA_NPS'].value_counts().sort_index().to_dict()}")
print(f"tamanho medio da transcricao: {csv['ANON_TRANSCRICAO'].astype(str).str.len().mean():.2f}")

print("\n=== JSON DE INFERENCIA ===")
print(f"linhas: {len(json_df)}")
print(f"reunioes unicas: {json_df['ID_MEETING'].nunique()}")
print(f"linhas duplicadas: {json_df.duplicated().sum()}")
print(f"tamanho medio da transcricao: {json_df['ANON_TRANSCRICAO'].astype(str).str.len().mean():.2f}")

print("\n=== DECISAO DO ALVO ===")
print("O primeiro modelo usara NOTA_NPS <= 6 como classe de risco e NOTA_NPS >= 7 como classe sem risco.")
