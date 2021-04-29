FROM ubuntu

# installing deno
RUN curl -fsSL https://deno.land/x/install/install.sh | sh

# expose the port of deno 80 to the external world
EXPOSE 8000

# copying all files of the repository into the /app folder to execute deno there
COPY . /app

# change to the /app directory
WORKDIR /app

# command that executes deno
CMD ["~/.deno/bin/deno", "run", "--allow-net", "main.ts"]
