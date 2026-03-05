# Asset IDs of v0.4.38 release of grpc-health-probe
# https://github.com/grpc-ecosystem/grpc-health-probe/releases/tag/v0.4.38
ARG AMD64_ID=251600596
ARG ARM64_ID=251600609

# Stage 1: Build the application
FROM gradle:9.3.1-jdk25-alpine AS build

WORKDIR /app

# libc6-compat: provide glibc compatibility for prebuilt binaries
RUN apk add libc6-compat

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

# Stage 2: Download grpc-health-probe (runs in parallel with build)
FROM alpine:3.21 AS grpc-health-probe

ARG AMD64_ID
ARG ARM64_ID

# Download binaries of grpc-health-probe based on the architecture and make them executable
RUN apk add --no-cache curl && \
    ARCH=$(uname -m) && \
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
      -o /grpc_health_probe \
    && chmod +x /grpc_health_probe

# Stage 3: Provide uv binary
FROM ghcr.io/astral-sh/uv:0.11.7 AS uv

# Stage 4: Run the application
FROM eclipse-temurin:25-jre-alpine-3.23 AS final

WORKDIR /app

RUN adduser -D backend-user

COPY --from=uv /uv /bin/uv

ENV PORT=8080
ENV PLUGIN_DIRECTORY=/app/plugins/
ENV PYTHON_EXECUTABLE=/app/.venv/bin/python3

# Install python and fetcher dependencies using uv
COPY --chown=backend-user:backend-user requirements.txt .
RUN apk add --no-cache python3 libcurl
RUN uv venv /app/.venv
# Needed for pycurl
ENV PYCURL_SSL_LIBRARY=openssl
RUN apk add --no-cache --virtual .py-build-deps python3-dev build-base curl-dev
RUN --mount=type=cache,target=/root/.cache/uv \
    uv pip install --python /app/.venv/bin/python -r requirements.txt
RUN apk del .py-build-deps && chown -R backend-user /app/.venv

VOLUME /app/plugins/

# Healthcheck uses grpc-health-probe
HEALTHCHECK CMD ./grpc_health_probe -addr=localhost:${PORT} -service "snowballr.SnowballR"

# Copy build artifacts last — source changes only invalidate layers below this point
COPY --chown=backend-user:backend-user --from=build /app/build/libs/snowballr-backend-*.jar app.jar
COPY --chown=backend-user:backend-user --from=grpc-health-probe /grpc_health_probe grpc_health_probe

USER backend-user

# Start execute the jar file when starting the container
ENTRYPOINT ["java", "-jar", "app.jar"]
