The app's configuration is driven by environment variables. The `PROFILE` variable sets sensible defaults for different
environments, but all settings can be overridden individually.

You can specify variables in a `.env` file in the root directory. To get started, copy the provided example:

```bash
cp .env.example .env
```

## Configuration Profiles

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

\* only used when using the docker compose profiles.

#### Profile-based Defaults

| Profile                               | `PRODUCTION` | `DEVELOPMENT` | `TESTING`   |
|---------------------------------------|--------------|---------------|-------------|
| `PORT`                                | -            | 8080          | 8080        |
| `DATABASE_HOST`                       | -            | `localhost`   | `localhost` |
| `LOG_LEVEL`                           | `INFO`       | `DEBUG`       | `TRACE`     |
| `AUTH_BYPASS_ENABLED`                 | `false`      | `false`       | `true`      |
| `DATABASE_SEED_USER_ENABLED`          | `false`      | `true`        | `true`      |
| `SMTP_TRANSPORT_LOGGING_ONLY_ENABLED` | `false`      | `true`        | `true`      |

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

> [!CAUTION]: Key Stability
>
> The `JWT_PRIVATE_KEY_BASE64` and `JWT_PUBLIC_KEY_BASE64` environment variables must **always remain the same** across
> deployments, unless a proper key rotation process is implemented.
>
> These keys are used to **sign and verify JWTs**. If you change them, any existing JWTs signed with the old private key
> will become invalid, and users will be forced to re-authenticate because the system can no longer verify their tokens.

## Authentication Bypass and User Seeding

For local development and manual testing, we provide helpers to simplify working with authenticated endpoints. These are
controlled by the `PROFILE` environment variable or the `AUTH_BYPASS_ENABLED` and `DATABASE_SEED_USER_ENABLED` flags.

### `DATABASE_SEED_USER_ENABLED`

- **What it does**: When set to `true`, the application will insert a pre-defined "dummy" user into the database on
  startup. If set to `false`, it will ensure the dummy user is removed.
- **Purpose**: This ensures the user account needed for development or testing exists without requiring manual
  registration.
- **Default Behavior**: Enabled in `DEVELOPMENT` and `TESTING` profiles.

### `AUTH_BYPASS_ENABLED`

- **What it does**: When set to `true`, the
  [AuthenticationInterceptor](https://github.com/SE-UUlm/snowballr-backend/blob/develop/src/main/kotlin/se/uulm/snowballr/backend/grpc/interceptor/AuthenticationInterceptor.kt)
  will automatically treat all incoming whitelisted requests as if they were made by the dummy user, bypassing JWT
  validation.
- **Purpose**: This is ideal for local manual testing of authenticated endpoints, as you don't need to handle login
  flows or manage JWTs in your API client (e.g., Postman, grpcurl).
- **Default Behavior**: Enabled in the `TESTING` profile only.

### How They Work Together

- Enabling `AUTH_BYPASS_ENABLED` automatically enables `DATABASE_SEED_USER_ENABLED`, because the user must exist in the
  database for the bypass to function.
- In the `DEVELOPMENT` profile, `DATABASE_SEED_USER_ENABLED` is on but `AUTH_BYPASS_ENABLED` is off. This is useful for
  developing features related to login and authentication, as you can log in with the known dummy user's credentials.
- In the `TESTING` profile, both are on, allowing you to directly call authenticated endpoints without logging in first.

> [!NOTE]: Restricting Allowed Calls
>
> The authentication bypass does not grant unrestricted access. The `authenticationInterceptor` maintains a whitelist of
> gRPC methods that can be called when `AUTH_BYPASS_ENABLED` is active.
>
> To modify this list, update the `AUTH_BYPASS_ENABLED` set in the authenticationInterceptor. Ensure that only endpoints
> required for local development or testing are permitted to avoid inadvertently bypassing critical authorization logic.
