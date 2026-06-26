# ==========================
# Stage 1 - Build
# ==========================
FROM maven:3.9.14-eclipse-temurin-25 AS builder

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

# ==========================
# Stage 2 - Runtime
# ==========================
FROM eclipse-temurin:25-jre

RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","app.jar"]