FROM ubuntu:focal

# install required packages
RUN apt-get -qq update && apt-get -qq install -y ca-certificates curl unzip --no-install-recommends

# installing deno
RUN curl -fsSL https://deno.land/x/install@v0.1.7/install.sh | sh

# clean installed packages
RUN apt-get -qq remove -y --purge ca-certificates curl unzip  && apt-get clean

# expose the port of deno 80 to the external world
EXPOSE 80

# copying all files of the repository into the /app folder to execute deno there
COPY ./src/ /app
COPY ./deno.json /app

# copying nessie stuff for db migrations
COPY ./nessie.config.ts /nessie/
COPY ./db/migrations/ /nessie/db/migrations/
COPY ./db/seeds /nessie/db/seeds/

# change to the /app directory
WORKDIR /app/

# exporting binary installation folder for deno
RUN cp /root/.deno/bin/deno /bin

# command that executes deno
CMD ["/bin/deno", "run", "--allow-net", "--allow-env", "--no-check", "--allow-read", "--allow-write", "--allow-import", "--unstable", "main.ts"]
