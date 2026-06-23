FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY knowledge-base-service/target/knowledge-base-service-1.0.0.jar app.jar
EXPOSE 8095
ENTRYPOINT ["java", "-jar", "app.jar"]
