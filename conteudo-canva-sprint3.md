# Context-CLI — Conteúdo final para documento/Canva

> **Instrução de uso:** substituir os campos entre colchetes pelos nomes dos integrantes, RMs, turma, professor e instituição. O conteúdo abaixo está organizado em páginas/seções para ser colado no Canva ou adaptado para o documento final da Sprint 3.

---

## Página 1 — Capa

### CONTEXT-CLI

### Análise inteligente de reuniões com Java, Oracle, Data Science e voz local

**Sprint 3 — Entrega de Java/DDD/JDBC**

**Equipe:** [NOME DO INTEGRANTE 1 — RM] · [NOME DO INTEGRANTE 2 — RM] · [NOME DO INTEGRANTE 3 — RM]

**Turma:** [TURMA]

**Professor:** Gilberto Alexandre das Neves

**Instituição:** FIAP

**Data:** [DATA DA ENTREGA]

**Sugestão visual:** utilizar uma imagem de terminal com a análise da reunião ou uma composição com os elementos Java, banco Oracle, microfone e gráfico de análise. A capa deve ser limpa, com o nome do projeto em destaque e sem excesso de texto.

---

## Página 2 — Sumário

1. Objetivo e escopo
2. Problema e proposta de solução
3. Funcionalidades do Context-CLI
4. Arquitetura e organização do código
5. Regras de negócio e modelo de domínio
6. Persistência com Oracle, JDBC e DAO
7. Integração com Data Science
8. Reconhecimento de voz local
9. Protótipo e interação no terminal
10. Testes e evidências
11. Limitações e evolução futura
12. Conclusão

---

## Página 3 — Objetivo e escopo

O Context-CLI foi desenvolvido para transformar transcrições de reuniões em informações estruturadas para apoiar equipes comerciais e de atendimento. A aplicação recebe o ID de uma reunião, recupera sua transcrição, calcula métricas de produtividade, sentimento e resolução, identifica sinais de negócio e persiste os resultados no Oracle FIAP.

O escopo da Sprint 3 inclui a aplicação do padrão JDBC/DAO, a criação de um DTO para transporte dos dados, o CRUD da entidade `Reuniao`, a conexão com o Oracle remoto, os testes das regras de negócio e a manutenção da arquitetura de domínio já existente. Como diferenciais, foram integrados um experimento de classificação em Python e o reconhecimento local de voz para informar o ID da reunião.

| Dentro do escopo | Fora do escopo desta versão |
|---|---|
| CLI Java para análise de reuniões | Interface gráfica web ou mobile |
| Persistência JDBC no Oracle FIAP | Streaming contínuo de áudio |
| Busca de transcrição por ID no JSON | Decisão comercial totalmente automatizada |
| Modelo Python integrado ao Java | Treinamento do modelo dentro do Java |
| Voz local para identificação do ID | Envio obrigatório de áudio para API externa |
| Relatório DOCX | Serviço em produção com alta disponibilidade |

---

## Página 4 — Problema e proposta de solução

Reuniões comerciais e de atendimento possuem informações importantes, mas a leitura manual de transcrições extensas dificulta a identificação rápida de riscos, oportunidades e próximos passos. O projeto propõe uma aplicação de terminal que automatiza a primeira análise e registra os resultados para consulta posterior.

A solução combina três estratégias. O domínio Java aplica regras determinísticas e explicáveis. O experimento Python fornece uma previsão adicional de risco. O serviço híbrido escolhe a decisão final conforme um limiar de confiança e utiliza o fallback Java quando a previsão do modelo é insuficiente. Dessa forma, a aplicação mantém rastreabilidade entre a saída do modelo e a decisão efetivamente persistida.

> **Decisão arquitetural:** o modelo Python não substitui as regras de negócio. Ele é um componente complementar; o Java continua responsável pela orquestração, persistência, apresentação e fallback.

---

## Página 5 — Funcionalidades principais

| Funcionalidade | Como funciona |
|---|---|
| Entrada por teclado | O usuário digita um ID existente no arquivo JSON. |
| Entrada por voz | O usuário fala cada dígito do ID; o áudio é gravado em WAV e transcrito localmente. |
| Busca de reunião | O `JsonMeetingRepository` lê o JSON Lines e localiza a transcrição pelo ID. |
| Métricas | O `Analyzer` calcula produtividade, sentimento e resolução. |
| Regras de negócio | O `InsightService` avalia churn, upsell, reclamação e budget, além de sinais adicionais. |
| IA complementar | O Java chama `ai/predict.py` e interpreta a previsão de risco em JSON. |
| Fallback responsável | Uma previsão Python com confiança menor que `0.65` não decide sozinha; as regras Java são usadas. |
| Persistência | `ReuniaoPersistenceService` mapeia os resultados para o DTO e chama o DAO JDBC. |
| Relatório | O usuário pode gerar um documento DOCX ao final da análise. |

A aplicação preserva o mesmo pipeline para entrada digitada e entrada por voz. A única diferença é a origem do ID e o valor registrado em `ORIGEM_ENTRADA`, que pode ser `JSON` ou `VOZ` no fluxo demonstrado.

---

## Página 6 — Arquitetura da solução

A arquitetura mantém o domínio isolado das tecnologias externas. `Conversation`, `Analysis`, `Analyzer` e `InsightService` representam o núcleo da aplicação. `Reuniao`, `ConnectionFactory`, `ReuniaoDAO` e `ReuniaoDAOImpl` implementam o transporte e a persistência. Os adaptadores de Python, JSON e áudio ficam em camadas próprias.

```text
Entrada do usuário
       ↓
Main
       ├── ID digitado
       └── ID falado
              ↓
       AudioRecorder → SpeechToText → VoiceIdResolver
              ↓
       JsonMeetingRepository
              ↓
       Conversation
              ↓
       Analyzer → Analysis → InsightService
              ↓
       HybridRiskService → PythonModelClient → ai/predict.py
              ↓
       ReuniaoPersistenceService
              ↓
       DTO Reuniao → DAO JDBC → Oracle FIAP
              ↓
       Terminal e relatório DOCX
```

| Camada | Classes principais | Responsabilidade |
|---|---|---|
| Domínio | `Conversation`, `Analysis`, `Analyzer`, `InsightService` | Regras e conceitos do negócio. |
| DTO | `Reuniao` | Representação simples dos campos persistidos. |
| DAO | `ConnectionFactory`, `ReuniaoDAO`, `ReuniaoDAOImpl` | Conexão e operações CRUD no Oracle. |
| Repositório | `JsonMeetingRepository` | Leitura e busca de transcrições no JSON. |
| Integração | `PythonModelClient`, `PredicaoModelo`, `HybridRiskService` | Comunicação com Python e decisão híbrida. |
| Voz | `AudioRecorder`, `SpeechToText`, `VoiceIdResolver` | Gravação, transcrição e validação do ID. |
| Serviço | `ReuniaoPersistenceService` | Orquestração do mapeamento e da persistência. |
| Apresentação | `Main`, `ReportGenerator` | Interação no terminal e relatório. |

---

## Página 7 — Modelo de domínio e regras de negócio

O modelo de domínio utiliza `Conversation` para representar a reunião e `Analysis` para armazenar as métricas e indicadores produzidos pelo analisador. A classe abstrata `Insight` possui especializações como `RiskInsight` e `BusinessInsight`, demonstrando herança e polimorfismo por meio da sobrescrita da mensagem exibida.

O `InsightService` separa as regras de negócio em quatro métodos públicos e testáveis, conforme a exigência da rubrica:

```text
verificarRiscoChurn(Analysis analysis)
verificarOportunidadeUpsell(Analysis analysis)
verificarReclamacao(Analysis analysis)
verificarBudget(Analysis analysis)
```

O método `generate()` atua como orquestrador. Ele chama as regras, cria os objetos de insight e devolve uma lista organizada para exibição. Essa separação evita que a interface do terminal concentre as decisões de negócio.

| Elemento avaliado | Evidência no projeto |
|---|---|
| Encapsulamento | Atributos privados e acesso por métodos da entidade/DTO. |
| Herança | `RiskInsight` e `BusinessInsight` derivam de `Insight`. |
| Polimorfismo | Cada especialização sobrescreve `getMessage()`. |
| Coleções | Os insights são trabalhados como `List<Insight>`. |
| Regras explícitas | Quatro métodos públicos no `InsightService`. |
| Testabilidade | `TesteRegrasNegocio` cria cenários positivos e negativos. |

---

## Página 8 — Oracle, JDBC, DTO e DAO

A persistência utiliza o Oracle remoto da FIAP e segue a didática de JDBC e Design Patterns DAO. A `ConnectionFactory` centraliza a abertura e o fechamento da conexão. O DTO `Reuniao` possui os campos privados, construtor vazio, getters e setters. A interface `ReuniaoDAO` define as operações, enquanto `ReuniaoDAOImpl` implementa o SQL com `PreparedStatement`, `ResultSet` e tratamento de `SQLException`.

A tabela `CONTEXT_REUNIAO` possui chave primária textual e armazena a transcrição como `CLOB`, pois as reuniões do JSON podem possuir dezenas ou centenas de milhares de caracteres.

| Grupo de campos | Colunas |
|---|---|
| Identificação | `ID_REUNIAO`, `DATA_ANALISE` |
| Conteúdo | `TRANSCRICAO`, `PARTICIPANTES`, `PRODUTOS_DETECTADOS` |
| Origem e estratégia | `ORIGEM_ENTRADA`, `MODELO_ANALISE`, `FONTE_DECISAO` |
| Análise de negócio | `PRED_RISCO`, `PRED_OPORTUNIDADE`, `SENTIMENTO` |
| Evidência do Python | `CLASSE_MODELO`, `PROBABILIDADE_MODELO` |

O CRUD foi validado com classes de teste separadas para inserir, listar, alterar e excluir. O uso de SQL parametrizado reduz o acoplamento entre entrada do usuário e comando SQL e segue a abordagem de acesso a dados apresentada na Aula 34.

---

## Página 9 — Integração com Data Science

O trabalho de Data Science utiliza TF-IDF e classificadores supervisionados para analisar transcrições. Os alvos são derivados de campos disponíveis no dataset: risco é aproximado por NPS menor ou igual a 6, oportunidade é relacionada ao tipo de recurso e sentimento é associado a faixas de NPS. Portanto, esses rótulos são **proxies** e não representam confirmação direta de churn, intenção de compra ou emoção humana.

Na integração final, o Python permanece responsável pelo treinamento e pela inferência. O Java chama `ai/predict.py` por `ProcessBuilder`, recebe um JSON e interpreta classe, indicador de risco e probabilidade.

| Característica | Resultado documentado |
|---|---:|
| Registros CSV de treinamento | 500 reuniões únicas |
| Registros JSON de inferência | 1.174 linhas |
| IDs únicos no JSON | 1.126 |
| Linhas duplicadas no JSON | 48 |
| Acurácia do artefato de risco integrado | 0,40 |
| Recall aproximado da classe `RISCO` | 0,215 |
| Baseline majoritário | aproximadamente 0,65 de acurácia |

Como o desempenho não supera o baseline majoritário, o modelo é apresentado como prova de conceito. A aplicação adota um limiar de confiança de `0.65`. A previsão original fica registrada em `CLASSE_MODELO` e `PROBABILIDADE_MODELO`; a decisão adotada pelo sistema fica em `PRED_RISCO` e `FONTE_DECISAO`.

---

## Página 10 — Diferencial de reconhecimento de voz

O diferencial de voz foi implementado sem depender de uma API paga. O `AudioRecorder` utiliza o microfone local e grava um WAV de 16 kHz, 16 bits e mono. O `SpeechToText` chama `ai/transcribe_local.py`, que executa o modelo `faster-whisper` localmente. O `VoiceIdResolver` normaliza números reconhecidos, remove pontuação, aceita dígitos falados e valida o resultado no repositório JSON.

O fluxo demonstrado é:

```text
Usuário escolhe “Falar ID”
        ↓
[GRAVANDO]
        ↓
Whisper local transcreve os dígitos
        ↓
VoiceIdResolver normaliza o texto
        ↓
JsonMeetingRepository valida o ID
        ↓
Transcrição é recuperada do JSON
        ↓
Análise, persistência e relatório continuam normalmente
```

O áudio não é enviado à OpenAI. A primeira execução pode baixar o modelo Whisper do Hugging Face, mas a inferência posterior é feita localmente. Como qualquer sistema de reconhecimento de fala, o componente pode confundir dígitos em ambientes ruidosos; por isso, recomenda-se falar cada número separadamente e manter a opção de entrada digitada.

---

## Página 11 — Protótipo e interação

O protótipo é uma interface de terminal. A tela inicial solicita o ID da reunião e, em seguida, oferece as opções `1 - Digitar ID` e `2 - Falar ID`. Na entrada por voz, o terminal informa quando a gravação começou, apresenta o texto reconhecido e confirma se o ID existe no JSON.

Após a confirmação, a aplicação mostra o início da transcrição, aguarda o comando do usuário e exibe as métricas, os insights e o resumo da análise. Ao final, pergunta se o usuário deseja gerar o relatório detalhado.

### Capturas recomendadas para inserir

| Captura | O que deve aparecer |
|---|---|
| 1 | Menu com `1 - Digitar ID` e `2 - Falar ID`. |
| 2 | Estado `[GRAVANDO]` e instrução para falar o ID. |
| 3 | Texto reconhecido e mensagem `ID validado no JSON`. |
| 4 | Início da transcrição recuperada. |
| 5 | Métricas, insights e resumo no terminal. |
| 6 | SQL Developer mostrando o registro persistido e `ORIGEM_ENTRADA = 'VOZ'`. |

> **Legenda sugerida:** “Fluxo integrado de voz local: o usuário informa o ID por fala, a aplicação valida a reunião no JSON e reaproveita o mesmo pipeline de análise e persistência.”

---

## Página 12 — Testes e evidências

A validação foi realizada por classes `main`, seguindo o formato didático utilizado no projeto. Foram testadas as regras de negócio, a conexão Oracle, as quatro operações CRUD, a ponte Java-Python, a busca no JSON, a gravação WAV, a transcrição local e o fluxo completo voz → ID → transcrição → análise → Oracle.

| Teste | Resultado esperado |
|---|---|
| `TesteRegrasNegocio` | Casos verdadeiro e falso para os quatro métodos. |
| `TesteConexao` | Conexão aberta e fechada sem erro. |
| `TesteInsercaoReuniao` | Registro inserido no Oracle. |
| `TesteListarReuniao` | Registros retornados pelo `ResultSet`. |
| `TesteAlterarReuniao` | Registro atualizado com sucesso. |
| `TesteExcluirReuniao` | Registro removido com sucesso. |
| `TesteModeloPython` | JSON Python interpretado e fallback demonstrado. |
| `TesteJsonRepository` | ID localizado e transcrição recuperada. |
| `TesteGravacao` | WAV criado a partir do microfone. |
| `TesteTranscricao` | Whisper local transcrevendo o áudio. |
| `TesteVoiceId` | ID falado normalizado e validado no JSON. |
| `Main` integrado | Análise persistida com origem e decisão registradas. |

A evidência do banco pode ser obtida com a consulta abaixo:

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

---

## Página 13 — Limitações e evolução futura

A solução foi construída para uma demonstração acadêmica funcional. O modelo Python ainda é um experimento com rótulos proxy, desempenho inferior ao baseline e necessidade de revisão humana. O analisador Java utiliza heurísticas explicáveis; ele não substitui uma compreensão semântica completa de reuniões extensas.

O reconhecimento de voz depende da qualidade do microfone, da pronúncia dos dígitos e do ambiente. A implementação realiza gravação curta e transcrição de arquivo, não streaming contínuo. Os dados, o driver Oracle e os áudios permanecem locais e não são versionados.

| Limitação atual | Evolução possível |
|---|---|
| Modelo com desempenho baixo | Ampliar e melhorar o dataset e os rótulos. |
| Regras baseadas em heurísticas | Avaliar modelos linguísticos com validação humana. |
| Captura curta de voz | Implementar streaming em uma etapa futura. |
| Setup manual no IntelliJ | Criar build automatizado quando o escopo exigir. |
| JSON local | Criar um repositório de dados controlado ou API. |
| Relatório DOCX básico | Evoluir para templates e indicadores visuais. |

---

## Página 14 — Conclusão

O Context-CLI atende ao núcleo da Sprint 3 ao combinar modelo de domínio, quatro regras de negócio testáveis, conexão com usuário e senha por configuração local, DTO, DAO e CRUD funcional no Oracle FIAP. A solução também preserva a separação de responsabilidades ensinada nas aulas e aplica tratamento de arquivos, coleções, exceções e integração entre componentes.

Como diferenciais, o projeto reutiliza o experimento de Data Science sem esconder suas limitações e incorpora reconhecimento de voz local para que o usuário informe o ID da reunião falando. A decisão híbrida mantém o sistema explicável: o modelo Python contribui com uma previsão, enquanto o Java registra a origem e aplica o fallback quando necessário.

> **Resultado final:** o usuário pode informar uma reunião por teclado ou voz, recuperar a transcrição no JSON, analisar o conteúdo, visualizar os insights, persistir os resultados no Oracle e gerar um relatório no mesmo fluxo.

---

## Página 15 — Referências

[1] [Repositório Context-CLI](https://github.com/TOTVScontext/Context-CLI)

[2] [Notebook Data Science — context.ipynb](https://colab.research.google.com/github/TOTVScontext/DataScience/blob/main/context.ipynb)

[3] [Java SE 25 — TargetDataLine](https://docs.oracle.com/en/java/javase/25/docs/api/java.desktop/javax/sound/sampled/class-use/TargetDataLine.html)

[4] [Java SE 25 — DriverManager](https://docs.oracle.com/en/java/javase/25/docs/api/java.sql/java/sql/DriverManager.html)

[5] [Oracle — JDBC and UCP Downloads](https://www.oracle.com/database/technologies/appdev/jdbc-downloads.html)

[6] [FIAP — Aula 27: Vetores de Objetos](https://on1.fiap.com.br/updown/upload_fiap/alunos/apostilas/Aula27-Vetores%20de%20Objetos.pdf)

[7] [FIAP — Aula 28: Vetores de Objetos e Coleções](https://on1.fiap.com.br/updown/upload_fiap/alunos/apostilas/Aula28-Vetores%20de%20Objetos%20e%20Cole%C3%A7%C3%B5es.pdf)

[8] [FIAP — Aula 29: Collections Framework](https://on1.fiap.com.br/updown/upload_fiap/alunos/apostilas/Aula29-Collections%20Framework.pdf)

[9] [FIAP — Aula 30: Stream API](https://on1.fiap.com.br/updown/upload_fiap/alunos/apostilas/Aula30-Stream%20API.pdf)

[10] [FIAP — Aula 31: Tratamento de Exceções](https://on1.fiap.com.br/updown/upload_fiap/alunos/apostilas/Aula31-Tratamento%20de%20Excecoes.pdf)

[11] [FIAP — Aula 32: Manipulação de Arquivos](https://on1.fiap.com.br/updown/upload_fiap/alunos/apostilas/Aula32-Manipula%C3%A7%C3%A3o%20de%20Arquivos.pdf)

[12] [FIAP — Aula 34: JDBC e Design Patterns DAO](https://on1.fiap.com.br/updown/upload_fiap/alunos/apostilas/Aula34-JDBC%20e%20Design%20Patterns%20DAO.pdf)
