# Stage 1: Build the application
FROM gradle:8.10.2-jdk21-jammy AS build

# Asset IDs of v0.4.38 release of grpc-health-probe
# https://github.com/grpc-ecosystem/grpc-health-probe/releases/tag/v0.4.38
ARG AMD64_ID=251600596
ARG ARM64_ID=251600609

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
    && ./gradlew shadowJar --no-daemon --stacktrace

# Install curl utility
RUN apt install curl

# Download binaries of grpc-health-probe based on the architecture and make them executable
RUN ARCH=$(uname -m) && \
    if [ "$ARCH" = "x86_64" ]; then \
      export ASSET_ID=${AMD64_ID}; \
    elif [ "$ARCH" = "aarch64" ]; then \
      export ASSET_ID=${ARM64_ID}; \
    else \
      # unsupported architecture
      exit 1; \
    fi && \
    curl -L \
      -H "Accept:application/octet-stream" \
      -H "X-GitHub-Api-Version: 2022-11-28" \
      https://api.github.com/repos/grpc-ecosystem/grpc-health-probe/releases/assets/${ASSET_ID} \
      -o grpc_health_probe \
    && chmod +x grpc_health_probe

# Stage 2: Run the application
FROM eclipse-temurin:21-jdk-alpine-3.21 AS final

################ Plugin System Setup ################
# Install dependencies
RUN apk add --no-cache python3 python3-dev py3-pip gcc musl-dev
# Create venv so pip can be used
RUN python3 -m venv /app/python-env
# Manually activate venv
ENV VIRTUAL_ENV=/app/python-env
ENV PATH=/app/python-env/bin:$PATH
# Install jep and requests
RUN pip3 install --no-cache jep requests
################ Plugin System Setup ################

WORKDIR /app

# Run the application as a non-root user.
RUN adduser -D backend-user && chown -R backend-user /app

# Copy built jar file
COPY --from=build /app/build/libs/snowballr-backend-*.jar app.jar
# Copy grpc_health_probe
COPY --from=build /app/grpc_health_probe grpc_health_probe

ENV PORT=8080
ENV PLUGIN_FETCHER_DIRECTORY=/app/plugins/fetchers

# Healthcheck uses grpc-health-probe
HEALTHCHECK CMD ./grpc_health_probe -addr=localhost:${PORT} -service "snowballr.SnowballR"

RUN apk add --no-cache runuser

# Start execute the jar file when starting the container
# `chown` workaround needed because /app/plugins would else be mounted as root
# and thus not accessible by backend-user.
ENTRYPOINT chown -R backend-user /app/plugins && exec runuser -u backend-user -- java -jar app.jar
