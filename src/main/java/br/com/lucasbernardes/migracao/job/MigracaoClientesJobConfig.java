package br.com.lucasbernardes.migracao.job;

import javax.sql.DataSource;

import br.com.lucasbernardes.migracao.cliente.Cliente;
import br.com.lucasbernardes.migracao.cliente.ClienteDocumento;
import br.com.lucasbernardes.migracao.cliente.ClienteDocumentoProcessor;
import br.com.lucasbernardes.migracao.config.DataSourceConfig;
import br.com.lucasbernardes.migracao.config.MigracaoProperties;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.data.MongoItemWriter;
import org.springframework.batch.item.data.builder.MongoItemWriterBuilder;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Job de carga full da tabela {@code clientes} para a coleção {@code clientes}:
 * limpa o destino, copia todos os registros da origem e valida as quantidades.
 */
@Configuration
public class MigracaoClientesJobConfig {

    public static final String NOME_DO_JOB = "migracaoClientesJob";

    private static final String SELECIONAR_CLIENTES = """
            SELECT id, nome, email, telefone, data_nascimento, logradouro, numero, cidade, estado, cep
              FROM clientes
             ORDER BY id
            """;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    MigracaoClientesJobConfig(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
    }

    @Bean
    Job migracaoClientesJob(Step limparDestinoStep, Step migrarClientesStep, Step validarMigracaoStep) {
        return new JobBuilder(NOME_DO_JOB, jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(limparDestinoStep)
                .next(migrarClientesStep)
                .next(validarMigracaoStep)
                .build();
    }

    @Bean
    Step limparDestinoStep(LimparDestinoTasklet tasklet) {
        return new StepBuilder("limparDestinoStep", jobRepository)
                .tasklet(tasklet, transactionManager)
                .build();
    }

    @Bean
    Step migrarClientesStep(JdbcCursorItemReader<Cliente> clienteReader,
                            ClienteDocumentoProcessor clienteDocumentoProcessor,
                            MongoItemWriter<ClienteDocumento> clienteDocumentoWriter,
                            MigracaoProperties properties) {
        return new StepBuilder("migrarClientesStep", jobRepository)
                .<Cliente, ClienteDocumento>chunk(properties.tamanhoChunk(), transactionManager)
                .reader(clienteReader)
                .processor(clienteDocumentoProcessor)
                .writer(clienteDocumentoWriter)
                .build();
    }

    @Bean
    Step validarMigracaoStep(ValidarMigracaoTasklet tasklet) {
        return new StepBuilder("validarMigracaoStep", jobRepository)
                .tasklet(tasklet, transactionManager)
                .build();
    }

    @Bean
    JdbcCursorItemReader<Cliente> clienteReader(@Qualifier(DataSourceConfig.ORIGEM) DataSource origem,
                                                MigracaoProperties properties) {
        return new JdbcCursorItemReaderBuilder<Cliente>()
                .name("clienteReader")
                .dataSource(origem)
                .sql(SELECIONAR_CLIENTES)
                .rowMapper(new DataClassRowMapper<>(Cliente.class))
                .fetchSize(properties.tamanhoChunk())
                .build();
    }

    /** Usa INSERT em vez de upsert porque o destino acabou de ser esvaziado: um _id repetido deve ser erro. */
    @Bean
    MongoItemWriter<ClienteDocumento> clienteDocumentoWriter(MongoOperations destino) {
        return new MongoItemWriterBuilder<ClienteDocumento>()
                .template(destino)
                .collection(ClienteDocumento.COLECAO)
                .mode(MongoItemWriter.Mode.INSERT)
                .build();
    }
}
