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

- \<no-arguments\>: Starts the backend, its proxy and the database.
- `db-only`: Only starts the database (for local development)
- `registry`: Starts the published backend image with the specified tag, its proxy and the database (use the BACKEND_TAG
  env variable).
- `proxy-only`: Only starts the proxy (for local development)

Use `docker compose --profile <profile> up` to start the backend with the desired profile.

The proxy is used to enable gRPC-Web support for the backend. It listens on the port specified by the `WEB_PORT`
environment variable (default: `8081`).

## Environment Variables

The app's configuration is driven by the `PROFILE` environment variable, which sets sensible defaults for different
environments. You can specify `PROFILE` and other variables in a `.env` file in the root directory. Either create the
file manually or copy the provided example:

```bash
cp .env.example .env
```

### Configuration Profiles

The `PROFILE` variable determines the default behavior of the application.

- `PRODUCTION` (Default): For live deployments. Requires explicit configuration for critical settings like PORT and
  DATABASE_HOST. Disables all development helpers.
- `DEVELOPMENT`: For local development. Provides convenient defaults for the server and database and automatically
  seeds a dummy user for easy interaction with the API.
- `TESTING`: For local manual testing. Similar to development, but enables authentication bypass for easier testing of
  authenticated endpoints.

### Environment Variables Table

| Variable                              |               Required               |    Default    | Description                                                                       |
|---------------------------------------|:------------------------------------:|:-------------:|-----------------------------------------------------------------------------------|
| `PROFILE`                             |                 :x:                  | `PRODUCTION`  | Sets the configuration profile (`PRODUCTION`, `DEVELOPMENT`, `TESTING`).          |
| `PORT`                                | :white_check_mark: (in `PRODUCTION`) |     8080      | The port where the backend is served.                                             |
| `DATABASE_HOST`                       | :white_check_mark: (in `PRODUCTION`) |  `localhost`  | Hostname of the database connection.                                              |
| `LOG_LEVEL`                           |                 :x:                  | Profile-based | The log level to use. One of `TRACE`, `DEBUG`, `INFO`, `WARN`, `ERROR`, or `OFF`. |
| `AUTH_BYPASS_ENABLED`                 |                 :x:                  | Profile-based | Bypasses authentication and uses the dummy user for all whitelisted requests.     |
| `DATABASE_SEED_USER_ENABLED`          |                 :x:                  | Profile-based | Inserts a dummy user into the database on startup.                                |
| `DATABASE_PASSWORD`                   |          :white_check_mark:          |       -       | Password for the database e.g. `postgres_password`.                               |
| `JWT_PRIVATE_KEY_BASE64`              |          :white_check_mark:          |       -       | Base64 encoded private key for JWT authentication.                                |
| `JWT_PUBLIC_KEY_BASE64`               |          :white_check_mark:          |       -       | Base64 encoded public key for JWT authentication.                                 |
| `SMTP_HOST`                           |          :white_check_mark:          |       -       | SMTP host for sending emails.                                                     |
| `SMTP_PORT`                           |          :white_check_mark:          |       -       | SMTP port for sending emails.                                                     |
| `SMTP_USER`                           |                 :x:                  |       -       | SMTP user for sending emails.                                                     |
| `SMTP_PASSWORD`                       |                 :x:                  |       -       | SMTP password for sending emails.                                                 |
| `SMTP_TRANSPORT_LOGGING_ONLY_ENABLED` |                 :x:                  | Profile-based | SMTP transport logging to only log emails instead of actually sending them.       |
| `SMTP_SENDER_NAME`                    |          :white_check_mark:          |       -       | Name of the sender for emails.                                                    |
| `SMTP_SENDER_EMAIL`                   |          :white_check_mark:          |       -       | Email address of the sender for emails.                                           |
| `WEB_PORT`                            |                 :x:*                 |     8081      | The port where the proxy is served (used for gRPC-Web).                           |
| `BACKEND_TAG`                         |                 :x:*                 | `latest-dev`  | Tag of registry backend image to use for `registry` docker compose profile.       |

### JWT Private/Public Key

To generate a **Base64-encoded RSA private/public key pair** for use in the `.env` file, follow these steps:

1. Generate a 2048-bit RSA private key

   ```bash
   openssl genpkey -algorithm RSA -out private_key.pem -pkeyopt rsa_keygen_bits:2048
   ```

2. Extract the corresponding public key

    ```bash
   openssl rsa -pubout -in private_key.pem -out public_key.pem
    ```

3. Encode the private key in Base64 (single line)

    ```bash
   echo "JWT_PRIVATE_KEY_BASE64=$(grep -v -- "-----" private_key.pem | tr -d '\n')" >> .env
    ```

4. Encode the public key in Base64 (single line)

    ```bash
    echo "JWT_PUBLIC_KEY_BASE64=$(grep -v -- "-----" public_key.pem | tr -d '\n')" >> .env
    ```

> **Attention**
>
> The `JWT_PRIVATE_KEY_BASE64` and `JWT_PUBLIC_KEY_BASE64` environment variables must **always remain the same** across
> deployments, unless a proper key rotation process is implemented.
>
> These keys are used to **sign and verify JWTs**. If you change them, any existing JWTs signed with the old private key
> will become invalid, and users will be forced to re-authenticate because the system can no longer verify their tokens.

## Building from Source

To build the project from source, run the following commands:

```bash
git clone git@github.com:SE-UUlm/snowballr-backend.git
cd snowballr-backend
git submodule update --init --recursive
./gradlew shadowJar
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
