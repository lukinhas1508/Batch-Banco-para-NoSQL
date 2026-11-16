package br.com.lucasbernardes.migracao.job;

import br.com.lucasbernardes.migracao.cliente.ClienteDocumento;
import br.com.lucasbernardes.migracao.config.DataSourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Confere se o destino terminou com a mesma quantidade de registros da origem; se não, o job falha. */
@Component
class ValidarMigracaoTasklet implements Tasklet {

    private static final Logger log = LoggerFactory.getLogger(ValidarMigracaoTasklet.class);
    private static final String CONTAR_CLIENTES = "SELECT COUNT(*) FROM clientes";

    private final JdbcTemplate origem;
    private final MongoOperations destino;

    ValidarMigracaoTasklet(@Qualifier(DataSourceConfig.ORIGEM) JdbcTemplate origem, MongoOperations destino) {
        this.origem = origem;
        this.destino = destino;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        long totalOrigem = contarClientesNaOrigem();
        long totalDestino = destino.count(new Query(), ClienteDocumento.class);

        if (totalOrigem != totalDestino) {
            throw new IllegalStateException(
                    "Migração incompleta: origem tem %d clientes e destino tem %d".formatted(totalOrigem, totalDestino));
        }
        log.info("Migração validada: {} clientes na origem e no destino", totalDestino);
        return RepeatStatus.FINISHED;
    }

    private long contarClientesNaOrigem() {
        Long total = origem.queryForObject(CONTAR_CLIENTES, Long.class);
        return total == null ? 0 : total;
    }
}
