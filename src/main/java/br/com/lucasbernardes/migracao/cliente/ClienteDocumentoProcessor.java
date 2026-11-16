package br.com.lucasbernardes.migracao.cliente;

import java.time.Clock;

import br.com.lucasbernardes.migracao.cliente.ClienteDocumento.Endereco;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

/** Converte a linha relacional no documento gravado no destino, registrando o momento da migração. */
@Component
public class ClienteDocumentoProcessor implements ItemProcessor<Cliente, ClienteDocumento> {

    private final Clock relogio;

    public ClienteDocumentoProcessor(Clock relogio) {
        this.relogio = relogio;
    }

    @Override
    public ClienteDocumento process(Cliente cliente) {
        Endereco endereco = new Endereco(
                cliente.logradouro(),
                cliente.numero(),
                cliente.cidade(),
                cliente.estado(),
                cliente.cep());

        return new ClienteDocumento(
                cliente.id(),
                cliente.nome(),
                cliente.email(),
                cliente.telefone(),
                cliente.dataNascimento(),
                endereco,
                relogio.instant());
    }
}
