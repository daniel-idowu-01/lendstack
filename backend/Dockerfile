# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

# ---- Run stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd -r -u 1001 lendstack
COPY --from=build /app/target/*.jar app.jar
RUN mkdir -p /app/data/documents && chown -R lendstack /app/data
USER lendstack
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
