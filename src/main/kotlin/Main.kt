package se.uulm.snowballr.backend

import se.uulm.snowballr.backend.env.Env

fun main() {
    val env = Env()
    val server = SnowballRServer(env.http.port)
    server.start()
    server.blockUntilShutdown()
}
