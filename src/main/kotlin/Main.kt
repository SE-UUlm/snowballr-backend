package se.uulm.snowballr.backend

fun main() {
    val server = SnowballRServer(8080)
    server.start()
    server.blockUntilShutdown()
}
