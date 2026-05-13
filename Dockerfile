# Build stage
FROM maven:3-eclipse-temurin-17 AS build
COPY . /app
WORKDIR /app
RUN mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:17-jre
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app.jar"]
