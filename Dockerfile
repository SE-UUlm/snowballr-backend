# Stage 1: Build the application
FROM gradle:9.6.1-jdk25-alpine AS build

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

# Stage 2: Provide uv binary
FROM ghcr.io/astral-sh/uv:0.11.7 AS uv

# Stage 3: Run the application
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

RUN mkdir -p /app/plugins && chown backend-user:backend-user /app/plugins
USER backend-user

VOLUME /app/plugins/

# Copy build artifacts last — source changes only invalidate layers below this point
COPY --chown=backend-user:backend-user --from=build /app/build/libs/snowballr-backend-*.jar app.jar

EXPOSE 8090

# Start execute the jar file when starting the container
ENTRYPOINT ["java", "-jar", "app.jar"]
