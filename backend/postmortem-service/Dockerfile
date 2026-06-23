FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY postmortem-service/target/postmortem-service-1.0.0.jar app.jar
EXPOSE 8097
ENTRYPOINT ["java", "-jar", "app.jar"]
