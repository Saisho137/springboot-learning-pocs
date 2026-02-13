# AWS Parameter Store Integration

Este proyecto integra AWS Parameter Store para gestionar configuraciones y secretos de forma centralizada.

## Configuración

### 1. Variables de Entorno

Configura las siguientes variables de entorno con tus credenciales de AWS:

```bash
export AWS_ACCESS_KEY_ID=tu-access-key-id
export AWS_SECRET_ACCESS_KEY=tu-secret-access-key
```

### 2. Archivo de Configuración

El archivo `application.yaml` contiene la configuración de AWS:

```yaml
aws:
  region: us-east-1  # Región de AWS
  credentials:
    access-key: ${AWS_ACCESS_KEY_ID:your-access-key-id}
    secret-key: ${AWS_SECRET_ACCESS_KEY:your-secret-access-key}
  parameter-store:
    enabled: true
    prefix: /demo/  # Prefijo para todos los parámetros
```

### 3. Dependencias

Las siguientes dependencias de AWS SDK se han agregado al `build.gradle`:

```groovy
implementation platform('software.amazon.awssdk:bom:2.25.11')
implementation 'software.amazon.awssdk:ssm'
implementation 'software.amazon.awssdk:sts'
```

## Estructura del Proyecto

### Clases Principales

1. **AwsConfig** (`com.parameter_store.demo.config.AwsConfig`)
   - Configuración de AWS SDK
   - Crea el cliente SsmClient para interactuar con Parameter Store

2. **ParameterStoreService** (`com.parameter_store.demo.service.ParameterStoreService`)
   - Servicio para gestionar parámetros en AWS Parameter Store
   - Métodos principales:
     - `getParameter(String name)`: Obtiene un parámetro individual
     - `getParametersByPath()`: Obtiene todos los parámetros del path configurado
     - `putParameter(String name, String value, boolean secure)`: Guarda/actualiza un parámetro
     - `deleteParameter(String name)`: Elimina un parámetro

3. **WebController** (`com.parameter_store.demo.WebController`)
   - API REST para gestionar parámetros
   - Endpoints disponibles (ver sección API)

## API Endpoints

### 1. Health Check
```bash
GET /api/v1/test
```
Respuesta: `true`

### 2. Obtener un Parámetro
```bash
GET /api/v1/parameter/{name}
```
Ejemplo:
```bash
curl http://localhost:8080/api/v1/parameter/database-url
```

### 3. Obtener Todos los Parámetros
```bash
GET /api/v1/parameters
```
Ejemplo:
```bash
curl http://localhost:8080/api/v1/parameters
```
Respuesta:
```json
{
  "database-url": "jdbc:mysql://localhost:3306/db",
  "api-key": "my-secret-key",
  "max-connections": "100"
}
```

### 4. Guardar/Actualizar un Parámetro
```bash
POST /api/v1/parameter
Content-Type: application/json
```
Body:
```json
{
  "name": "database-url",
  "value": "jdbc:mysql://localhost:3306/db",
  "secure": false
}
```
Ejemplo:
```bash
curl -X POST http://localhost:8080/api/v1/parameter \
  -H "Content-Type: application/json" \
  -d '{
    "name": "database-url",
    "value": "jdbc:mysql://localhost:3306/mydb",
    "secure": false
  }'
```

Para parámetros sensibles, usa `"secure": true` para encriptarlos:
```json
{
  "name": "api-key",
  "value": "my-secret-api-key",
  "secure": true
}
```

### 5. Eliminar un Parámetro
```bash
DELETE /api/v1/parameter/{name}
```
Ejemplo:
```bash
curl -X DELETE http://localhost:8080/api/v1/parameter/database-url
```

## Uso en AWS

### Crear Parámetros Manualmente en AWS Console

1. Ve a AWS Systems Manager > Parameter Store
2. Crea un nuevo parámetro con el path `/demo/{nombre}`
3. Ejemplo: `/demo/database-url` con valor `jdbc:mysql://localhost:3306/db`

### Usar AWS CLI

```bash
# Crear un parámetro simple
aws ssm put-parameter \
  --name "/demo/database-url" \
  --value "jdbc:mysql://localhost:3306/db" \
  --type "String"

# Crear un parámetro seguro (encriptado)
aws ssm put-parameter \
  --name "/demo/api-key" \
  --value "my-secret-key" \
  --type "SecureString"

# Obtener un parámetro
aws ssm get-parameter --name "/demo/database-url" --with-decryption

# Obtener todos los parámetros de un path
aws ssm get-parameters-by-path --path "/demo/" --with-decryption

# Eliminar un parámetro
aws ssm delete-parameter --name "/demo/database-url"
```

## Ejecutar el Proyecto

### 1. Compilar
```bash
./gradlew build
```

### 2. Ejecutar
```bash
./gradlew bootRun
```

O con variables de entorno:
```bash
AWS_ACCESS_KEY_ID=tu-key AWS_SECRET_ACCESS_KEY=tu-secret ./gradlew bootRun
```

### 3. Usando JAR
```bash
java -jar build/libs/demo-0.0.1-SNAPSHOT.jar
```

## Seguridad

⚠️ **Importante:**
- Nunca commitas las credenciales de AWS en el código
- Usa variables de entorno o AWS IAM roles en producción
- Para ambientes productivos, considera usar AWS IAM roles en lugar de credenciales estáticas
- Los parámetros sensibles deben marcarse como `secure: true` para encriptarlos

## Permisos IAM Requeridos

Tu usuario o rol de AWS debe tener los siguientes permisos:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "ssm:GetParameter",
        "ssm:GetParameters",
        "ssm:GetParametersByPath",
        "ssm:PutParameter",
        "ssm:DeleteParameter"
      ],
      "Resource": "arn:aws:ssm:us-east-1:*:parameter/demo/*"
    }
  ]
}
```

## Ejemplos de Uso

### En código Java

```java
@Service
public class MiServicio {
    
    private final ParameterStoreService parameterStoreService;
    
    public MiServicio(ParameterStoreService parameterStoreService) {
        this.parameterStoreService = parameterStoreService;
    }
    
    public void conectarBaseDatos() {
        String dbUrl = parameterStoreService.getParameter("database-url");
        // Usar la URL para conectar
    }
    
    public void cargarConfiguracion() {
        Map<String, String> config = parameterStoreService.getParametersByPath();
        // Procesar todas las configuraciones
    }
}
```

## Troubleshooting

### Error: "Cannot resolve symbol 'software'"
Ejecuta `./gradlew build --refresh-dependencies` para descargar las dependencias de AWS SDK.

### Error: "Access Denied"
Verifica que tu usuario de AWS tenga los permisos necesarios en IAM.

### Error: "ParameterNotFoundException"
El parámetro no existe en Parameter Store. Créalo primero usando la consola de AWS o AWS CLI.

### Error: "Invalid credentials"
Verifica que las variables de entorno AWS_ACCESS_KEY_ID y AWS_SECRET_ACCESS_KEY estén configuradas correctamente.
