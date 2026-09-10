# Step 1: Build file JAR bằng Maven
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Step 2: Runtime image nhẹ hơn
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Khai báo port mặc định
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]