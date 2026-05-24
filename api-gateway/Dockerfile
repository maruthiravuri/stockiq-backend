FROM eclipse-temurin:21-jdk-alpine AS builder
RUN apk add --no-cache maven

WORKDIR /build

# Install parent POM into local .m2 (it's now in context because context=repo root)
COPY pom.xml ./pom.xml
RUN mvn install -f pom.xml -N -q

# Copy and build this service
COPY api-gateway/pom.xml ./api-gateway/pom.xml
COPY api-gateway/src ./api-gateway/src
WORKDIR /build/api-gateway
RUN mvn package -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S stockiq && adduser -S stockiq -G stockiq
WORKDIR /app
COPY --from=builder /build/api-gateway/target/*.jar app.jar
USER stockiq
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
