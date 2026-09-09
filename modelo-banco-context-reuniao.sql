/*
   Context-CLI - Modelo físico Oracle
   Tabela principal da análise de reuniões.
*/

/* ================================================================
   1. Criação completa para uma instalação nova
   ================================================================ */
CREATE TABLE CONTEXT_REUNIAO (
    ID_REUNIAO           VARCHAR2(50) PRIMARY KEY,
    TITULO                VARCHAR2(200),
    TRANSCRICAO          CLOB NOT NULL,
    PARTICIPANTES        VARCHAR2(1000),
    ORIGEM_ENTRADA       VARCHAR2(20),
    DATA_ANALISE         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRED_RISCO           NUMBER(1),
    PRED_OPORTUNIDADE    NUMBER(1),
    SENTIMENTO            VARCHAR2(20),
    PRODUTOS_DETECTADOS  VARCHAR2(1000),
    MODELO_ANALISE       VARCHAR2(100),
    CLASSE_MODELO        VARCHAR2(20),
    PROBABILIDADE_MODELO NUMBER(5,4),
    FONTE_DECISAO        VARCHAR2(40),
);

/* ================================================================
   2. Migração para tabelas já existentes
   ================================================================ */
-- Executar somente se a coluna ainda não existir:
-- ALTER TABLE CONTEXT_REUNIAO ADD (TITULO VARCHAR2(200));
-- UPDATE CONTEXT_REUNIAO SET TITULO = ID_REUNIAO WHERE TITULO IS NULL;

/* ================================================================
   3. Consultas de verificação
   ================================================================ */
SELECT
    ID_REUNIAO,
    TITULO,
    ORIGEM_ENTRADA,
    DBMS_LOB.GETLENGTH(TRANSCRICAO) AS TAMANHO_TRANSCRICAO,
    PRED_RISCO,
    PRED_OPORTUNIDADE,
    SENTIMENTO,
    CLASSE_MODELO,
    PROBABILIDADE_MODELO,
    FONTE_DECISAO,
    DATA_ANALISE
FROM CONTEXT_REUNIAO
ORDER BY DATA_ANALISE DESC;

/* Últimas 30 análises (usado pelo comando /history) */
SELECT ID_REUNIAO, TITULO, DATA_ANALISE, SENTIMENTO, PRED_RISCO, PRED_OPORTUNIDADE
FROM CONTEXT_REUNIAO
ORDER BY DATA_ANALISE DESC
FETCH FIRST 30 ROWS ONLY;

/* ================================================================
   4. Dicionário resumido
   ================================================================

   ID_REUNIAO            Chave primária; derivado do nome do arquivo de transcrição.
   TITULO                Título de exibição da análise (editável via /rename).
   TRANSCRICAO           Texto completo; CLOB para reuniões extensas.
   PARTICIPANTES         Lista textual de participantes.
   ORIGEM_ENTRADA        Origem controlada: TEXTO, VOZ, CSV ou JSON.
   DATA_ANALISE           Data/hora da persistência/análise.
   PRED_RISCO            Decisão final após o serviço híbrido.
   PRED_OPORTUNIDADE     Resultado de oportunidade de negócio.
   SENTIMENTO            Sentimento calculado no domínio Java.
   PRODUTOS_DETECTADOS   Produtos identificados na transcrição.
   MODELO_ANALISE        Identificação da estratégia utilizada.
   CLASSE_MODELO         Classe original retornada pelo Python.
   PROBABILIDADE_MODELO  Probabilidade original retornada pelo Python.
   FONTE_DECISAO         MODELO_PYTHON ou FALLBACK_REGRAS_JAVA.
*/
