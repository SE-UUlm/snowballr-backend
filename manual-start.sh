#!/bin/bash

export $(cat manual-start/.env | xargs)
echo "Building docker image..."
docker build -t backend-image .
echo "Starting backend development containers..."
docker-compose -f manual-start/docker-compose.yml --env-file manual-start/.env --profile all up -d --force-recreate backend