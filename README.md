# AWS Parameter Store → Spring Boot YAML Mapping

Documentación de la configuración mínima para cargar parámetros de **AWS Parameter Store** e inyectarlos en el `application.yaml` de un microservicio Spring Boot 3.x.

---

## Tabla de contenidos

1. [Prerrequisitos](#1-prerrequisitos)
2. [Estructura de parámetros en AWS](#2-estructura-de-parámetros-en-aws)
3. [Vía 1 — Parámetros con valores String normales](#3-vía-1--parámetros-con-valores-string-normales-cifrados-o-no)
4. [Vía 2 — Parámetros cuyo valor es un JSON (aplanado)](#4-vía-2--parámetros-cuyo-valor-es-un-json-aplanado)
5. [Tests](#5-tests)
6. [Diagrama de flujo](#6-diagrama-de-flujo)
7. [Troubleshooting](#7-troubleshooting)

---

## 1. Prerrequisitos

| Requisito | Detalle |
|-----------|---------|
| Java | 21+ |
| Spring Boot | 3.x |
| Spring Cloud AWS | 3.3.x |
| AWS SDK v2 | 2.25.x |
| Perfil AWS configurado | `~/.aws/credentials` con el perfil correcto |
| Permiso IAM mínimo | `ssm:GetParameter`, `ssm:GetParametersByPath` (+ `kms:Decrypt` si el parámetro es `SecureString`) |

---

## 2. Estructura de parámetros en AWS

Todos los parámetros deben crearse bajo un **prefijo común** (path). En este proyecto el prefijo es `/demo/`.

```
/demo/
  ├── name                → "mi-aplicacion"                        (String normal)
  ├── description.test    → "Una descripción de prueba"            (String normal)
  ├── secreto             → "s3cr3t0_cifrado"                      (SecureString cifrado)
  └── micro_yaml          → {"browsers":{"firefox":{"name":"Firefox",...}}}  (String con JSON)
```

> **Convención de nombres:** el nombre del parámetro **sin el prefijo** es la clave que se usa en `${...}` dentro del YAML.  
> Ejemplo: el parámetro `/demo/name` se referencia como `${name}`.

---

## 3. Vía 1 — Parámetros con valores String normales (cifrados o no)

Esta es la vía estándar. Spring Cloud AWS se encarga de la carga automática al arrancar.

### 3.1 Dependencias — `build.gradle`

```groovy
dependencies {
    // BOM de Spring Cloud AWS (gestiona versiones de todos los módulos)
    implementation platform('io.awspring.cloud:spring-cloud-aws-dependencies:3.3.0')

    // Starter de Parameter Store: incluye el ConfigDataLoader automático
    implementation 'io.awspring.cloud:spring-cloud-aws-starter-parameter-store'

    // Spring Boot Web (o cualquier otro starter que uses)
    implementation 'org.springframework.boot:spring-boot-starter-web'

    // AWS SDK v2 BOM + módulos SSM y STS (STS solo si usas AssumeRole)
    implementation platform('software.amazon.awssdk:bom:2.25.11')
    implementation 'software.amazon.awssdk:ssm'
    implementation 'software.amazon.awssdk:sts'
}
```

### 3.2 Configuración — `application.yaml`

```yaml
spring:
  application:
    name: demo

  config:
    # Le indica a Spring Boot que cargue todos los parámetros bajo el path /demo/
    # Spring Cloud AWS los convierte en propiedades del Environment antes de que
    # arranque cualquier bean.
    import: "aws-parameterstore:/demo/"

  cloud:
    aws:
      credentials:
        profile:
          # Nombre del perfil en ~/.aws/credentials
          name: ${AWS_PROFILE:mi-perfil-aws}
      region:
        static: us-east-1
      parameterstore:
        enabled: true

# Mapeo explícito: los ${...} apuntan al nombre del parámetro sin el prefijo /demo/
app:
  name: ${name}                    # ← /demo/name
  description: ${description.test} # ← /demo/description.test
  secreto: ${secreto}              # ← /demo/secreto  (SecureString: se descifra automáticamente)
```

> **Parámetros cifrados (`SecureString`):** Spring Cloud AWS llama a SSM con `withDecryption=true` por defecto. No se requiere ninguna configuración adicional, solo que el rol IAM tenga permiso `kms:Decrypt` sobre la clave KMS usada.

### 3.3 Inyección con `@Value`

La forma más directa. Usa la ruta YAML que definiste en `application.yaml`:

```java
@Value("${app.name}")
private String appName;

@Value("${app.secreto}")
private String secreto;
```

### 3.4 Inyección con `@ConfigurationProperties` (recomendada para grupos)

**Paso 1** — Clase de binding tipado:

```java
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private String name;
    private String description;
    private String secreto;

    // getters y setters
}
```

**Paso 2** — Habilitar en la clase principal:

```java
@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class DemoApplication { ... }
```

**Paso 3** — Inyectar donde se necesite:

```java
@Service
public class MiServicio {
    private final AppProperties props;

    public MiServicio(AppProperties props) {
        this.props = props;
    }

    public void ejemplo() {
        System.out.println(props.getName());    // valor de /demo/name
        System.out.println(props.getSecreto()); // valor de /demo/secreto
    }
}
```

### 3.5 Bean `SsmClient` personalizado (opcional)

Solo necesario si quieres consultar Parameter Store **programáticamente** en tiempo de ejecución (fuera del ciclo de arranque):

```java
@Configuration
public class AwsConfig {

    @Value("${aws.region}")
    private String region;

    @Value("${spring.cloud.aws.credentials.profile.name:default}")
    private String profileName;

    @Bean
    public SsmClient ssmClient() {
        return SsmClient.builder()
                .region(Region.of(region))
                .credentialsProvider(ProfileCredentialsProvider.create(profileName))
                .build();
    }
}
```

Y en `application.yaml` añadir la sección de soporte:

```yaml
aws:
  region: us-east-1
  parameter-store:
    prefix: /demo/
```

---

## 4. Vía 2 — Parámetros cuyo valor es un JSON (aplanado)

### 4.1 El problema

Algunas librerías de terceros (o configuraciones propias) esperan encontrar en el `Environment` propiedades con notación de puntos planas. Por ejemplo:

```
recaptcha.clientKey = abc123
recaptcha.secretKey = xyz789
```

Pero en AWS Parameter Store es habitual guardar toda esa configuración en un **único parámetro** con valor JSON:

```
/demo/micro_yaml  →  {"browsers":{"firefox":{"name":"Firefox","releases":{"1":{"status":"retired","engine":"Gecko"}}}}}
```

Spring Boot carga ese parámetro como un `String` en el `Environment`, no como claves individuales. La Vía 2 resuelve esto mediante un componente que **detecta y aplana** automáticamente esos JSONs.

### 4.2 Dependencias

Las **mismas** que en la Vía 1. No se requieren dependencias adicionales ya que `spring-boot-starter-web` incluye Jackson.

### 4.3 Configuración — `application.yaml`

```yaml
spring:
  config:
    import: "aws-parameterstore:/demo/"
  cloud:
    aws:
      credentials:
        profile:
          name: ${AWS_PROFILE:mi-perfil-aws}
      region:
        static: us-east-1
      parameterstore:
        enabled: true

app:
  # El valor de /demo/micro_yaml es un JSON completo.
  # Se mapea como String; el JsonPropertyFlattener se encargará de aplanarlo.
  micro_yaml: ${micro_yaml}
```

### 4.4 Componente `JsonPropertyFlattener`

Este es el **núcleo de la Vía 2**. Crea el siguiente archivo en tu paquete `config`:

```java
package com.tu_empresa.demo.config;

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
import org.springframework.core.env.*;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Detecta propiedades cuyo valor es un JSON válido en el Environment de Spring
 * y las "aplana" para que cada clave del JSON quede como propiedad individual
 * accesible con @Value, @ConfigurationProperties o environment.getProperty().
 *
 * ¿Por qué BeanFactoryPostProcessor y no EnvironmentPostProcessor?
 * spring.config.import (ConfigData) se ejecuta DESPUÉS de los EnvironmentPostProcessor.
 * Un BeanFactoryPostProcessor se ejecuta cuando el Environment ya está completamente
 * poblado (incluidos los valores de Parameter Store), pero ANTES de instanciar beans.
 */
@Component
public class JsonPropertyFlattener
        implements BeanFactoryPostProcessor, EnvironmentAware, PriorityOrdered {

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
                    try {
                        String resolvedValue = environment.getProperty(propertyName);
                        if (resolvedValue != null) {
                            processJsonValue(propertyName, resolvedValue, flattenedProperties);
                        }
                    } catch (Exception e) {
                        // Propiedades que no se resuelven se ignoran
                    }
                }
            }
        }

        if (!flattenedProperties.isEmpty()) {
            logger.info("JSON Flattener: {} propiedades aplanadas", flattenedProperties.size());
            // addFirst → máxima prioridad en el Environment
            environment.getPropertySources().addFirst(
                    new MapPropertySource(FLATTENED_SOURCE_NAME, flattenedProperties));
        }
    }

    private void processJsonValue(String propertyName, String value, Map<String, Object> result) {
        String cleaned = value.trim();

        if (cleaned.isEmpty() || (!cleaned.startsWith("{") && !cleaned.startsWith("\""))) {
            return;
        }

        // Manejar doble serialización de AWS Parameter Store: "\"{ ... }\""
        if (cleaned.startsWith("\"") && cleaned.endsWith("\"")) {
            try {
                cleaned = objectMapper.readValue(cleaned, String.class);
            } catch (JsonProcessingException e) {
                return;
            }
        }

        if (!cleaned.startsWith("{")) return;

        try {
            Map<String, Object> jsonMap = objectMapper.readValue(cleaned, new TypeReference<>() {});
            flatten(propertyName, jsonMap, result);
        } catch (JsonProcessingException e) {
            // No es JSON válido, se ignora (es lo esperado para valores String normales)
        }
    }

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

    @Override
    public int getOrder() {
        // Se ejecuta antes que cualquier otro BeanFactoryPostProcessor
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
```

#### ¿Cómo funciona internamente?

```
Arranque de Spring Boot
       │
       ▼
spring.config.import carga /demo/* desde Parameter Store
       │  (incluye micro_yaml como String con valor JSON)
       ▼
JsonPropertyFlattener.postProcessBeanFactory()
  ├─ Itera todas las PropertySources del Environment
  ├─ Para cada valor, intenta parsearlo como JSON
  ├─ Si es JSON Object → aplana recursivamente (notación de puntos)
  └─ Registra las claves aplanadas como nueva PropertySource (máxima prioridad)
       │
       ▼
 Environment contiene:
   app.micro_yaml                              = {"browsers":{...}}  (String original)
   app.micro_yaml.browsers.firefox.name        = Firefox             (aplanado ✓)
   app.micro_yaml.browsers.firefox.releases.1.status = retired      (aplanado ✓)
```

### 4.5 Inyección de las claves aplanadas con `@Value`

Una vez que el `JsonPropertyFlattener` ha procesado el JSON, sus claves internas están disponibles **exactamente igual** que cualquier otra propiedad del `Environment`:

```java
// Parámetro en AWS: /demo/micro_yaml → {"browsers":{"firefox":{"name":"Firefox",...}}}
// YAML: app.micro_yaml: ${micro_yaml}

@Value("${app.micro_yaml.browsers.firefox.name:N/A}")
private String firefoxName;

@Value("${app.micro_yaml.browsers.firefox.releases.1.status:N/A}")
private String firefoxReleaseStatus;

@Value("${app.micro_yaml.browsers.firefox.releases.1.engine:N/A}")
private String firefoxReleaseEngine;
```

> **Clave:** las propiedades aplanadas usan como **prefijo el nombre de la propiedad YAML** que contiene el JSON (en este caso `app.micro_yaml`), no el nombre del parámetro en AWS.

### 4.6 Acceso programático (como lo haría una librería de terceros)

```java
// Esto funciona porque JsonPropertyFlattener registra las claves en el Environment global
String name = environment.getProperty("app.micro_yaml.browsers.firefox.name");
```

### 4.7 Habilitación de `@ConfigurationProperties` en la clase principal

```java
@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class DemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
```

### 4.8 Endpoint de diagnóstico (opcional)

Útil para verificar en tiempo de ejecución qué claves fueron aplanadas:

```java
@GetMapping("/flattened-properties")
public ResponseEntity<Map<String, String>> getFlattenedProperties() {
    Map<String, String> flattened = new TreeMap<>();
    for (PropertySource<?> ps : environment.getPropertySources()) {
        if (ps.getName().startsWith("json-flattened:") &&
            ps instanceof EnumerablePropertySource<?> enumerable) {
            for (String name : enumerable.getPropertyNames()) {
                Object value = enumerable.getProperty(name);
                flattened.put(name, value != null ? value.toString() : "null");
            }
        }
    }
    return ResponseEntity.ok(flattened);
}
```

---

## 5. Tests

### Configuración de `src/test/resources/application.yaml`

En tests se deshabilita la conexión real a AWS y se proveen valores mock:

```yaml
spring:
  config:
    # "optional:" evita que el test falle si AWS no está disponible
    import: "optional:aws-parameterstore:/demo/"
  cloud:
    aws:
      parameterstore:
        enabled: false

aws:
  region: us-east-1
  parameter-store:
    prefix: /demo/

app:
  name: test-name
  description: test-description
  secreto: test-secreto
  # Valor JSON mock para que el JsonPropertyFlattener lo aplane durante los tests
  micro_yaml: '{"browsers":{"chrome":{"name":"Chrome","releases":{"1":{"status":"stable","engine":"Blink"}}}}}'
```

### Test unitario del `JsonPropertyFlattener`

```java
class JsonPropertyFlattenerTest {

    private void runFlattener(ConfigurableEnvironment env) {
        JsonPropertyFlattener flattener = new JsonPropertyFlattener();
        flattener.setEnvironment(env);
        flattener.postProcessBeanFactory(new DefaultListableBeanFactory());
    }

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

    @Test
    void shouldFlattenNestedJson() {
        ConfigurableEnvironment env = new MockEnvironment();
        String json = "{\"browsers\":{\"firefox\":{\"name\":\"Firefox\"}}}";
        env.getPropertySources().addFirst(new MapPropertySource("test", Map.of("app.micro_yaml", json)));
        runFlattener(env);

        assertEquals("Firefox", env.getProperty("app.micro_yaml.browsers.firefox.name"));
    }

    @Test
    void shouldIgnoreNonJsonValues() {
        ConfigurableEnvironment env = new MockEnvironment();
        env.getPropertySources().addFirst(new MapPropertySource("test", Map.of(
                "app.name", "mi-app",
                "app.port", "8080"
        )));
        runFlattener(env);

        // Los valores String normales no se modifican
        assertEquals("mi-app", env.getProperty("app.name"));
        assertEquals("8080", env.getProperty("app.port"));
    }
}
```

---

## 6. Diagrama de flujo

```
┌─────────────────────────────────────────────────────────────────┐
│                        AWS Parameter Store                      │
│                                                                 │
│  /demo/name            → "mi-aplicacion"           (String)     │
│  /demo/description.test→ "Una descripción"          (String)    │
│  /demo/secreto         → "s3cr3t0"                 (SecureStr)  │
│  /demo/micro_yaml      → {"browsers":{...}}         (String)    │
└───────────────────────────────┬─────────────────────────────────┘
                                │  spring.config.import
                                │  aws-parameterstore:/demo/
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Spring Environment (arranque)                 │
│                                                                 │
│  name              = "mi-aplicacion"                            │
│  description.test  = "Una descripción"                          │
│  secreto           = "s3cr3t0"          ← descifrado auto ✓     │
│  micro_yaml        = '{"browsers":{...}}'  ← String crudo       │
└───────────────────────────────┬─────────────────────────────────┘
                                │  application.yaml resuelve ${...}
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Spring Environment (YAML)                     │
│                                                                 │
│  app.name          = "mi-aplicacion"                            │
│  app.description   = "Una descripción"                          │
│  app.secreto       = "s3cr3t0"                                  │
│  app.micro_yaml    = '{"browsers":{...}}'  ← todavía String     │
└───────────────────────────────┬─────────────────────────────────┘
                                │  JsonPropertyFlattener
                                │  (BeanFactoryPostProcessor)
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                Spring Environment (FINAL)                        │
│                                                                 │
│  app.name              = "mi-aplicacion"                        │
│  app.secreto           = "s3cr3t0"                              │
│  app.micro_yaml        = '{"browsers":{...}}'  ← String original│
│                                                                 │
│  app.micro_yaml.browsers.firefox.name          = "Firefox"  ✓  │
│  app.micro_yaml.browsers.firefox.releases.1.status = "retired" │
│  app.micro_yaml.browsers.firefox.releases.1.engine = "Gecko"   │
└─────────────────────────────────────────────────────────────────┘
         │                            │
         ▼                            ▼
  @Value("${app.name}")     @Value("${app.micro_yaml.browsers.firefox.name}")
  @ConfigurationProperties  environment.getProperty(...)   ← librerías terceros
```

---

## 7. Troubleshooting

### `Could not resolve placeholder '${name}'`
- Verifica que el parámetro `/demo/name` exista en Parameter Store.
- Verifica que el perfil AWS tenga los permisos `ssm:GetParameter`.
- En local, comprueba que `~/.aws/credentials` tenga el perfil configurado.

### El valor JSON no se aplana (propiedades aplanadas no aparecen)
- Asegúrate de que `JsonPropertyFlattener` está anotado con `@Component` y que el paquete está dentro del `@SpringBootApplication` scan.
- Verifica que el valor del parámetro en AWS sea un JSON **Object** válido (empieza con `{`).
- Comprueba los logs: el flattener registra `INFO` con las propiedades aplanadas al arrancar.

### El JSON llega doblemente serializado (`"\"{ ... }\""`)
- El `JsonPropertyFlattener` maneja este caso automáticamente: detecta la cadena envuelta en comillas y la desenvuelve antes de parsear.

### Error `Access Denied` al descifrar un `SecureString`
- El rol/usuario IAM necesita permiso `kms:Decrypt` sobre la clave KMS usada al cifrar el parámetro.

### Tests fallan por conexión a AWS
- Usa `optional:aws-parameterstore:/demo/` en `src/test/resources/application.yaml` y deshabilita `spring.cloud.aws.parameterstore.enabled: false`.

