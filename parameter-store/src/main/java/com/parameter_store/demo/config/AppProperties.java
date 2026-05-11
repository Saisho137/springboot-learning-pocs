package com.parameter_store.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Clase de binding tipado para las propiedades bajo el prefijo "app".
 * <p>
 * {@code micro_yaml} se mantiene como String porque el {@link JsonPropertyFlattener}
 * se encarga de aplanar su contenido JSON en propiedades individuales del Environment.
 * Así, las claves internas del JSON quedan accesibles como {@code app.micro_yaml.browsers.firefox.name}, etc.
 */
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String name;
    private String description;
    private String secreto;
    private String microYaml;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSecreto() {
        return secreto;
    }

    public void setSecreto(String secreto) {
        this.secreto = secreto;
    }

    public String getMicroYaml() {
        return microYaml;
    }

    public void setMicroYaml(String microYaml) {
        this.microYaml = microYaml;
    }
}
