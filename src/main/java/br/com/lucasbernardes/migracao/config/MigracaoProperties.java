package br.com.lucasbernardes.migracao.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param tamanhoChunk quantidade de registros lidos e gravados por transação
 * @param origem       conexão com o banco relacional de origem
 */
@ConfigurationProperties("migracao")
public record MigracaoProperties(@DefaultValue("100") int tamanhoChunk, Origem origem) {

    public MigracaoProperties {
        if (tamanhoChunk <= 0) {
            throw new IllegalArgumentException("migracao.tamanho-chunk deve ser maior que zero");
        }
        if (origem == null) {
            throw new IllegalArgumentException("migracao.origem deve ser configurado");
        }
    }

    public record Origem(String url, String usuario, @DefaultValue("") String senha) {

        public Origem {
            if (url == null || url.isBlank()) {
                throw new IllegalArgumentException("migracao.origem.url deve ser informada");
            }
        }
    }
}
