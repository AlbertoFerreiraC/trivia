# Usamos una imagen oficial de Java 21
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copiamos el JAR que ya compilamos localmente
COPY build/libs/trivia-0.0.1-SNAPSHOT.jar app.jar

# Exponemos el puerto
EXPOSE 8081

# Comando para arrancar la aplicación respetando el puerto dinámico de la nube
ENTRYPOINT ["java", "-jar", "app.jar"]