package com.parameter_store.demo;

import com.parameter_store.demo.config.AppProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.ConfigurableEnvironment;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class DemoApplication {

    private final AppProperties appProperties;
    private final ConfigurableEnvironment environment;

    /**
     * Estas propiedades NO existen como parámetros individuales en AWS Parameter Store.
     * Están dentro del JSON del parámetro "micro_yaml". El {@code JsonPropertyFlattener}
     * las aplana automáticamente para que queden accesibles con @Value.
     *
     * Esto demuestra que cualquier librería de terceros (ej: Recaptcha) que espere
     * propiedades planas en el Environment las encontrará sin modificación.
     */
    @Value("${app.micro_yaml.browsers.firefox.name:N/A}")
    private String firefoxName;

    @Value("${app.micro_yaml.browsers.firefox.releases.1.status:N/A}")
    private String firefoxReleaseStatus;

    @Value("${app.micro_yaml.browsers.firefox.releases.1.engine:N/A}")
    private String firefoxReleaseEngine;

    public DemoApplication(AppProperties appProperties, ConfigurableEnvironment environment) {
        this.appProperties = appProperties;
        this.environment = environment;
    }

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    @Bean
    public CommandLineRunner printParameterStoreValues() {
        return args -> {
            System.out.println("==================================================");
            System.out.println("✓ Backend iniciado correctamente");
            System.out.println("==================================================");
            System.out.println("Valores cargados desde AWS Parameter Store:");
            System.out.println("  - app.name: " + appProperties.getName());
            System.out.println("  - app.description: " + appProperties.getDescription());
            System.out.println("  - app.secreto: " + appProperties.getSecreto());
            System.out.println("==================================================");

            System.out.println("Valores aplanados por JsonPropertyFlattener (accesibles con @Value):");
            System.out.println("  - @Value app.micro_yaml.browsers.firefox.name: " + firefoxName);
            System.out.println("  - @Value app.micro_yaml.browsers.firefox.releases.1.status: " + firefoxReleaseStatus);
            System.out.println("  - @Value app.micro_yaml.browsers.firefox.releases.1.engine: " + firefoxReleaseEngine);
            System.out.println("==================================================");

            System.out.println("Verificación directa desde Environment (como lo haría una librería de terceros):");
            System.out.println("  - environment.getProperty(\"app.micro_yaml.browsers.firefox.name\"): "
                    + environment.getProperty("app.micro_yaml.browsers.firefox.name"));
            System.out.println("  - environment.getProperty(\"app.micro_yaml.browsers.firefox.releases.1.engine\"): "
                    + environment.getProperty("app.micro_yaml.browsers.firefox.releases.1.engine"));
            System.out.println("==================================================");
        };
    }

}
