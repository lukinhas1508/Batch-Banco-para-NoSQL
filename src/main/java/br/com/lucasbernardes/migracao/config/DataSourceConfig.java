package br.com.lucasbernardes.migracao.config;

import javax.sql.DataSource;

import br.com.lucasbernardes.migracao.config.MigracaoProperties.Origem;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

/**
 * Declara as conexões JDBC da aplicação.
 *
 * <p>O {@link Primary} é um H2 em memória que guarda apenas as tabelas de controle do
 * Spring Batch: assim o banco de origem é acessado somente para leitura e o destino
 * recebe somente os dados migrados. Como a carga é full, não há estado a preservar
 * entre execuções. O destino (MongoDB) é configurado pelo Spring Boot.
 */
@Configuration
public class DataSourceConfig {

    public static final String ORIGEM = "origem";

    @Bean
    @Primary
    DataSource controleDoBatchDataSource() {
        return new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .generateUniqueName(true)
                .build();
    }

    @Bean
    @Qualifier(ORIGEM)
    DataSource origemDataSource(MigracaoProperties properties) {
        Origem origem = properties.origem();
        HikariDataSource dataSource = DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .url(origem.url())
                .username(origem.usuario())
                .password(origem.senha())
                .build();
        dataSource.setPoolName("pool-origem");
        dataSource.setReadOnly(true);
        return dataSource;
    }

    @Bean
    @Qualifier(ORIGEM)
    JdbcTemplate origemJdbcTemplate(@Qualifier(ORIGEM) DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
