package se.uulm.snowballr.backend.grpc

import io.github.oshai.kotlinlogging.KotlinLogging
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor

private val logger = KotlinLogging.logger {}

val loggingInterceptor =
    object : ServerInterceptor {
        override fun <ReqT : Any?, RespT : Any?> interceptCall(
            call: ServerCall<ReqT?, RespT?>?,
            headers: Metadata?,
            next: ServerCallHandler<ReqT?, RespT?>?,
        ): ServerCall.Listener<ReqT?>? {
            logger.info { "Received call to ${call?.methodDescriptor?.fullMethodName}" }
            return next?.startCall(call, headers)
        }
    }
