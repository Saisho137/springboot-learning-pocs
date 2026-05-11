package com.parameter_store.demo.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.mock.env.MockEnvironment;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JsonPropertyFlattenerTest {

    /**
     * Helper: crea un flattener, le inyecta el environment y ejecuta postProcessBeanFactory.
     */
    private void runFlattener(ConfigurableEnvironment env) {
        JsonPropertyFlattener flattener = new JsonPropertyFlattener();
        flattener.setEnvironment(env);
        flattener.postProcessBeanFactory(new DefaultListableBeanFactory());
    }

    /**
     * Verifica que un JSON simple se aplane correctamente en propiedades individuales.
     */
    @Test
    void shouldFlattenSimpleJson() {
        ConfigurableEnvironment env = new MockEnvironment();
        env.getPropertySources().addFirst(new MapPropertySource("test", Map.of(
                "recaptcha", "{\"clientKey\":\"abc123\",\"secretKey\":\"xyz789\"}"
        )));

        runFlattener(env);

        assertEquals("abc123", env.getProperty("recaptcha.clientKey"));
        assertEquals("xyz789", env.getProperty("recaptcha.secretKey"));
    }

    /**
     * Verifica que un JSON anidado se aplane recursivamente con notación de puntos.
     */
    @Test
    void shouldFlattenNestedJson() {
        ConfigurableEnvironment env = new MockEnvironment();
        String json = "{\"browsers\":{\"firefox\":{\"name\":\"Firefox\",\"releases\":{\"1\":{\"status\":\"retired\",\"engine\":\"Gecko\"}}}}}";
        env.getPropertySources().addFirst(new MapPropertySource("test", Map.of(
                "app.micro_yaml", json
        )));

        runFlattener(env);

        assertEquals("Firefox", env.getProperty("app.micro_yaml.browsers.firefox.name"));
        assertEquals("retired", env.getProperty("app.micro_yaml.browsers.firefox.releases.1.status"));
        assertEquals("Gecko", env.getProperty("app.micro_yaml.browsers.firefox.releases.1.engine"));
    }

    /**
     * Verifica que se maneje correctamente JSON doblemente serializado
     * (como puede ocurrir con AWS Parameter Store).
     */
    @Test
    void shouldHandleDoubleSerializedJson() {
        ConfigurableEnvironment env = new MockEnvironment();
        String doubleSerializedJson = "\"{\\\"clientKey\\\":\\\"abc123\\\",\\\"secretKey\\\":\\\"xyz789\\\"}\"";
        env.getPropertySources().addFirst(new MapPropertySource("test", Map.of(
                "recaptcha", doubleSerializedJson
        )));

        runFlattener(env);

        assertEquals("abc123", env.getProperty("recaptcha.clientKey"));
        assertEquals("xyz789", env.getProperty("recaptcha.secretKey"));
    }

    /**
     * Verifica que valores no-JSON se ignoren sin errores.
     */
    @Test
    void shouldIgnoreNonJsonValues() {
        ConfigurableEnvironment env = new MockEnvironment();
        env.getPropertySources().addFirst(new MapPropertySource("test", Map.of(
                "app.name", "mi-aplicacion",
                "app.description", "Una descripción normal",
                "app.port", "8080"
        )));

        runFlattener(env);

        assertEquals("mi-aplicacion", env.getProperty("app.name"));
        assertEquals("Una descripción normal", env.getProperty("app.description"));
        assertEquals("8080", env.getProperty("app.port"));
    }

    /**
     * Verifica que las propiedades aplanadas coexistan con las originales.
     */
    @Test
    void flattenedPropertiesShouldCoexistWithOriginal() {
        ConfigurableEnvironment env = new MockEnvironment();
        String json = "{\"clientKey\":\"abc123\",\"secretKey\":\"xyz789\"}";
        env.getPropertySources().addFirst(new MapPropertySource("test", Map.of(
                "recaptcha", json,
                "app.name", "mi-aplicacion"
        )));

        runFlattener(env);

        assertEquals("abc123", env.getProperty("recaptcha.clientKey"));
        assertEquals("xyz789", env.getProperty("recaptcha.secretKey"));
        assertEquals("mi-aplicacion", env.getProperty("app.name"));
        assertEquals(json, env.getProperty("recaptcha"));
    }

    /**
     * Verifica que un JSON con múltiples niveles de anidación se aplane completamente.
     */
    @Test
    void shouldFlattenDeeplyNestedJson() {
        ConfigurableEnvironment env = new MockEnvironment();
        String json = "{\"level1\":{\"level2\":{\"level3\":{\"value\":\"deep\"}}}}";
        env.getPropertySources().addFirst(new MapPropertySource("test", Map.of(
                "config", json
        )));

        runFlattener(env);

        assertEquals("deep", env.getProperty("config.level1.level2.level3.value"));
    }

    /**
     * Verifica que múltiples propiedades JSON se aplanen en una sola pasada.
     */
    @Test
    void shouldFlattenMultipleJsonProperties() {
        ConfigurableEnvironment env = new MockEnvironment();
        env.getPropertySources().addFirst(new MapPropertySource("test", Map.of(
                "recaptcha", "{\"clientKey\":\"abc\",\"secretKey\":\"xyz\"}",
                "database", "{\"host\":\"localhost\",\"port\":\"5432\"}"
        )));

        runFlattener(env);

        assertEquals("abc", env.getProperty("recaptcha.clientKey"));
        assertEquals("xyz", env.getProperty("recaptcha.secretKey"));
        assertEquals("localhost", env.getProperty("database.host"));
        assertEquals("5432", env.getProperty("database.port"));
    }
}

