package com.parameter_store.demo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class DemoApplication {

    @Value("${app.name}")
    private String appName;

    @Value("${app.description}")
    private String appDescription;

    @Value("${app.secreto}")
    private String appSecreto;

    @Value("${app.micro_yaml}")
    private String appMicroYaml;

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
            System.out.println("  - app.name: " + appName);
            System.out.println("  - app.description: " + appDescription);
            System.out.println("  - app.description: " + appSecreto);
            System.out.println("  - app.description: " + appMicroYaml);
            System.out.println("==================================================");
        };
    }

}
