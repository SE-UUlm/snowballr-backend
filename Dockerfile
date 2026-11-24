# Stage 1: Build the application
FROM gradle:9.8.2-jdk25-alpine AS build

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
COPY api/grpc/proto api/grpc/proto
RUN --mount=type=cache,target=/root/.gradle \
    gradle shadowJar --no-daemon --stacktrace

# Stage 2: Provide uv binary
FROM ghcr.io/astral-sh/uv:0.13.6 AS uv

# Stage 3: Generate the gRPC API documentation (static HTML, served by the backend under /grpc-docs)
# The sabledocs image is amd64-only; its output is architecture-independent, so always run it on the build platform.
FROM --platform=$BUILDPLATFORM markvincze/sabledocs:0.16.893-slim AS grpc-docs

WORKDIR /build
COPY api/grpc/sabledocs.toml ./
COPY api/grpc/docs docs
COPY api/grpc/patches patches
COPY api/grpc/proto proto

WORKDIR /build/proto
RUN protoc ./*.proto -o ../descriptor.pb --include_source_info

WORKDIR /build
RUN pip3 install pypatch==1.0.2 markdown-mermaidjs==2.0.0 \
    && pypatch apply ./patches/sabledocs_fix_nested.patch sabledocs \
    && pypatch apply ./patches/sabledocs_nested_links.patch sabledocs \
    && pypatch apply ./patches/sabledocs_nested_names.patch sabledocs \
    && pypatch apply ./patches/sabledocs_fix_arguments.patch sabledocs \
    && pypatch apply ./patches/sabledocs_auth_tag.patch sabledocs \
    && pypatch apply ./patches/markdown_mermaidjs_no_script.patch markdown_mermaidjs \
    && sabledocs

# Stage 4: Run the application
FROM eclipse-temurin:27-jre-alpine-3.24 AS final

WORKDIR /app

RUN adduser -D backend-user

COPY --from=uv /uv /bin/uv

ENV PORT=8080
ENV PLUGIN_DIRECTORY=/app/plugins/
ENV PYTHON_EXECUTABLE=/app/.venv/bin/python3

# Install python and fetcher dependencies using uv
COPY --chown=backend-user:backend-user requirements.txt .
RUN apk add --no-cache python3
RUN uv venv /app/.venv
RUN --mount=type=cache,target=/root/.cache/uv \
    uv pip install --python /app/.venv/bin/python -r requirements.txt
RUN chown -R backend-user /app/.venv

RUN mkdir -p /app/plugins && chown backend-user:backend-user /app/plugins
USER backend-user

VOLUME /app/plugins/

# Copy the generated gRPC API documentation (see GrpcDocsConfig)
COPY --chown=backend-user:backend-user --from=grpc-docs /build/sabledocs_output /app/grpc-docs

# Copy build artifacts last — source changes only invalidate layers below this point
COPY --chown=backend-user:backend-user --from=build /app/build/libs/snowballr-backend-*.jar app.jar

EXPOSE 8090

# Start execute the jar file when starting the container
ENTRYPOINT ["java", "-jar", "app.jar"]
