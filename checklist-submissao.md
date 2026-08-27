# Checklist final de submissão — Context-CLI

## 1. Arquivos que devem entrar no repositório

Substituir o `README.md` pelo README final e copiar para `docs/` os arquivos `context-cli-uml.mmd`, `modelo-banco-context-reuniao.sql` e `conteudo-canva-sprint3.md`. A imagem `context-cli-uml.png` pode ser mantida como evidência visual do UML.

## 2. Arquivos que devem permanecer fora do Git

Não adicionar `data/`, `audio/`, `lib/ojdbc17.jar`, `.idea/`, senhas, tokens ou arquivos locais de configuração. Os dados e o driver devem ser explicados no README, mas não precisam ser publicados no repositório.

## 3. Revisão dos documentos no computador

Confirmar que o README contém os nomes reais da equipe, RMs, turma, professor e data. Conferir também se o nome da tabela e as 13 colunas estão iguais ao banco utilizado:

```text
CONTEXT_REUNIAO
ID_REUNIAO
TRANSCRICAO
PARTICIPANTES
ORIGEM_ENTRADA
DATA_ANALISE
PRED_RISCO
PRED_OPORTUNIDADE
SENTIMENTO
PRODUTOS_DETECTADOS
MODELO_ANALISE
CLASSE_MODELO
PROBABILIDADE_MODELO
FONTE_DECISAO
```

## 4. Evidência recomendada no Oracle

Executar no SQL Developer:

```sql
SELECT
    ID_REUNIAO,
    ORIGEM_ENTRADA,
    DBMS_LOB.GETLENGTH(TRANSCRICAO) AS TAMANHO_TRANSCRICAO,
    PRED_RISCO,
    CLASSE_MODELO,
    PROBABILIDADE_MODELO,
    FONTE_DECISAO,
    DATA_ANALISE
FROM CONTEXT_REUNIAO
ORDER BY DATA_ANALISE DESC;
```

Para uma evidência específica de voz:

```sql
SELECT
    ID_REUNIAO,
    ORIGEM_ENTRADA,
    CLASSE_MODELO,
    PROBABILIDADE_MODELO,
    FONTE_DECISAO,
    DBMS_LOB.GETLENGTH(TRANSCRICAO) AS TAMANHO_TRANSCRICAO
FROM CONTEXT_REUNIAO
WHERE ORIGEM_ENTRADA = 'VOZ'
ORDER BY DATA_ANALISE DESC;
```

Não reutilizar um ID já persistido para uma nova demonstração, pois `ID_REUNIAO` é chave primária e pode gerar `ORA-00001`.

## 5. Revisão segura antes do commit documental

Depois de substituir o README e copiar os documentos, executar os comandos abaixo. Não usar `git add .`.

```bash
git status --short
git diff --check
git diff -- README.md docs/
```

Se a revisão estiver correta, preparar somente os documentos:

```bash
git add README.md \
  docs/context-cli-uml.mmd \
  docs/context-cli-uml.png \
  docs/modelo-banco-context-reuniao.sql \
  docs/conteudo-canva-sprint3.md \
  docs/checklist-submissao.md
```

Verificar o stage antes de criar o commit:

```bash
git diff --cached --check
git diff --cached --name-only
git status --short
```

A lista do stage deve conter somente os documentos acima. Os arquivos `.idea` devem continuar com espaço na primeira coluna, portanto fora do stage.

## 6. Commit documental

Somente depois da revisão anterior:

```bash
git commit -m "docs: atualiza documentação da Sprint 3"
```

Depois confirmar:

```bash
git status --short
git log -5 --oneline
```

O `git push` deve ser feito apenas depois de revisar o conteúdo no GitHub e confirmar que nenhuma credencial ou arquivo local foi incluído.

## 7. Correspondência com a rubrica

| Critério | Evidência para apresentar |
|---|---|
| Model | Classes `Conversation`, `Analysis`, `Insight`, `RiskInsight`, `BusinessInsight`, `Analyzer` e `InsightService`. |
| Quatro regras | `verificarRiscoChurn`, `verificarOportunidadeUpsell`, `verificarReclamacao` e `verificarBudget`. |
| Classe de teste | `TesteRegrasNegocio` com cenários verdadeiros e falsos. |
| Conexão | `ConnectionFactory`, Oracle FIAP e variáveis locais `ORACLE_USER`/`ORACLE_PASS`. |
| CRUD | `ReuniaoDAO`, `ReuniaoDAOImpl` e classes de teste de inserir, listar, alterar e excluir. |
| Documento final | Capa, sumário, objetivo/escopo, funcionalidades, protótipo, banco e UML. |
| Diferencial | Modelo Python com fallback e fluxo voz → ID → JSON → análise → Oracle. |
