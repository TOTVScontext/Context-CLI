# Context-CLI

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

As credenciais reais não ficam gravadas no código nem no repositório. A classe `ConnectionFactory` lê as variáveis de ambiente `ORACLE_USER` e `ORACLE_PASS`.

No IntelliJ, abra **Run > Edit Configurations**, selecione a configuração da classe que será executada e preencha o campo **Environment variables** com valores locais:

```text
ORACLE_USER=seu_usuario_fiap;ORACLE_PASS=sua_senha_fiap
```

No Git Bash, a configuração equivalente para a sessão atual é:

```bash
export ORACLE_USER="seu_usuario_fiap"
export ORACLE_PASS="sua_senha_fiap"
```

Nunca substitua esses exemplos pela senha em um arquivo versionado e nunca publique a senha no GitHub. A URL usada pela conexão é:

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

Configure o JDK 25, o `ojdbc17.jar`, as variáveis de ambiente Oracle e os arquivos de dados. Em seguida, execute a classe:

### 9.1 Cenários de demonstração

Para evidenciar que a aplicação consulta o JSON e não inventa uma reunião, recomenda-se executar primeiro um ID inexistente pela opção `1 - Digitar ID`:

```text
ID informado: 0000000
Resultado esperado: Nenhuma reunião encontrada para o ID: 0000000
```

Em seguida, execute dois IDs reais existentes na base de transcrições. Os exemplos abaixo foram separados para a demonstração:

| Tipo de teste | ID | Resultado esperado |
|---|---|---|
| ID inexistente | `0000000` | O sistema rejeita o valor e não cria uma `Conversation`. |
| ID real | `989351` | O sistema encontra a transcrição no `ANON_transcricao.json`; esse ID já foi validado no fluxo de voz. |
| ID real | `1027294` | O sistema encontra a transcrição no `ANON_transcricao.json`; esse ID foi confirmado na base enviada. |

Para a demonstração por voz, escolha `2 - Falar ID` e fale cada algarismo separadamente. O ID `989351` já foi validado anteriormente no fluxo voz → Whisper local → JSON. Se o registro já tiver sido persistido no Oracle, não o reutilize na mesma tabela, pois `ID_REUNIAO` é chave primária e uma nova análise poderá gerar `ORA-00001`. Nesse caso, use `1027294`, desde que ainda não esteja persistido no seu Oracle. O ID `989351` já foi utilizado como evidência de voz no ambiente do grupo.

O resultado esperado para um ID real é a mensagem `Transcrição encontrada`, seguida do início do texto e da solicitação para pressionar Enter antes da análise. O resultado esperado para o ID inexistente é uma mensagem de reunião não encontrada, sem transcrição inventada e sem persistência no Oracle.

```text
br.com.totvs.main.Main
```

O fluxo esperado é:

1. A aplicação pergunta como a reunião será informada.
2. Na opção `1`, o usuário digita um ID existente no JSON.
3. Na opção `2`, o usuário fala o ID, um dígito por vez, e pressiona Enter para finalizar a gravação.
4. O Whisper local transcreve o áudio.
5. O `VoiceIdResolver` normaliza números, pontuação e números escritos por extenso.
6. O Java valida o ID no `ANON_transcricao.json`.
7. A transcrição encontrada é mostrada parcialmente no terminal.
8. O `Analyzer` calcula as métricas e o `InsightService` gera os insights.
9. O `HybridRiskService` consulta o modelo Python e utiliza as regras Java quando a confiança é insuficiente.
10. O resultado é persistido no Oracle com `ReuniaoPersistenceService`.
11. O terminal mostra a análise e oferece a geração do relatório DOCX.

O fluxo por voz é uma captura curta de arquivo WAV. Ele não é uma conversa contínua, não implementa streaming e não envia o áudio à OpenAI.

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
