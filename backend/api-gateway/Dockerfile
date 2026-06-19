FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
RUN apk add --no-cache maven

COPY pom.xml ./pom.xml
COPY common-lib ./common-lib
COPY user-service ./user-service
COPY order-service ./order-service
COPY payment-service ./payment-service
COPY inventory-service ./inventory-service
COPY log-analyzer-service ./log-analyzer-service
COPY deployment-tracker-service ./deployment-tracker-service
COPY alert-correlation-service ./alert-correlation-service
COPY incident-management-service ./incident-management-service
COPY service-health-analyzer ./service-health-analyzer
COPY ai-rca-service ./ai-rca-service
COPY api-gateway ./api-gateway

RUN mvn -B -DskipTests -q -pl common-lib install && \
    mvn -B -DskipTests -q -pl api-gateway package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser
COPY --from=builder /app/api-gateway/target/*.jar app.jar
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
