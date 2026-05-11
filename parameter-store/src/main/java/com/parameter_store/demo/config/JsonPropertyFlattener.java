package com.parameter_store.demo.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * {@link BeanFactoryPostProcessor} que detecta propiedades cuyo valor es un JSON
 * válido en el {@link Environment} de Spring y las "aplana" (flatten) para que
 * cada clave del JSON quede como una propiedad individual accesible globalmente.
 * <p>
 * <b>¿Por qué {@code BeanFactoryPostProcessor} y no {@code EnvironmentPostProcessor}?</b>
 * <br>
 * Cuando se usa {@code spring.config.import: aws-parameterstore:/demo/}, Spring Cloud AWS
 * carga los parámetros mediante el mecanismo de {@code ConfigData}, que se ejecuta
 * <b>después</b> de los {@code EnvironmentPostProcessor}. Un {@code BeanFactoryPostProcessor}
 * se ejecuta cuando el {@code Environment} ya está completamente poblado (incluidos los
 * valores de Parameter Store), pero <b>antes</b> de que se instancien los beans. Esto
 * garantiza que las propiedades aplanadas estén disponibles para {@code @Value},
 * {@code @ConfigurationProperties} y cualquier librería de terceros.
 * <p>
 * Implementa {@link PriorityOrdered} con {@link Ordered#HIGHEST_PRECEDENCE} para
 * ejecutarse antes que cualquier otro {@code BeanFactoryPostProcessor}, asegurando
 * que las propiedades aplanadas existan antes de que otros procesadores las necesiten.
 * <p>
 * <b>Caso de uso:</b> Un parámetro en AWS Parameter Store almacena un JSON como
 * String literal. Por ejemplo, el parámetro {@code micro_yaml} con valor:
 * <pre>
 * {"browsers":{"firefox":{"name":"Firefox","releases":{"1":{"status":"retired","engine":"Gecko"}}}}}
 * </pre>
 * Y en {@code application.yml}:
 * <pre>
 * app:
 *   micro_yaml: ${micro_yaml}
 * </pre>
 * Tras el procesamiento, quedan disponibles en el Environment:
 * <ul>
 *   <li>{@code app.micro_yaml.browsers.firefox.name} = {@code Firefox}</li>
 *   <li>{@code app.micro_yaml.browsers.firefox.releases.1.status} = {@code retired}</li>
 *   <li>{@code app.micro_yaml.browsers.firefox.releases.1.engine} = {@code Gecko}</li>
 * </ul>
 */
@Component
public class JsonPropertyFlattener implements BeanFactoryPostProcessor, EnvironmentAware, PriorityOrdered {

    private static final Logger logger = LoggerFactory.getLogger(JsonPropertyFlattener.class);
    private static final String FLATTENED_SOURCE_NAME = "json-flattened:properties";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private ConfigurableEnvironment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = (ConfigurableEnvironment) environment;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        Map<String, Object> flattenedProperties = new HashMap<>();

        for (PropertySource<?> propertySource : environment.getPropertySources()) {
            if (propertySource instanceof EnumerablePropertySource<?> enumerable) {
                for (String propertyName : enumerable.getPropertyNames()) {
                    // Resolver el valor con placeholders (${micro_yaml} → valor real)
                    try {
                        String resolvedValue = environment.getProperty(propertyName);
                        if (resolvedValue != null) {
                            processJsonValue(propertyName, resolvedValue, flattenedProperties);
                        }
                    } catch (Exception e) {
                        // Algunas propiedades pueden no resolverse; ignorar
                    }
                }
            }
        }

        if (!flattenedProperties.isEmpty()) {
            logger.info("JSON Flattener: Se aplanaron {} propiedades desde JSON en el Environment",
                    flattenedProperties.size());
            flattenedProperties.forEach((key, value) ->
                    logger.info("  → {} = {}", key, value));

            // addFirst garantiza que las propiedades aplanadas tengan máxima prioridad
            environment.getPropertySources().addFirst(
                    new MapPropertySource(FLATTENED_SOURCE_NAME, flattenedProperties));
        } else {
            logger.warn("JSON Flattener: No se encontró ninguna propiedad JSON para aplanar");
        }
    }

    /**
     * Intenta parsear un valor como JSON. Si es un JSON Object válido, aplana
     * recursivamente sus claves usando el nombre de la propiedad original como prefijo.
     * <p>
     * También maneja el caso de doble serialización de AWS Parameter Store, donde
     * el JSON puede llegar como: {@code "\"{\\\"key\\\":\\\"value\\\"}\""}.
     */
    private void processJsonValue(String propertyName, String value, Map<String, Object> result) {
        String cleaned = value.trim();

        // Ignorar valores vacíos o que claramente no son JSON
        if (cleaned.isEmpty() || (!cleaned.startsWith("{") && !cleaned.startsWith("\""))) {
            return;
        }

        // Manejar doble serialización de AWS Parameter Store
        if (cleaned.startsWith("\"") && cleaned.endsWith("\"")) {
            try {
                cleaned = objectMapper.readValue(cleaned, String.class);
            } catch (JsonProcessingException e) {
                // No es un String JSON doblemente serializado, ignorar
                return;
            }
        }

        // Intentar parsear como JSON Object
        if (!cleaned.startsWith("{")) {
            return;
        }

        try {
            Map<String, Object> jsonMap = objectMapper.readValue(
                    cleaned, new TypeReference<>() {});
            flatten(propertyName, jsonMap, result);
            logger.info("JSON Flattener: Propiedad '{}' aplanada exitosamente ({} claves extraídas)",
                    propertyName, countLeaves(jsonMap));
        } catch (JsonProcessingException e) {
            // No es un JSON válido, se ignora silenciosamente
            // (es normal que muchos valores String no sean JSON)
        }
    }

    /**
     * Aplana recursivamente un Map anidado en propiedades con notación de puntos.
     * <p>
     * Ejemplo: {@code flatten("app.micro_yaml", {"db": {"host": "localhost"}})} produce
     * {@code app.micro_yaml.db.host = localhost}.
     */
    @SuppressWarnings("unchecked")
    private void flatten(String prefix, Map<String, Object> map, Map<String, Object> result) {
        map.forEach((key, value) -> {
            String fullKey = prefix + "." + key;
            if (value instanceof Map) {
                flatten(fullKey, (Map<String, Object>) value, result);
            } else {
                result.put(fullKey, value != null ? value.toString() : "");
            }
        });
    }

    /**
     * Cuenta las hojas (valores finales) de un Map anidado para logging.
     */
    @SuppressWarnings("unchecked")
    private int countLeaves(Map<String, Object> map) {
        int count = 0;
        for (Object value : map.values()) {
            if (value instanceof Map) {
                count += countLeaves((Map<String, Object>) value);
            } else {
                count++;
            }
        }
        return count;
    }

    /**
     * Se ejecuta con la prioridad más alta para garantizar que las propiedades
     * aplanadas estén disponibles antes que cualquier otro BeanFactoryPostProcessor.
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}

