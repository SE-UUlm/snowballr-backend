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

| Variable            |      Required      | Default | Description                                                                      |
|---------------------|:------------------:|:-------:|----------------------------------------------------------------------------------|
| `PORT`              | :white_check_mark: |    -    | The port where the backend is served                                             |
| `LOG_LEVEL`         |        :x:         | `DEBUG` | The log level to use. One of `TRACE`, `DEBUG`, `INFO`, `WARN`, `ERROR`, or `OFF` |
| `DATABASE_PASSWORD` | :white_check_mark: |    -    | Password for the database e.g. `postgres_password`                               |

## Building from Source

See [our wiki](https://github.com/SE-UUlm/snowballr-backend/wiki/Getting-Started) to build the project from source.
