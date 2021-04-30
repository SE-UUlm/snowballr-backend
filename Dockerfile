FROM ubuntu

# install required packages
RUN apt-get -qq update && apt-get -qq install -y ca-certificates curl unzip --no-install-recommends

# installing deno
RUN curl -fsSL https://deno.land/x/install/install.sh | sh

# clean installed packages
RUN apt-get -qq remove -y --purge ca-certificates curl unzip && apt-get clean

# expose the port of deno 80 to the external world
EXPOSE 8000

# copying all files of the repository into the /app folder to execute deno there
COPY . /app

# change to the /app directory
WORKDIR /app

# exporting binary installation folder for deno
RUN cp /root/.deno/bin/deno /bin

# command that executes deno
CMD ["/bin/deno", "run", "--allow-net", "main.ts"]
