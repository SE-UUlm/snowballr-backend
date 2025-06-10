The fastest way to get started is to use the provided Docker setup. To do so, run the following commands:

```bash
git clone git@github.com:SE-UUlm/snowballr-backend.git
cd snowballr-backend
git submodule update --init --recursive
docker compose up
```

Be sure to have the environment variables set or create a `.env` file in the root directory of the project (see
[below](#environment-variables)).

We provide several docker compose profiles for different setups.

- \<no-arguments\>: Starts the backend together with the database.
- `db-only`: Only starts the database (for local development)
- `latest`: Starts the published backend image with the 'latest' tag together with the database.
- `latest-dev`: Starts the published backend image with the 'latest-dev' tag together with the database.

Use `docker compose --profile <profile> up` to start the frontend with the desired profile.

## Environment Variables

The app requires a set of environment variables to run. You can set them in a `.env` file in the root directory of the
project. Either create the file manually or copy the provided example:

```bash
cp .env.example .env
```

The environment variables are as follows:

| Variable            |      Required      |   Default   | Description                                                                      |
|---------------------|:------------------:|:-----------:|----------------------------------------------------------------------------------|
| `PORT`              | :white_check_mark: |      -      | The port where the backend is served                                             |
| `LOG_LEVEL`         |        :x:         |   `DEBUG`   | The log level to use. One of `TRACE`, `DEBUG`, `INFO`, `WARN`, `ERROR`, or `OFF` |
| `DATABASE_PASSWORD` | :white_check_mark: |      -      | Password for the database e.g. `postgres_password`                               |
| `DATABASE_HOST`     |        :x:         | `localhost` | Hostname of database connection                                                  |

## Building from Source

To build the project from source, run the following commands:

```bash
git clone git@github.com:SE-UUlm/snowballr-backend.git
cd snowballr-backend
git submodule update --init --recursive
./gradlew jar
```

The built JAR file can be found in the `build/libs` directory, named `snowballr-backend-<version>.jar`.

In comparison to the frontend repository, we don't have to manually generate the API code, as it is done automatically.
You can find the generated API code in the `build/generated` directory.

The JAR file can be executed with the following command:

```bash
java -jar build/libs/snowballr-backend-<version>.jar
```

Remember to provide the environment variables either via a `.env` file or by writing them in front of the command.

If you want to run the project in an IDE, you can import it as a Gradle project. The Gradle wrapper is included, so you
can run Gradle commands directly from the project root directory. For example, to run the server, you can use:

```bash
./gradlew run
```

If you're using IntelliJ IDEA, you can use the provided run configuration "Run Backend", which does the same as
executing `./gradlew run`.
