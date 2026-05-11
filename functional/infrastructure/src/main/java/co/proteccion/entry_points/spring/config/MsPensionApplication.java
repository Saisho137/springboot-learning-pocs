package co.proteccion.entry_points.spring.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "co.proteccion")
public class MsPensionApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsPensionApplication.class, args);
        System.out.println("🚀 Microservicio Pension Voluntaria iniciado en puerto 8080");
    }
}