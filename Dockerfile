# Stage 1: Build the application
FROM gradle:9.3.1-jdk21-alpine AS build

# Asset IDs of v0.4.38 release of grpc-health-probe
# https://github.com/grpc-ecosystem/grpc-health-probe/releases/tag/v0.4.38
ARG AMD64_ID=251600596
ARG ARM64_ID=251600609

WORKDIR /app

RUN apk add libc6-compat curl

# Copy build files only — dependency resolution re-runs only when these change
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY gradle gradle

# Resolve and cache all dependencies before copying source
RUN --mount=type=cache,target=/root/.gradle \
    gradle dependencies --no-daemon

# Copy the project's source code and proto files and build
COPY src/main src/main
RUN --mount=type=cache,target=/root/.gradle \
    gradle shadowJar --no-daemon --stacktrace

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

# Stage 2: Provide uv binary
FROM ghcr.io/astral-sh/uv:0.11.7 AS uv

# Stage 3: Run the application
FROM eclipse-temurin:21-jre-alpine-3.21 AS final

WORKDIR /app

# Copy built jar file
COPY --from=build /app/build/libs/snowballr-backend-*.jar app.jar
# Copy grpc_health_probe
COPY --from=build /app/grpc_health_probe grpc_health_probe
COPY --from=uv /uv /bin/uv

ENV PORT=8080
ENV PLUGIN_DIRECTORY=/app/plugins/
ENV PYTHON_EXECUTABLE=/app/.venv/bin/python3

VOLUME /app/plugins/

# Healthcheck uses grpc-health-probe
HEALTHCHECK CMD ./grpc_health_probe -addr=localhost:${PORT} -service "snowballr.SnowballR"

# Install python and fetcher dependencies using uv
COPY requirements.txt .
RUN apk add --no-cache python3 libcurl
RUN uv venv /app/.venv
# Needed for pycurl
ENV PYCURL_SSL_LIBRARY=openssl
RUN apk add --no-cache --virtual .py-build-deps python3-dev build-base curl-dev
RUN --mount=type=cache,target=/root/.cache/uv \
    uv pip sync --python /app/.venv/bin/python requirements.txt
RUN apk del .py-build-deps

# Run the application as a non-root user.
RUN adduser -D backend-user && chown -R backend-user /app
USER backend-user

# Start execute the jar file when starting the container
ENTRYPOINT ["java", "-jar", "app.jar"]
