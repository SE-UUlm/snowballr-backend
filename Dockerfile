# Stage 1: Build the application
FROM gradle:8.10.2-jdk21-jammy AS build

WORKDIR /app

# Copy the Gradle wrapper files
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .

# Copy the project's source code and proto files
COPY src/main src/main
COPY api/proto api/proto

# Grant execution rights to the Gradle wrapper script and build the jar file
RUN chmod +x gradlew \
    && ./gradlew jar --no-daemon --stacktrace

# Stage 2: Run the application
FROM eclipse-temurin:21-jre-alpine-3.21 AS final

WORKDIR /app

# Run the application as a non-root user.
RUN adduser -D backend-user && chown -R backend-user /app
USER backend-user

# Copy built jar file
COPY --from=build /app/build/libs/snowballr-backend-*.jar app.jar

# Start execute the jar file when starting the container
ENTRYPOINT ["java", "-jar", "app.jar"]
