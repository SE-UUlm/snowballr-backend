# snowballr-backend

SnowballR Backend

## Commands

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

| Variable    |      Required      | Default | Description                                                                      |
|-------------|:------------------:|:-------:|----------------------------------------------------------------------------------|
| `PORT`      | :white_check_mark: |    -    | The port where the backend is served                                             |
| `LOG_LEVEL` |        :x:         | `DEBUG` | The log level to use. One of `TRACE`, `DEBUG`, `INFO`, `WARN`, `ERROR`, or `OFF` |
