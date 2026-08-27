/*
   Context-CLI - Modelo físico Oracle
   Tabela principal da análise de reuniões.

   ATENÇÃO:
   - Execute o CREATE TABLE somente em um schema em que a tabela ainda não exista.
   - Se CONTEXT_REUNIAO já possuir as dez colunas iniciais, use apenas a migração
     indicada na seção 2.
   - Não executar DROP TABLE em ambiente da FIAP sem autorização.
*/

/* ================================================================
   1. Criação completa para uma instalação nova
   ================================================================ */
CREATE TABLE CONTEXT_REUNIAO (
    ID_REUNIAO           VARCHAR2(50) PRIMARY KEY,
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
    CONSTRAINT CK_CONTEXT_ORIGEM
        CHECK (ORIGEM_ENTRADA IN ('TEXTO', 'VOZ', 'CSV', 'JSON'))
);

/* ================================================================
   2. Migração para a tabela que já possua as dez colunas iniciais
   ================================================================ */
-- Executar somente se as colunas ainda não existirem:
-- ALTER TABLE CONTEXT_REUNIAO ADD (
--     CLASSE_MODELO        VARCHAR2(20),
--     PROBABILIDADE_MODELO NUMBER(5,4),
--     FONTE_DECISAO        VARCHAR2(40)
-- );

/* ================================================================
   3. Consultas de verificação
   ================================================================ */
SELECT
    ID_REUNIAO,
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

/* Evidência específica do fluxo de voz. */
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

/* ================================================================
   4. Dicionário resumido
   ================================================================

   ID_REUNIAO            Chave primária e ID vindo do JSON ou da entrada.
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
