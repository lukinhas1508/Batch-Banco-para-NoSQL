package br.com.lucasbernardes.migracao;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MigracaoClientesApplication {

    /** Encerra o processo com código diferente de zero quando o job falha, para que agendadores percebam o erro. */
    public static void main(String[] args) {
        System.exit(SpringApplication.exit(SpringApplication.run(MigracaoClientesApplication.class, args)));
    }
}
