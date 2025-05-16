package se.uulm.snowballr.backend

import se.uulm.snowballr.backend.env.Env
import se.uulm.snowballr.backend.grpc.SnowballRServer

fun main() {
    val env = Env()
    val server = SnowballRServer(env.http.port)
    server.start()
    server.blockUntilShutdown()
}
