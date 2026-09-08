<div align="center">

<img src="./public/assets/img/TOTVScontext.png" width="350px" alt="TOTVS Context" />

![Status](https://img.shields.io/badge/status-In%20Development-yellow )
![Version](https://img.shields.io/badge/version-0.1.0-blue )

</div>


## Análise de reuniões com Java, Oracle, Data Science e voz local

O **Context-CLI** é uma aplicação de linha de comando desenvolvida em Java para analisar transcrições de reuniões, gerar métricas e insights de negócio, persistir os resultados no Oracle FIAP e oferecer um fluxo de entrada por teclado ou por voz. O projeto preserva a estrutura de domínio da versão original e amplia a solução com JDBC/DAO, integração com um modelo de Data Science em Python e reconhecimento local de fala para identificação do ID da reunião.

> **Escopo da entrega:** esta versão foi preparada para demonstração acadêmica no ambiente da FIAP, com IntelliJ IDEA, JDK 25, Python 3.11, driver Oracle JDBC e acesso ao Oracle remoto da FIAP.

---

## 1. Objetivo

O objetivo do projeto é transformar uma transcrição de reunião em informações úteis para uma equipe comercial e de atendimento. A aplicação identifica indicadores de produtividade, sentimento e resolução, gera regras de negócio relacionadas a churn, upsell, reclamações, orçamento, persona e confiança, salva o resultado no Oracle e permite gerar um relatório detalhado.

A versão final também permite que o usuário informe o ID da reunião digitando-o ou falando-o. No fluxo por voz, o áudio é gravado localmente, o modelo Whisper é executado localmente e o Java valida o ID reconhecido no arquivo `ANON_transcricao.json`. Depois da validação, a transcrição correspondente é carregada e segue o mesmo pipeline de análise usado pela entrada digitada.

## 2. Funcionalidades

| Funcionalidade | Descrição |
|---|---|
| Análise de domínio | Cria `Conversation` e `Analysis`, calcula métricas e produz insights. |
| Regras de negócio | O `InsightService` possui métodos explícitos para risco de churn, oportunidade de upsell, reclamação e budget. |
| Entrada por ID | O usuário pode digitar o ID ou escolher a entrada por voz. |
| Busca no JSON | O `JsonMeetingRepository` lê o JSON Lines e localiza a transcrição pelo ID. |
| Reconhecimento local | `AudioRecorder`, `SpeechToText` e `VoiceIdResolver` gravam e interpretam o ID sem enviar o áudio à OpenAI. |
| Integração Python | O Java chama `ai/predict.py` por `ProcessBuilder` e interpreta o JSON retornado pelo modelo. |
| Decisão híbrida | A aplicação combina o resultado do modelo Python com o fallback pelas regras Java. |
| Persistência Oracle | O resultado é salvo por JDBC usando DTO, DAO, `PreparedStatement` e CRUD. |
| Relatório | O usuário pode solicitar a geração de relatório DOCX após a análise. |

## 3. Arquitetura

A solução segue uma separação inspirada em DDD e no padrão JDBC/DAO apresentado nas aulas do professor Gilberto. O domínio não conhece detalhes do Oracle, do microfone ou do processo Python. Essas responsabilidades ficam em componentes próprios.

```text
br.com.totvs
├── domain
│   ├── Conversation.java
│   ├── Analysis.java
│   ├── Analyzer.java
│   ├── Insight.java
│   ├── RiskInsight.java
│   ├── BusinessInsight.java
│   └── InsightService.java
├── dto
│   └── Reuniao.java
├── dao
│   ├── ConnectionFactory.java
│   ├── ReuniaoDAO.java
│   └── ReuniaoDAOImpl.java
├── repository
│   └── JsonMeetingRepository.java
├── integration
│   ├── PythonModelClient.java
│   ├── PredicaoModelo.java
│   └── HybridRiskService.java
├── voice
│   ├── AudioRecorder.java
│   ├── SpeechToText.java
│   └── VoiceIdResolver.java
├── service
│   └── ReuniaoPersistenceService.java
├── infrastructure
│   └── ReportGenerator.java
└── main
    ├── Main.java
    └── classes de teste manual
```

O fluxo principal é:

```text
Usuário
   ↓
Main
   ├── ID digitado ───────────────┐
   └── ID falado                  │
          ↓                      │
   AudioRecorder                  │
          ↓                      │
   Whisper local                  │
          ↓                      │
   VoiceIdResolver ──────────────┘
          ↓
JsonMeetingRepository
          ↓
Conversation
          ↓
Analyzer + InsightService
          ↓
HybridRiskService ── PythonModelClient ── ai/predict.py
          ↓
Analysis + insights + decisão final
          ↓
ReuniaoPersistenceService
          ↓
ReuniaoDAO / ReuniaoDAOImpl / Oracle
          ↓
Exibição no terminal e relatório DOCX
```

## 4. Pré-requisitos

Para executar a versão demonstrável, é necessário utilizar o ambiente abaixo.

| Recurso | Versão ou configuração |
|---|---|
| Java | JDK 25, configurado no IntelliJ IDEA e no terminal |
| Projeto | Java SE, sem Spring Boot e sem Maven/Gradle |
| Banco | Oracle remoto da FIAP, acessado pelo Oracle SQL Developer ou pelo JDBC |
| Driver | `ojdbc17.jar`, salvo localmente em `lib/` e associado ao módulo do IntelliJ |
| Python | Python 3.11 |
| Bibliotecas Python | `pandas`, `scikit-learn`, `joblib` e `faster-whisper` |
| Microfone | Microfone reconhecido pelo sistema operacional, para o fluxo de voz |
| Dados | `reunioes_transcricoes_mockado.csv` e `ANON_transcricao.json` na pasta `data/` |

O projeto não exige uma chave da OpenAI para a funcionalidade de voz. O reconhecimento é executado pelo `faster-whisper` localmente. Na primeira execução, os pesos do modelo Whisper podem ser baixados pelo Hugging Face, exigindo internet e espaço em disco.

## 5. Configuração do projeto no IntelliJ

Clone o repositório e abra a pasta como um projeto Java. Configure o Project SDK e o Module SDK com o JDK 25. Mantenha a estrutura de pastas `src`, `lib`, `ai` e `data`.

Baixe o driver Oracle JDBC compatível, salve o arquivo como `lib/ojdbc17.jar` e associe o JAR às dependências do módulo no IntelliJ. O driver é uma dependência local e não deve ser adicionado ao Git.

Os arquivos de dados também são locais. Coloque os arquivos recebidos na seguinte estrutura:

```text
data/
├── reunioes_transcricoes_mockado.csv
└── ANON_transcricao.json
```

O arquivo JSON é lido como JSON Lines, ou seja, uma reunião por linha. O repositório busca o valor de `ID_MEETING` e retorna a transcrição de `ANON_TRANSCRICAO`.

## 6. Credenciais do Oracle

A classe `ConnectionFactory` já traz como padrão as credenciais do ambiente acadêmico da FIAP (usuário `rm564929`). Isso é suficiente para rodar o projeto sem nenhuma configuração adicional.

Caso seja necessário apontar para outro schema/usuário (por exemplo, em outra máquina ou ambiente), as credenciais podem ser sobrescritas pelas variáveis de ambiente `ORACLE_USER`, `ORACLE_PASSWORD` e `ORACLE_URL`, sem alterar o código-fonte:

```bash
export ORACLE_USER="seu_usuario_fiap"
export ORACLE_PASSWORD="sua_senha_fiap"
export ORACLE_URL="jdbc:oracle:thin:@oracle.fiap.com.br:1521:ORCL"
```

No IntelliJ, isso pode ser definido em **Run > Edit Configurations > Environment variables**. A URL padrão usada pela conexão é:

```text
jdbc:oracle:thin:@oracle.fiap.com.br:1521:ORCL
```

## 7. Modelo de banco Oracle

A tabela principal é `CONTEXT_REUNIAO`. O campo `TRANSCRICAO` utiliza `CLOB` porque as transcrições do JSON podem ser muito longas, ultrapassando o tamanho adequado para uma coluna textual curta.

### 7.1 Script de referência para uma instalação nova

Execute no Oracle SQL Developer com o usuário da FIAP:

```sql
CREATE TABLE CONTEXT_REUNIAO (
    ID_REUNIAO          VARCHAR2(50) PRIMARY KEY,
    TRANSCRICAO         CLOB NOT NULL,
    PARTICIPANTES       VARCHAR2(1000),
    ORIGEM_ENTRADA      VARCHAR2(20),
    DATA_ANALISE        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRED_RISCO          NUMBER(1),
    PRED_OPORTUNIDADE   NUMBER(1),
    SENTIMENTO          VARCHAR2(20),
    PRODUTOS_DETECTADOS VARCHAR2(1000),
    MODELO_ANALISE      VARCHAR2(100),
    CLASSE_MODELO       VARCHAR2(20),
    PROBABILIDADE_MODELO NUMBER(5,4),
    FONTE_DECISAO       VARCHAR2(40),
    CONSTRAINT CK_CONTEXT_ORIGEM
        CHECK (ORIGEM_ENTRADA IN ('TEXTO', 'VOZ', 'CSV', 'JSON'))
);
```

### 7.2 Atualização de uma tabela que já possua as dez colunas iniciais

Se a tabela já foi criada com as dez colunas originais, não execute novamente o `CREATE TABLE`. Execute apenas a migração abaixo, desde que as três colunas ainda não existam:

```sql
ALTER TABLE CONTEXT_REUNIAO ADD (
    CLASSE_MODELO        VARCHAR2(20),
    PROBABILIDADE_MODELO NUMBER(5,4),
    FONTE_DECISAO        VARCHAR2(40)
);
```

A tabela utilizada na demonstração contém as seguintes informações:

| Coluna | Tipo | Finalidade |
|---|---|---|
| `ID_REUNIAO` | `VARCHAR2(50)` | Identificador da reunião e chave primária. |
| `TRANSCRICAO` | `CLOB` | Texto completo recuperado do JSON. |
| `PARTICIPANTES` | `VARCHAR2(1000)` | Participantes conhecidos da conversa. |
| `ORIGEM_ENTRADA` | `VARCHAR2(20)` | Origem registrada, como `JSON` ou `VOZ`. |
| `DATA_ANALISE` | `TIMESTAMP` | Data e hora da análise. |
| `PRED_RISCO` | `NUMBER(1)` | Decisão final de risco após o híbrido. |
| `PRED_OPORTUNIDADE` | `NUMBER(1)` | Indicador de oportunidade. |
| `SENTIMENTO` | `VARCHAR2(20)` | Classificação textual do sentimento. |
| `PRODUTOS_DETECTADOS` | `VARCHAR2(1000)` | Produtos encontrados pela análise. |
| `MODELO_ANALISE` | `VARCHAR2(100)` | Identificação da estratégia de análise. |
| `CLASSE_MODELO` | `VARCHAR2(20)` | Classe retornada pelo modelo Python. |
| `PROBABILIDADE_MODELO` | `NUMBER(5,4)` | Probabilidade retornada pelo modelo Python. |
| `FONTE_DECISAO` | `VARCHAR2(40)` | `MODELO_PYTHON` ou `FALLBACK_REGRAS_JAVA`. |

A separação entre `PRED_RISCO` e `CLASSE_MODELO` é intencional. `CLASSE_MODELO` registra o que o experimento Python previu; `PRED_RISCO` registra a decisão efetivamente adotada pela aplicação. Assim, o fallback Java não sobrescreve a evidência da previsão original.

## 8. Instalação do ambiente Python

Verifique a versão do Python:

```bash
python --version
```

O projeto foi testado com Python 3.11. Instale as dependências:

```bash
python -m pip install pandas scikit-learn joblib faster-whisper
```

O modelo pode ser treinado novamente com:

```bash
python ai/train_model.py
```

A previsão isolada pode ser testada com:

```bash
python ai/predict.py "O cliente gostou do RM e quer comprar uma proposta."
```

A saída possui formato JSON semelhante a:

```json
{
  "modelo": "modelo_risco.joblib",
  "classe": "RISCO",
  "risco": true,
  "probabilidade_risco": 0.501,
  "texto_recebido": "O cliente gostou do RM e quer comprar uma proposta."
}
```

A transcrição local pode ser testada com um WAV gravado pelo Java:

```bash
python ai/transcribe_local.py audio/gravacao-teste.wav
```

O arquivo `ai/transcribe_local.py` usa `faster-whisper` com o modelo `base`, idioma português, execução em CPU e quantização `int8`. O áudio permanece no computador local durante esse processo.

## 9. Como executar o fluxo principal

Configure o JDK 25, o `ojdbc17.jar` no classpath e os arquivos de transcrição. Em seguida, execute a classe:

```text
br.com.totvs.main.Main
```

A partir desta versão, o CONTEXT CLI é **orientado a comandos**: não há mais um prompt inicial pedindo o caminho do arquivo. Depois do banner de boas-vindas, o terminal apresenta o prompt `context-cli ▸` e aguarda um comando no formato `/comando [argumento]`.

### 9.1 Comandos disponíveis

| Comando | Uso | Descrição |
|---|---|---|
| `/analyze <caminho>` | `/analyze downloads/transcricoes/meet08.json` | Carrega a transcrição, executa o pipeline de análise (NLP + modelo híbrido de risco), persiste o resultado no Oracle e, ao final, pergunta se deseja gerar o relatório em PDF. |
| `/history` | `/history` | Lista as últimas 30 análises persistidas (título, data, sentimento e indicador de risco). |
| `/read <caminho>` | `/read downloads/transcricoes/meet08.json` | Exibe o conteúdo da transcrição no terminal, linha por linha e numerada. |
| `/rename <caminho>` | `/rename downloads/transcricoes/meet08.json` | Localiza a análise associada ao arquivo (pelo identificador derivado do nome do arquivo) e permite alterar seu título de exibição. |
| `/delete <caminho>` | `/delete downloads/transcricoes/meet08.json` | Remove definitivamente do Oracle a análise associada ao arquivo, mediante confirmação. |
| `/help` | `/help` | Lista todos os comandos disponíveis. |
| `/exit` | `/exit` | Encerra o CONTEXT CLI. |

O identificador (`ID_REUNIAO`) de cada análise é derivado do nome do arquivo informado em `/analyze` (sem a extensão) — por isso `/rename` e `/delete` usam o mesmo caminho de arquivo para localizar o registro correspondente no banco.

### 9.2 Fluxo esperado do comando `/analyze`

1. O usuário digita `/analyze <caminho do arquivo>`.
2. O CLI valida o arquivo (existência, tamanho, codificação) e carrega a transcrição.
3. O `Analyzer` calcula as métricas e o `InsightService` gera os insights.
4. O `HybridRiskService` consulta o modelo Python e utiliza as regras Java quando a confiança é insuficiente.
5. O resultado é persistido no Oracle com `ReuniaoPersistenceService`.
6. O terminal mostra a análise completa (metadados, métricas, sinais e insights).
7. O CLI pergunta se deseja gerar o relatório em PDF e, em caso afirmativo, se deseja abri-lo automaticamente.
8. O prompt `context-cli ▸` volta a ficar disponível para o próximo comando.

## 10. Classes de teste

As classes de teste seguem o formato de classes `main` utilizado nas aulas e permitem demonstrar cada camada separadamente.

| Classe | O que valida |
|---|---|
| `TesteRegrasNegocio` | Instancia `Analysis` e testa cenários verdadeiros e falsos dos quatro métodos de negócio. |
| `TesteConexao` | Abre e fecha a conexão com o Oracle FIAP. |
| `TesteInsercaoReuniao` | Testa o `CREATE` do CRUD. |
| `TesteListarReuniao` | Testa a consulta e listagem dos registros. |
| `TesteAlterarReuniao` | Testa o `UPDATE` de um registro. |
| `TesteExcluirReuniao` | Testa o `DELETE` de um registro. |
| `TesteModeloPython` | Testa a ponte Java-Python e a decisão híbrida. |
| `TesteJsonRepository` | Busca uma transcrição pelo ID no JSON. |
| `TesteGravacao` | Testa a gravação local de um arquivo WAV. |
| `TesteTranscricao` | Testa a chamada Java para o Whisper local. |
| `TesteVoiceId` | Testa o fluxo voz → ID → validação no JSON. |

Os testes que acessam o Oracle exigem as variáveis `ORACLE_USER` e `ORACLE_PASS`. Os testes de voz exigem microfone, Python e `faster-whisper`. Os testes que usam o JSON exigem o arquivo em `data/ANON_transcricao.json`.

## 11. Experimento de Data Science

O experimento original utiliza TF-IDF e algoritmos de classificação supervisionada para gerar alvos derivados dos dados de reuniões. Os alvos são proxies: risco é aproximado por NPS menor ou igual a 6, oportunidade é relacionada ao tipo de recurso e sentimento é associado a faixas de NPS. Esses rótulos não comprovam, sozinhos, churn, intenção de compra ou emoção humana.

A integração do CLI utiliza um artefato de risco treinado em Python com TF-IDF e Regressão Logística. O Java não tenta reimplementar o scikit-learn. Ele chama o script Python, recebe um JSON, interpreta a classe e a probabilidade e registra tanto a previsão original quanto a decisão final.

| Medida do artefato de risco integrado | Resultado registrado |
|---|---:|
| Acurácia | 0,40 |
| Recall da classe `RISCO` | aproximadamente 0,215 |
| Baseline majoritário | aproximadamente 0,65 de acurácia |
| Interpretação | Prova de conceito; não é modelo de produção. |

Por causa desse desempenho, foi adotado um limiar de confiança de `0.65`. Quando o modelo não atinge esse limiar, a aplicação usa o fallback das regras Java, registra `FALLBACK_REGRAS_JAVA` em `FONTE_DECISAO` e mantém a saída do Python nas colunas próprias. Essa decisão torna o comportamento auditável e evita apresentar uma previsão fraca como verdade absoluta.

## 12. Dados utilizados

O CSV de treinamento possui 500 reuniões únicas, NPS válido de 0 a 10 e transcrições curtas. O JSON de inferência possui 1.174 linhas, 1.126 IDs únicos e 48 linhas duplicadas. As transcrições do JSON são muito maiores: a mediana é de aproximadamente 34.493 caracteres e o maior registro possui aproximadamente 185.626 caracteres.

Essas características justificam duas decisões técnicas. Primeiro, o JSON é lido linha a linha para não carregar todo o arquivo na memória de uma vez. Segundo, a transcrição é persistida em `CLOB`, evitando o limite de uma coluna curta de texto Oracle.

## 13. Regras de negócio

O `InsightService` separa quatro métodos de relevância exigidos na entrega:

```text
verificarRiscoChurn(Analysis analysis)
verificarOportunidadeUpsell(Analysis analysis)
verificarReclamacao(Analysis analysis)
verificarBudget(Analysis analysis)
```

Cada método retorna uma decisão booleana baseada nos dados de `Analysis`. O método `generate()` atua como orquestrador, chama as regras e cria objetos de insight. A hierarquia `Insight`, `RiskInsight` e `BusinessInsight` demonstra herança e polimorfismo por meio da sobrescrita da mensagem apresentada ao usuário.

## 14. Segurança e limitações

As credenciais do Oracle são lidas por variáveis de ambiente e não fazem parte do código versionado. O driver, os dados anonimizados e os arquivos WAV são dependências locais e ficam fora do Git por meio do `.gitignore`.

O reconhecimento de voz é local, mas pode confundir dígitos, especialmente quando o áudio possui ruído, pronúncia rápida ou palavras semelhantes. Para melhorar a chance de acerto, recomenda-se falar cada algarismo separadamente. Se o ID não existir no JSON, o sistema informa o problema e permite uma nova entrada digitada.

O modelo Python é um experimento acadêmico baseado em rótulos proxy e apresentou desempenho inferior ao baseline majoritário na execução registrada. Por isso, ele não deve ser descrito como inteligência autônoma confiável para decisões comerciais. O fallback Java, a separação das colunas e a revisão humana são partes intencionais da solução.

O fluxo de voz é por gravação curta e transcrição de arquivo. O projeto não promete streaming contínuo, atendimento conversacional em tempo real ou uma API externa de voz.

## 15. Evidências recomendadas para a entrega

Para a apresentação, recomenda-se capturar o terminal mostrando a seleção da opção `2 - Falar ID`, o estado `[GRAVANDO]`, o texto reconhecido, o ID validado, o início da transcrição, as métricas, os insights e a confirmação de persistência no Oracle. Também é importante mostrar no SQL Developer uma consulta que evidencie `ORIGEM_ENTRADA = 'VOZ'`, `CLASSE_MODELO`, `PROBABILIDADE_MODELO` e `FONTE_DECISAO`.

Uma consulta de verificação é:

```sql
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
```

## 16. Referências

[1]: https://github.com/TOTVScontext/Context-CLI "Repositório público do Context-CLI"

[2]: https://colab.research.google.com/github/TOTVScontext/DataScience/blob/main/context.ipynb "Notebook Data Science do projeto"

[3]: https://docs.oracle.com/en/java/javase/25/docs/api/java.desktop/javax/sound/sampled/class-use/TargetDataLine.html "Java SE 25 — TargetDataLine"

[4]: https://docs.oracle.com/en/java/javase/25/docs/api/java.sql/java/sql/DriverManager.html "Java SE 25 — DriverManager"

[5]: https://www.oracle.com/database/technologies/appdev/jdbc-downloads.html "Oracle — JDBC and UCP Downloads"

[6]: https://on1.fiap.com.br/updown/upload_fiap/alunos/apostilas/Aula27-Vetores%20de%20Objetos.pdf "FIAP — Aula 27: Vetores de Objetos"

[7]: https://on1.fiap.com.br/updown/upload_fiap/alunos/apostilas/Aula28-Vetores%20de%20Objetos%20e%20Cole%C3%A7%C3%B5es.pdf "FIAP — Aula 28: Vetores de Objetos e Coleções"

[8]: https://on1.fiap.com.br/updown/upload_fiap/alunos/apostilas/Aula29-Collections%20Framework.pdf "FIAP — Aula 29: Collections Framework"

[9]: https://on1.fiap.com.br/updown/upload_fiap/alunos/apostilas/Aula30-Stream%20API.pdf "FIAP — Aula 30: Stream API"

[10]: https://on1.fiap.com.br/updown/upload_fiap/alunos/apostilas/Aula31-Tratamento%20de%20Excecoes.pdf "FIAP — Aula 31: Tratamento de Exceções"

[11]: https://on1.fiap.com.br/updown/upload_fiap/alunos/apostilas/Aula32-Manipula%C3%A7%C3%A3o%20de%20Arquivos.pdf "FIAP — Aula 32: Manipulação de Arquivos"

[12]: https://on1.fiap.com.br/updown/upload_fiap/alunos/apostilas/Aula34-JDBC%20e%20Design%20Patterns%20DAO.pdf "FIAP — Aula 34: JDBC e Design Patterns DAO"
