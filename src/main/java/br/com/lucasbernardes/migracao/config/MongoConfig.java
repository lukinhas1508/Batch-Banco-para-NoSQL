package br.com.lucasbernardes.migracao.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions.MongoConverterConfigurationAdapter;

@Configuration
public class MongoConfig {

    /**
     * Grava {@code LocalDate} como meia-noite em UTC. Sem isso a conversão usaria o fuso
     * da máquina e a mesma data de nascimento poderia ser salva com dias diferentes.
     */
    @Bean
    MongoCustomConversions mongoCustomConversions() {
        return MongoCustomConversions.create(MongoConverterConfigurationAdapter::useNativeDriverJavaTimeCodecs);
    }

    @Bean
    Clock relogio() {
        return Clock.systemUTC();
    }
}
