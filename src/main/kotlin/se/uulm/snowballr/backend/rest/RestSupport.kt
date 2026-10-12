package se.uulm.snowballr.backend.rest

import kotlinx.coroutines.runBlocking
import se.uulm.snowballr.backend.context.RequestContext

internal fun <T> onRequest(block: suspend () -> T): T = runBlocking(RequestContext.current()) { block() }
