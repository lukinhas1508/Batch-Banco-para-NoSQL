# Batch Banco para NoSQL

[![CI](https://github.com/lukinhas1508/Batch-Banco-para-NoSQL/actions/workflows/ci.yml/badge.svg)](https://github.com/lukinhas1508/Batch-Banco-para-NoSQL/actions/workflows/ci.yml)

Job **Spring Batch** que faz a **carga full** da tabela `clientes` de um banco relacional (**PostgreSQL**) para uma coleção de um banco NoSQL (**MongoDB**). A cada execução, o destino é esvaziado e recebe uma cópia completa da origem, já convertida para o formato de documento.

## Tecnologias

- Java 21
- Spring Boot 3.5 / Spring Batch 5 / Spring Data MongoDB
- PostgreSQL 17 (origem) e MongoDB 8 (destino), via Docker Compose
- JUnit 5 + Testcontainers nos testes
- Maven

## Como o job funciona

```
migracaoClientesJob
 ├─ 1. limparDestinoStep     remove todos os documentos da coleção de destino
 ├─ 2. migrarClientesStep    lê da origem, converte e grava no destino, em chunks
 └─ 3. validarMigracaoStep   compara a quantidade de registros dos dois bancos
```

| Step | Tipo | O que faz |
|------|------|-----------|
| `limparDestinoStep` | Tasklet | Esvazia a coleção `clientes` para a carga full não gerar duplicidade. Os índices são preservados. |
| `migrarClientesStep` | Chunk | `JdbcCursorItemReader` lê a origem em streaming, `ClienteDocumentoProcessor` monta o documento e `MongoItemWriter` insere em lote (100 registros por chunk, configurável). |
| `validarMigracaoStep` | Tasklet | Se a contagem da origem e do destino divergir, o job termina como `FAILED`. |

Decisões de projeto:

- O banco de origem é acessado **somente para leitura**.
- As tabelas de controle do Spring Batch (`BATCH_*`) ficam em um **H2 em memória**, para não criar nada na origem nem misturar metadados com os dados do destino. Como a carga é full, não há estado a preservar entre execuções.
- Se o job falhar, o processo termina com código de saída diferente de zero.

## Modelo de dados

**Origem** — tabela `clientes` (PostgreSQL):

| Coluna | Tipo | Exemplo |
|--------|------|---------|
| `id` | `BIGINT` (PK) | `1` |
| `nome` | `VARCHAR(120)` | `Diego Barbosa Nascimento` |
| `email` | `VARCHAR(160)` (único) | `diego.nascimento1@exemplo.com` |
| `telefone` | `VARCHAR(20)` | `(61) 94658-7891` |
| `data_nascimento` | `DATE` | `1969-03-17` |
| `logradouro` | `VARCHAR(120)` | `Rua Dom Pedro II` |
| `numero` | `VARCHAR(10)` | `2244` |
| `cidade` | `VARCHAR(80)` | `Brasília` |
| `estado` | `CHAR(2)` | `DF` |
| `cep` | `CHAR(9)` | `70962-848` |

**Destino** — coleção `clientes` (MongoDB). As colunas de endereço viram um subdocumento:

```json
{
  "_id": 1,
  "nome": "Diego Barbosa Nascimento",
  "email": "diego.nascimento1@exemplo.com",
  "telefone": "(61) 94658-7891",
  "dataNascimento": { "$date": "1969-03-17T00:00:00Z" },
  "endereco": {
    "logradouro": "Rua Dom Pedro II",
    "numero": "2244",
    "cidade": "Brasília",
    "estado": "DF",
    "cep": "70962-848"
  },
  "migradoEm": { "$date": "2026-11-16T13:00:00Z" }
}
```

A origem já sobe com **50 clientes fictícios** (`src/main/resources/db/origem/02-dados.sql`). A coleção de destino é criada com índice único em `email` e índice em `endereco.estado` + `endereco.cidade` (`src/main/resources/db/destino/01-init.js`).

## Como executar

Pré-requisitos: Java 21, Maven e Docker.

```bash
# 1. Crie o arquivo de variáveis de ambiente e defina as senhas
cp .env.example .env

# 2. Suba os dois bancos (tabela, dados da origem e coleção de destino são criados sozinhos)
docker compose up -d

# 3. Execute a migração
mvn spring-boot:run
```

Conferindo o resultado (troque `SENHA` pela senha definida no `.env`):

```bash
docker exec -it banco-destino mongosh "mongodb://destino_user:SENHA@localhost:27017/destino?authSource=admin" \
  --eval "db.clientes.countDocuments()" --eval "db.clientes.findOne()"
```

Para recomeçar do zero (apaga os volumes dos bancos): `docker compose down -v`.

## Configuração

| Variável | Descrição |
|----------|-----------|
| `DB_ORIGEM_URL` / `DB_ORIGEM_USER` / `DB_ORIGEM_PASSWORD` | Conexão com o PostgreSQL de origem |
| `DB_DESTINO_HOST` / `DB_DESTINO_USER` / `DB_DESTINO_PASSWORD` | Conexão com o MongoDB de destino |
| `MIGRACAO_TAMANHOCHUNK` | Registros por chunk (padrão: `100`) |

As credenciais ficam apenas no `.env`, que está no `.gitignore`. Nenhuma senha é versionada.

## Testes

```bash
mvn test
```

Requer Docker em execução: os testes de integração sobem um MongoDB real com Testcontainers (a origem é um H2 em memória carregado com os mesmos scripts SQL) e verificam que:

- todos os clientes da origem chegam ao destino com os mesmos dados e o endereço aninhado;
- a data de nascimento é gravada como meia-noite em UTC, independente do fuso da máquina;
- registros antigos do destino são substituídos;
- executar o job duas vezes não duplica dados e reflete exclusões feitas na origem.

## Estrutura do projeto

```
src/main/java/br/com/lucasbernardes/migracao
├── MigracaoClientesApplication.java     ponto de entrada
├── cliente/
│   ├── Cliente.java                     linha da tabela de origem
│   ├── ClienteDocumento.java            documento gravado no destino
│   └── ClienteDocumentoProcessor.java   conversão de linha para documento
├── config/
│   ├── DataSourceConfig.java            conexões JDBC (origem e controle do batch)
│   ├── MongoConfig.java                 conversão de datas do MongoDB
│   └── MigracaoProperties.java          configurações do job
└── job/
    ├── MigracaoClientesJobConfig.java   job, steps, reader e writer
    ├── LimparDestinoTasklet.java
    └── ValidarMigracaoTasklet.java
src/main/resources/db                    scripts de criação dos dois bancos
```
