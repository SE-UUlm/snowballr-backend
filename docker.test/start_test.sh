#!/bin/bash

docker-compose --profile all up --exit-code-from backend
BACKEND_ID=$(docker-compose ps -q backend)
mkdir -p cov_profile
for f in ./cov_profile/*; do rm -f "$f"; done
docker cp "$BACKEND_ID":cov_profile ./
deno --unstable coverage cov_profile --lcov > cov_profile.lcov
genhtml -o /html cov_profile.lcov
