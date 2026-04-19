This guide provides instructions on how to set up and run the SnowballR backend on your local machine. You can either
use the provided Docker setup for a quick start or build the project from source for more control.

Before you begin, ensure you have configured the necessary environment variables. For a detailed guide on all
configuration options, see the **[Configuration](https://github.com/SE-UUlm/snowballr-backend/wiki/Configuration.)**
page.

## Docker Setup

The fastest way to get started is to use the provided Docker setup.

1. Clone the repository:

    ```bash
    git clone git@github.com:SE-UUlm/snowballr-backend.git
    cd snowballr-backend
    ```

2. Set up your environment variables by copying the example file:

    ```bash
    cp .env.example .env
    ```

3. Start the services using Docker Compose:

    ```bash
    docker compose up
    ```

> [!INFO]
> If you encounter a permission-denied error regarding the plugin directory not
> being writable, make sure to execute `chmod o+wr ./plugins` on the created
> directory once. In some cases, Docker's created volumes aren't writable for
> the user within the container.

The proxy is used to enable gRPC-Web support for the backend. It listens on the port specified by the `WEB_PORT`
environment variable (default: `8081`).

### Docker Compose Profiles

We provide several docker compose profiles for different setups. Use `docker compose --profile <profile> up` to start
the backend with the desired profile.

- **\<no-arguments\>**: Starts the backend, its proxy and the database.
- **`db-only`**: Only starts the database (for local development)
- **`registry`**: Starts the published backend image with the specified tag, its proxy and the database (use the
  BACKEND_TAG
  env variable).
- **`proxy-only`**: Only starts the proxy (for local development)

## Building from Source

To build the project from source, run the following commands:

1. Clone the repository:

    ```bash
    git clone git@github.com:SE-UUlm/snowballr-backend.git
    cd snowballr-backend
    ```

2. Build the project using the Gradle wrapper. This will create a JAR file.

    ```bash
    ./gradlew shadowJar
    ```

   The built JAR file can be found in the `build/libs` directory, named `snowballr-backend-<version>.jar`.

3. Setup Python dependencies for the plugin system using
   [uv](https://docs.astral.sh/uv/):

    ```bash
    uv venv .venv
    uv pip sync --python .venv/bin/python3 requirements.txt
    ```

4. Run the application:

    ```bash
    PYTHON_EXECUTABLE=.venv/bin/python3 java -jar build/libs/snowballr-backend-<version>.jar
    ```

    Remember to provide the environment variables either via a `.env` file or by writing them in front of the command.

> [!IMPORTANT]
> Always ensure that the server is running in a python environment providing the required dependencies specified in the `requirements.txt`.

### Running in an IDE

If you want to run the project in an IDE, you can import it as a Gradle project. The Gradle wrapper is included, so you
can run Gradle commands directly from the project root directory. For example, to run the server, you can use:

```bash
./gradlew run
```

This command automatically prepares `.venv` and synchronizes `requirements.txt`
with `uv` before startup.

If you're using IntelliJ IDEA, you can use the provided run configuration "Run Backend", which does the same as
executing `./gradlew run`.
