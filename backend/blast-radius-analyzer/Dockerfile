FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY blast-radius-analyzer/target/blast-radius-analyzer-1.0.0.jar app.jar
EXPOSE 8087
ENTRYPOINT ["java", "-jar", "app.jar"]
