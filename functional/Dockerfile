# CAMBIO CLAVE: Usamos Amazon Corretto que tiene soporte nativo para Mac M1/M2/M3
FROM amazoncorretto:17-alpine-jdk

WORKDIR /app

# Copiamos el JAR generado
COPY build/libs/*.jar app.jar

# Variables de entorno para los límites y Logs de GC
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+PrintGCDetails -Xlog:gc*"

# Ejecutamos
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]