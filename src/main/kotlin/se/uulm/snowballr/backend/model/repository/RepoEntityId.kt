package se.uulm.snowballr.backend.model.repository

import java.util.UUID

sealed interface RepoEntityId {
    fun asString(): String
}

interface SingleEntityId : RepoEntityId {
    val id: UUID

    override fun asString(): String = id.toString()
}

interface DualEntityId : RepoEntityId {
    val id1: UUID
    val id2: UUID

    override fun asString(): String = "$id1 and $id2"
}

/** ====== Single Entity IDs ====== */

data class AuthorId(override val id: UUID) : SingleEntityId

data class CriterionId(override val id: UUID) : SingleEntityId

data class InvitationTokenId(override val id: UUID) : SingleEntityId

data class PaperId(override val id: UUID) : SingleEntityId

data class ProjectId(override val id: UUID) : SingleEntityId

data class ReviewId(override val id: UUID) : SingleEntityId

data class UserId(override val id: UUID) : SingleEntityId

data class VerificationTokenId(override val id: UUID) : SingleEntityId

data class ProjectPaperId(override val id: UUID) : SingleEntityId

/** ====== Dual Entity IDs ====== */

data class AuthorOfPaperId(override val id1: UUID, override val id2: UUID) : DualEntityId

data class CitationId(override val id1: UUID, override val id2: UUID) : DualEntityId

data class ProjectMemberId(override val id1: UUID, override val id2: UUID) : DualEntityId

data class ReadingListId(override val id1: UUID, override val id2: UUID) : DualEntityId

data class ReviewHasCriterionId(override val id1: UUID, override val id2: UUID) : DualEntityId
