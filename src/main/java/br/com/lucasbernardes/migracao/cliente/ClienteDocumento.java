package br.com.lucasbernardes.migracao.cliente;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Documento da coleção {@code clientes} no MongoDB de destino.
 *
 * <p>As colunas de endereço, que na origem ficam soltas na tabela, viram um subdocumento.
 */
@Document(collection = ClienteDocumento.COLECAO)
public record ClienteDocumento(
        @Id Long id,
        String nome,
        String email,
        String telefone,
        LocalDate dataNascimento,
        Endereco endereco,
        Instant migradoEm) {

    public static final String COLECAO = "clientes";

    public record Endereco(String logradouro, String numero, String cidade, String estado, String cep) {
    }
}
