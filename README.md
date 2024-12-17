# SnowballR Backend

Backend deno/nodejs server written in typescript.

## Run

```bash
docker compose --profile run -f unitTests/docker-compose.yml --env-file unitTests/.env up --abort-on-container-exit
```

## Tests

The tests require a running postgres database.

## Run in Docker

```bash
docker compose --profile test -f unitTests/docker-compose.yml --env-file unitTests/.env up --abort-on-container-exit
```
