# SnowballR Backend

This is the backend of the SnowballR application.

## Commands

### Starting the server

First, provide a `.env` file with all required environment variables shown in the section below.

As the server assumes a database to be running, we first need to start the database.
The best way to do this is to use the docker compose file. Run it with `docker compose up`.
If the database is up and running, we can start the server. This can either be done by executing the built JAR file or
by using the Gradle command:

```bash
java -jar build/libs/snowballr-backend-<version>.jar
# or
./gradlew run
```

If you're using IntelliJ IDEA, you can use the added run configuration "Run Backend", which does the same as executing
`./gradlew run`.

### Formatting

```bash
# Format files
./gradlew formatKotlin

# Verify formatting
./gradlew lintKotlin
```

### Linting

```bash
./gradlew detekt
```

### Testing

```bash
./gradlew test
```

### Build Jar

```bash
./gradlew jar
# Jar can be found in build/libs/snowballr-backend-<version>.jar
```

## Environment Variables

| Variable            |      Required      | Default | Description                                                                      |
|---------------------|:------------------:|:-------:|----------------------------------------------------------------------------------|
| `PORT`              | :white_check_mark: |    -    | The port where the backend is served                                             |
| `LOG_LEVEL`         |        :x:         | `DEBUG` | The log level to use. One of `TRACE`, `DEBUG`, `INFO`, `WARN`, `ERROR`, or `OFF` |
| `DATABASE_PASSWORD` | :white_check_mark: |    -    | Password for the database e.g. `postgres_password`                               |
