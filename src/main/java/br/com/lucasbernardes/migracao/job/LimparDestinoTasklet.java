package br.com.lucasbernardes.migracao.job;

import br.com.lucasbernardes.migracao.cliente.ClienteDocumento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

/**
 * Esvazia a coleção de destino: por ser carga full, cada execução substitui todo o conteúdo anterior.
 * Remove os documentos em vez de apagar a coleção para preservar os índices.
 */
@Component
class LimparDestinoTasklet implements Tasklet {

    private static final Logger log = LoggerFactory.getLogger(LimparDestinoTasklet.class);

    private final MongoOperations destino;

    LimparDestinoTasklet(MongoOperations destino) {
        this.destino = destino;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        long removidos = destino.remove(new Query(), ClienteDocumento.class).getDeletedCount();
        log.info("Coleção de destino esvaziada para receber a carga full ({} documentos removidos)", removidos);
        return RepeatStatus.FINISHED;
    }
}
