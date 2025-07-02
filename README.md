# SnowballR Backend

This is the backend of the SnowballR application.
Have a look at the [wiki of our frontend repo](https://github.com/SE-UUlm/snowballr-frontend/wiki) to learn more about
SLRs and Snowballing. Also have a look at the [wiki of this repo](https://github.com/SE-UUlm/snowballr-backend/wiki) to
find about our architecture, how to get the backend started, and how to contribute.

## Getting Started

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

The app requires a set of environment variables to run. You can set them in a `.env` file in the root directory of the
project. Either create the file manually or copy the provided example:

```bash
cp .env.example .env
```

The environment variables are as follows:

| Variable                 |      Required      |   Default    | Description                                                                      |
|--------------------------|:------------------:|:------------:|----------------------------------------------------------------------------------|
| `PORT`                   | :white_check_mark: |      -       | The port where the backend is served                                             |
| `WEB_PORT`               |        :x:*        |     8081     | The port where the proxy is served (used for gRPC-Web)                           |
| `LOG_LEVEL`              |        :x:         |   `DEBUG`    | The log level to use. One of `TRACE`, `DEBUG`, `INFO`, `WARN`, `ERROR`, or `OFF` |
| `DATABASE_PASSWORD`      | :white_check_mark: |      -       | Password for the database e.g. `postgres_password`                               |
| `DATABASE_HOST`          |        :x:         | `localhost`  | Hostname of database connection                                                  |
| `BACKEND_TAG`            |        :x:*        | `latest-dev` | Tag of registry backend image to use for `registry` docker compose profile       |
| `JWT_PRIVATE_KEY_BASE64` | :white_check_mark: |      -       | Base64 encoded private key for JWT authentication                                |
| `JWT_PUBLIC_KEY_BASE64`  | :white_check_mark: |      -       | Base64 encoded public key for JWT authentication                                 |

\* only used when using the docker compose profiles.

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

See [our wiki](https://github.com/SE-UUlm/snowballr-backend/wiki/Getting-Started) to build the project from source.
