package br.com.lucasbernardes.migracao.cliente;

import java.time.LocalDate;

/** Linha da tabela {@code clientes} do banco relacional de origem. */
public record Cliente(
        Long id,
        String nome,
        String email,
        String telefone,
        LocalDate dataNascimento,
        String logradouro,
        String numero,
        String cidade,
        String estado,
        String cep) {
}
