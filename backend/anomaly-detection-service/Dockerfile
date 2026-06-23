FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY anomaly-detection-service/target/anomaly-detection-service-1.0.0.jar app.jar
EXPOSE 8088
ENTRYPOINT ["java", "-jar", "app.jar"]
