FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

# Copy built jar from Jenkins workspace
COPY target/*.jar app.jar

EXPOSE 8050

ENTRYPOINT ["java", "-jar", "app.jar"]
