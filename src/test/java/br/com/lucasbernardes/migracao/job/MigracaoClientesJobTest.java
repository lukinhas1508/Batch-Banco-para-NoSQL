package br.com.lucasbernardes.migracao.job;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;

import javax.sql.DataSource;

import br.com.lucasbernardes.migracao.cliente.Cliente;
import br.com.lucasbernardes.migracao.cliente.ClienteDocumento;
import br.com.lucasbernardes.migracao.cliente.ClienteDocumento.Endereco;
import br.com.lucasbernardes.migracao.config.MigracaoProperties;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Executa o job completo: a origem é um H2 em memória carregado com os mesmos scripts
 * usados no PostgreSQL e o destino é um MongoDB real, em container.
 */
@SpringBatchTest
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class MigracaoClientesJobTest {

    @Container
    private static final MongoDBContainer MONGO = new MongoDBContainer("mongo:8.0");

    @DynamicPropertySource
    static void conectarAoMongoDoTeste(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", () -> MONGO.getReplicaSetUrl("destino"));
    }

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private MigracaoProperties properties;

    @Autowired
    private MongoOperations destino;

    private JdbcTemplate origem;

    @BeforeEach
    void recriarBancos() {
        DataSource origemComEscrita = new DriverManagerDataSource(
                properties.origem().url(), properties.origem().usuario(), properties.origem().senha());
        origem = new JdbcTemplate(origemComEscrita);
        origem.execute("DROP TABLE IF EXISTS clientes");

        ResourceDatabasePopulator scripts = new ResourceDatabasePopulator(
                new ClassPathResource("db/origem/01-schema.sql"),
                new ClassPathResource("db/origem/02-dados.sql"));
        scripts.setSqlScriptEncoding("UTF-8");
        scripts.execute(origemComEscrita);

        destino.remove(new Query(), ClienteDocumento.class);
    }

    @Test
    void migraTodosOsClientesDaOrigemParaODestino() throws Exception {
        JobExecution execucao = jobLauncherTestUtils.launchJob();

        assertThat(execucao.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        List<ClienteDocumento> esperados = clientesDaOrigem().stream()
                .map(MigracaoClientesJobTest::documentoEsperado)
                .toList();
        assertThat(esperados).hasSizeGreaterThan(properties.tamanhoChunk());
        assertThat(documentosDoDestino())
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("migradoEm")
                .containsExactlyElementsOf(esperados);
        assertThat(documentosDoDestino()).allSatisfy(documento -> assertThat(documento.migradoEm()).isNotNull());
    }

    @Test
    void gravaDataDeNascimentoComoMeiaNoiteEmUtc() throws Exception {
        Cliente primeiro = clientesDaOrigem().getFirst();

        jobLauncherTestUtils.launchJob();

        Document gravado = destino.getCollection(ClienteDocumento.COLECAO)
                .find(new Document("_id", primeiro.id()))
                .first();
        Date meiaNoiteEmUtc = Date.from(primeiro.dataNascimento().atStartOfDay(ZoneOffset.UTC).toInstant());
        assertThat(gravado).isNotNull();
        assertThat(gravado.getDate("dataNascimento")).isEqualTo(meiaNoiteEmUtc);
    }

    @Test
    void substituiOConteudoAnteriorDoDestino() throws Exception {
        long idQueNaoExisteNaOrigem = 9_999L;
        destino.insert(new ClienteDocumento(idQueNaoExisteNaOrigem, "Registro Antigo", "antigo@exemplo.com",
                "(11) 90000-0000", null, new Endereco("Rua Velha", "1", "São Paulo", "SP", "01000-000"), null));

        JobExecution execucao = jobLauncherTestUtils.launchJob();

        assertThat(execucao.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(destino.findById(idQueNaoExisteNaOrigem, ClienteDocumento.class)).isNull();
        assertThat(documentosDoDestino()).hasSameSizeAs(clientesDaOrigem());
    }

    @Test
    void executarDuasVezesNaoDuplicaERefleteExclusoesDaOrigem() throws Exception {
        jobLauncherTestUtils.launchJob();
        origem.update("DELETE FROM clientes WHERE id = 1");

        JobExecution segundaExecucao = jobLauncherTestUtils.launchJob();

        assertThat(segundaExecucao.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(destino.findById(1L, ClienteDocumento.class)).isNull();
        assertThat(documentosDoDestino()).hasSameSizeAs(clientesDaOrigem());
    }

    private List<Cliente> clientesDaOrigem() {
        return origem.query("SELECT * FROM clientes ORDER BY id", new DataClassRowMapper<>(Cliente.class));
    }

    private List<ClienteDocumento> documentosDoDestino() {
        return destino.find(new Query().with(Sort.by("id")), ClienteDocumento.class);
    }

    private static ClienteDocumento documentoEsperado(Cliente cliente) {
        Endereco endereco = new Endereco(
                cliente.logradouro(), cliente.numero(), cliente.cidade(), cliente.estado(), cliente.cep());
        return new ClienteDocumento(cliente.id(), cliente.nome(), cliente.email(), cliente.telefone(),
                cliente.dataNascimento(), endereco, null);
    }
}
