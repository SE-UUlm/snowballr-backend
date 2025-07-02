package se.uulm.snowballr.backend.model.auth

import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler

/**
 * Represents the state of an authentication request in a gRPC server call.
 *
 * @param ReqT The type of the request message.
 * @param RespT The type of the response message.
 * @property call The server call associated with the request.
 * @property headers The metadata headers associated with the request.
 * @property next The next handler in the call chain, if any.
 */
data class AuthRequestState<ReqT, RespT>(
    val call: ServerCall<ReqT?, RespT?>,
    val headers: Metadata?,
    val next: ServerCallHandler<ReqT?, RespT?>?,
)
