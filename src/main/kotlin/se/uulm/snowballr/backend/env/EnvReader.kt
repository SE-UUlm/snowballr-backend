package se.uulm.snowballr.backend.env

// Default values
const val DEFAULT_LOG_LEVEL = "DEBUG"
const val DEFAULT_DATABASE_HOST = "localhost"

// Http
private const val PORT = "PORT"

// Miscellaneous
private const val LOG_LEVEL = "LOG_LEVEL"

// Database
private const val DATABASE_PASSWORD = "DATABASE_PASSWORD"
private const val DATABASE_HOST = "DATABASE_HOST"

// Encryption
private const val JWT_PRIVATE_KEY_BASE64 = "JWT_PRIVATE_KEY_BASE64"
private const val JWT_PUBLIC_KEY_BASE64 = "JWT_PUBLIC_KEY_BASE64"

class EnvReader(
    envService: IEnvService,
) {
    val env = Env(
        http = Env.Http(
            port = envService[PORT].toInt(),
        ),
        miscellaneous = Env.Miscellaneous(
            logLevel = envService.getOrDefault(LOG_LEVEL, DEFAULT_LOG_LEVEL),
        ),
        database = Env.Database(
            password = envService[DATABASE_PASSWORD],
            host = envService.getOrDefault(DATABASE_HOST, DEFAULT_DATABASE_HOST),
        ),
        encryption = Env.Encryption(
            jwtPrivateKeyBase64 = envService[JWT_PRIVATE_KEY_BASE64],
            jwtPublicKeyBase64 = envService[JWT_PUBLIC_KEY_BASE64],
        ),
    )
}
