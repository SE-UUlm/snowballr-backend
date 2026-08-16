package se.uulm.snowballr.backend.grpc

import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.parseUUID
import snowballr.Base
import java.util.UUID

/**
 * Shared gRPC helper utilities.
 *
 * This object provides:
 * - Wrappers that translate service-layer results into gRPC response types.
 * - Centralized parsing of [Base.Id] values with typed [EntityType] error reporting.
 */
internal object GrpcHelpers {
    /**
     * Executes a block and returns an empty gRPC response.
     *
     * @param block The action to run, typically a service call.
     * @return [Base.Nothing] as a canonical empty response.
     */
    suspend fun returnNothing(block: suspend () -> Unit): Base.Nothing {
        block()
        return Base.Nothing.getDefaultInstance()
    }

    /**
     * Executes a block and wraps its boolean result in a gRPC [Base.BoolValue].
     *
     * @param block The action that produces the boolean result.
     * @return [Base.BoolValue] containing the block result.
     */
    @Suppress("BooleanPropertyNaming")
    suspend fun returnBoolValue(block: suspend () -> Boolean): Base.BoolValue {
        val value = block()
        return Base.BoolValue.newBuilder().setValue(value).build()
    }

    fun parseUserId(id: Base.Id): UUID = parseUUID(id.id, EntityType.USER)

    fun parseProjectId(id: Base.Id): UUID = parseUUID(id.id, EntityType.PROJECT)

    fun parseProjectPaperId(id: Base.Id): UUID = parseUUID(id.id, EntityType.PROJECT_PAPER)

    fun parsePaperId(id: Base.Id): UUID = parseUUID(id.id, EntityType.PAPER)

    fun parseCriterionId(id: Base.Id): UUID = parseUUID(id.id, EntityType.CRITERION)

    fun parseReviewId(id: Base.Id): UUID = parseUUID(id.id, EntityType.REVIEW)
}
