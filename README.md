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

| Variable | Description                          |
|----------|--------------------------------------|
| `PORT`   | The port where the backend is served |
