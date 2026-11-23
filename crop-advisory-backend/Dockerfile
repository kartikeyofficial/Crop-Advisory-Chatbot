FROM maven:3.9-eclipse-temurin-25 AS build

WORKDIR /app

# Copy Maven configuration
COPY pom.xml .

# Download dependencies
RUN mvn dependency:go-offline

# Copy source code
COPY src ./src

# Build the JAR file
RUN mvn clean package -DskipTests

# Stage 2: Run the application
FROM eclipse-temurin:25-jre

WORKDIR /app

# Copy the generated JAR from the build stage
COPY --from=build /app/target/*.jar app.jar

# Expose Spring Boot's default port
EXPOSE 8090

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
