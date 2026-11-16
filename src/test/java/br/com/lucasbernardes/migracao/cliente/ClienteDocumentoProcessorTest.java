package br.com.lucasbernardes.migracao.cliente;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import br.com.lucasbernardes.migracao.cliente.ClienteDocumento.Endereco;
import org.junit.jupiter.api.Test;

class ClienteDocumentoProcessorTest {

    private static final Instant AGORA = Instant.parse("2026-01-15T10:30:00Z");

    private final ClienteDocumentoProcessor processor =
            new ClienteDocumentoProcessor(Clock.fixed(AGORA, ZoneOffset.UTC));

    @Test
    void converteLinhaRelacionalEmDocumentoComEnderecoAninhado() {
        Cliente cliente = new Cliente(
                7L, "Ana Souza Costa", "ana.costa7@exemplo.com", "(11) 91234-5678",
                LocalDate.of(1990, 5, 20), "Rua das Flores", "123", "São Paulo", "SP", "01310-100");

        ClienteDocumento documento = processor.process(cliente);

        assertThat(documento).isEqualTo(new ClienteDocumento(
                7L, "Ana Souza Costa", "ana.costa7@exemplo.com", "(11) 91234-5678",
                LocalDate.of(1990, 5, 20),
                new Endereco("Rua das Flores", "123", "São Paulo", "SP", "01310-100"),
                AGORA));
    }
}
