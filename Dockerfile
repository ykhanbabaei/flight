# Stage 1: Build React/Vite frontend
FROM node:22-alpine AS frontend-build
WORKDIR /app/web-frontend
COPY web-frontend/package*.json ./
RUN npm ci
COPY web-frontend/ ./
RUN npm run build


# Stage 2: Build Spring Boot application
FROM maven:3.9-eclipse-temurin-21 AS backend-build
WORKDIR /app
COPY pom.xml ./
# Download dependencies for better caching
RUN mvn dependency:go-offline -B
COPY src ./src
COPY --from=frontend-build /app/web-frontend/dist ./src/main/resources/static
RUN mvn clean package -DskipTests


# Stage 3: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=backend-build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]