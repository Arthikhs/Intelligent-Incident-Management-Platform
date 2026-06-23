FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY ai-copilot-service/target/ai-copilot-service-1.0.0.jar app.jar
EXPOSE 8098
ENTRYPOINT ["java", "-jar", "app.jar"]
