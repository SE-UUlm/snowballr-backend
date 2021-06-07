#!/bin/bash

docker-compose -f ../docker/docker-compose.yml --profile min up -d
sleep 1s
#BACKEND_ID=$(docker-compose ps -q backend)
mkdir -p cov_profile
for f in ./cov_profile/*; do rm -f "$f"; done
#docker cp "$BACKEND_ID":cov_profile ./
deno test --allow-env --allow-read --allow-net --coverage=cov_profile --unstable
deno --unstable coverage cov_profile --lcov > cov_profile.lcov
genhtml -o ./html cov_profile.lcov
docker-compose -f ../docker/docker-compose.yml --profile min down