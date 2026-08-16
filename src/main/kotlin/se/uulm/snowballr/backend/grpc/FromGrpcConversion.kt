@file:Suppress("TooManyFunctions")

package se.uulm.snowballr.backend.grpc

import se.uulm.snowballr.backend.model.dto.criterion.CriterionCategory
import se.uulm.snowballr.backend.model.dto.criterion.CriterionField
import se.uulm.snowballr.backend.model.dto.paper.PaperField
import se.uulm.snowballr.backend.model.dto.project.DecisionMatrixPattern
import se.uulm.snowballr.backend.model.dto.project.DecisionMatrixPatternEntry
import se.uulm.snowballr.backend.model.dto.project.ProjectField
import se.uulm.snowballr.backend.model.dto.project.ProjectInfoField
import se.uulm.snowballr.backend.model.dto.project.ProjectStatus
import se.uulm.snowballr.backend.model.dto.project.ReviewDecisionMatrix
import se.uulm.snowballr.backend.model.dto.project.SnowballingType
import se.uulm.snowballr.backend.model.dto.projectmember.MemberRole
import se.uulm.snowballr.backend.model.dto.projectpaper.PaperDecision
import se.uulm.snowballr.backend.model.dto.review.ReviewDecision
import se.uulm.snowballr.backend.model.dto.user.UserField
import se.uulm.snowballr.backend.model.dto.user.UserRole
import se.uulm.snowballr.backend.model.dto.user.UserStatus
import snowballr.CriterionOuterClass
import snowballr.ProjectOuterClass
import snowballr.ReviewOuterClass
import snowballr.UserOuterClass

private const val INVALID_CONVERSION_MESSAGE = "Invalid conversion"

fun criterionCategoryFromGrpc(category: CriterionOuterClass.CriterionCategory): CriterionCategory = when (category) {
    CriterionOuterClass.CriterionCategory.CRITERION_CATEGORY_INCLUSION -> CriterionCategory.INCLUSION
    CriterionOuterClass.CriterionCategory.CRITERION_CATEGORY_EXCLUSION -> CriterionCategory.EXCLUSION
    CriterionOuterClass.CriterionCategory.CRITERION_CATEGORY_HARD_EXCLUSION -> CriterionCategory.HARD_EXCLUSION
    CriterionOuterClass.CriterionCategory.UNRECOGNIZED,
    CriterionOuterClass.CriterionCategory.CRITERION_CATEGORY_UNSPECIFIED,
    ->
        @Suppress("UseCheckOrError")
        throw IllegalStateException(INVALID_CONVERSION_MESSAGE)
}

fun decisionMatrixPatternFromGrpc(pattern: ProjectOuterClass.ReviewDecisionMatrix.Pattern) = DecisionMatrixPattern(
    decision = paperDecisionFromGrpc(pattern.decision),
    entries = pattern.entriesList.map { decisionMatrixPatternEntryFromGrpc(it) },
)

fun decisionMatrixPatternEntryFromGrpc(entry: ProjectOuterClass.ReviewDecisionMatrix.Pattern.Entry) =
    DecisionMatrixPatternEntry(
        decision = reviewDecisionFromGrpc(entry.reviewDecision),
        count = entry.count.toInt(),
    )

fun reviewDecisionMatrixFromGrpc(decisionMatrix: ProjectOuterClass.ReviewDecisionMatrix) = ReviewDecisionMatrix(
    numberOfReviewers = decisionMatrix.numberOfReviewers,
    patterns = decisionMatrix.patternsList.map { decisionMatrixPatternFromGrpc(it) },
)

fun projectStatusFromGrpc(status: ProjectOuterClass.ProjectStatus): ProjectStatus = when (status) {
    ProjectOuterClass.ProjectStatus.PROJECT_STATUS_ACTIVE -> ProjectStatus.ACTIVE
    ProjectOuterClass.ProjectStatus.PROJECT_STATUS_ARCHIVED -> ProjectStatus.ARCHIVED
    ProjectOuterClass.ProjectStatus.PROJECT_STATUS_DELETED -> ProjectStatus.DELETED
    ProjectOuterClass.ProjectStatus.PROJECT_STATUS_ACTIVE_LOCKED -> ProjectStatus.ACTIVE_LOCKED
    ProjectOuterClass.ProjectStatus.UNRECOGNIZED,
    ProjectOuterClass.ProjectStatus.PROJECT_STATUS_UNSPECIFIED,
    ->
        @Suppress("UseCheckOrError")
        throw IllegalStateException(INVALID_CONVERSION_MESSAGE)
}

fun snowballingTypeFromGrpc(type: ProjectOuterClass.SnowballingType): SnowballingType = when (type) {
    ProjectOuterClass.SnowballingType.SNOWBALLING_TYPE_FORWARD -> SnowballingType.FORWARD
    ProjectOuterClass.SnowballingType.SNOWBALLING_TYPE_BACKWARD -> SnowballingType.BACKWARD
    ProjectOuterClass.SnowballingType.SNOWBALLING_TYPE_BOTH -> SnowballingType.BOTH
    ProjectOuterClass.SnowballingType.UNRECOGNIZED,
    ProjectOuterClass.SnowballingType.SNOWBALLING_TYPE_UNSPECIFIED,
    ->
        @Suppress("UseCheckOrError")
        throw IllegalStateException(INVALID_CONVERSION_MESSAGE)
}

fun memberRoleFromGrpc(role: ProjectOuterClass.MemberRole): MemberRole = when (role) {
    ProjectOuterClass.MemberRole.MEMBER_ROLE_DEFAULT -> MemberRole.DEFAULT
    ProjectOuterClass.MemberRole.MEMBER_ROLE_ADMIN -> MemberRole.ADMIN
    ProjectOuterClass.MemberRole.UNRECOGNIZED, ProjectOuterClass.MemberRole.MEMBER_ROLE_UNSPECIFIED ->
        @Suppress("UseCheckOrError")
        throw IllegalStateException(INVALID_CONVERSION_MESSAGE)
}

fun paperDecisionFromGrpc(decision: ProjectOuterClass.PaperDecision): PaperDecision = when (decision) {
    ProjectOuterClass.PaperDecision.PAPER_DECISION_UNREVIEWED -> PaperDecision.UNREVIEWED
    ProjectOuterClass.PaperDecision.PAPER_DECISION_IN_REVIEW -> PaperDecision.IN_REVIEW
    ProjectOuterClass.PaperDecision.PAPER_DECISION_DECLINED -> PaperDecision.DECLINED
    ProjectOuterClass.PaperDecision.PAPER_DECISION_ACCEPTED -> PaperDecision.ACCEPTED
    ProjectOuterClass.PaperDecision.UNRECOGNIZED, ProjectOuterClass.PaperDecision.PAPER_DECISION_UNSPECIFIED,
    ->
        @Suppress("UseCheckOrError")
        throw IllegalStateException(INVALID_CONVERSION_MESSAGE)
}

fun reviewDecisionFromGrpc(decision: ReviewOuterClass.ReviewDecision): ReviewDecision = when (decision) {
    ReviewOuterClass.ReviewDecision.REVIEW_DECISION_DECLINED -> ReviewDecision.DECLINED
    ReviewOuterClass.ReviewDecision.REVIEW_DECISION_MAYBE -> ReviewDecision.MAYBE
    ReviewOuterClass.ReviewDecision.REVIEW_DECISION_ACCEPTED -> ReviewDecision.ACCEPTED
    ReviewOuterClass.ReviewDecision.UNRECOGNIZED,
    ReviewOuterClass.ReviewDecision.REVIEW_DECISION_UNSPECIFIED,
    ->
        @Suppress("UseCheckOrError")
        throw IllegalStateException(INVALID_CONVERSION_MESSAGE)
}

fun userRoleFromGrpc(role: UserOuterClass.UserRole): UserRole = when (role) {
    UserOuterClass.UserRole.USER_ROLE_DEFAULT -> UserRole.DEFAULT
    UserOuterClass.UserRole.USER_ROLE_ADMIN -> UserRole.ADMIN
    UserOuterClass.UserRole.UNRECOGNIZED, UserOuterClass.UserRole.USER_ROLE_UNSPECIFIED ->
        @Suppress("UseCheckOrError")
        throw IllegalStateException(INVALID_CONVERSION_MESSAGE)
}

fun userStatusFromGrpc(status: UserOuterClass.UserStatus): UserStatus = when (status) {
    UserOuterClass.UserStatus.USER_STATUS_ACTIVE -> UserStatus.ACTIVE
    UserOuterClass.UserStatus.USER_STATUS_DELETED -> UserStatus.DELETED
    UserOuterClass.UserStatus.USER_STATUS_ACTIVE_UNCONFIRMED -> UserStatus.ACTIVE_UNCONFIRMED
    UserOuterClass.UserStatus.UNRECOGNIZED, UserOuterClass.UserStatus.USER_STATUS_UNSPECIFIED ->
        @Suppress("UseCheckOrError")
        throw IllegalStateException(INVALID_CONVERSION_MESSAGE)
}

fun paperFieldFromGrpc(fieldMaskPath: String): PaperField {
    fun getGrpcPaths(field: PaperField) = when (field) {
        PaperField.TITLE -> "paper.title"
        PaperField.ABSTRACT -> "paper.abstrakt"
        PaperField.YEAR -> "paper.year"
        PaperField.PUBLISHER -> "paper.publisher"
        PaperField.PUBLICATION_NAME -> "paper.publication_name"
        PaperField.PUBLICATION_TYPE -> "paper.publication_type"
        PaperField.AUTHORS -> "paper.authors"
        PaperField.EXTERNAL_IDS -> "paper.external_ids"
    }

    val allPaths = PaperField.entries.associateBy { getGrpcPaths(it) }
    return allPaths.getValue(fieldMaskPath)
}

fun projectFieldFromGrpc(fieldMaskPath: String): ProjectField {
    fun getGrpcPaths(field: ProjectField) = when (field) {
        ProjectField.NAME -> "project.name"
        ProjectField.STATUS -> "project.status"
        ProjectField.SIMILARITY_THRESHOLD -> "project.settings.similarity_threshold"
        ProjectField.SNOWBALLING_TYPE -> "project.settings.snowballing_type"
        ProjectField.REVIEW_MAYBE_ALLOWED -> "project.settings.review_maybe_allowed"
        ProjectField.FETCHERS -> "project.settings.fetchers"
        ProjectField.NUMBER_OF_REVIEWERS -> "project.settings.decision_matrix.number_of_reviewers"
        ProjectField.DECISION_MATRIX_PATTERNS -> "project.settings.decision_matrix.patterns"
    }

    val allPaths = ProjectField.entries.associateBy { getGrpcPaths(it) }
    return allPaths.getValue(fieldMaskPath)
}

fun criterionFieldFromGrpc(fieldMaskPath: String): CriterionField {
    fun getGrpcPaths(field: CriterionField) = when (field) {
        CriterionField.TAG -> "criterion.tag"
        CriterionField.NAME -> "criterion.name"
        CriterionField.DESCRIPTION -> "criterion.description"
        CriterionField.CATEGORY -> "criterion.category"
    }

    val allPaths = CriterionField.entries.associateBy { getGrpcPaths(it) }
    return allPaths.getValue(fieldMaskPath)
}

fun userFieldFromGrpc(fieldMaskPath: String): UserField {
    fun getGrpcPaths(field: UserField) = when (field) {
        UserField.EMAIL -> "user.email"
        UserField.FIRST_NAME -> "user.first_name"
        UserField.LAST_NAME -> "user.last_name"
        UserField.ROLE -> "user.role"
        UserField.STATUS -> "user.status"
    }

    val allPaths = UserField.entries.associateBy { getGrpcPaths(it) }
    return allPaths.getValue(fieldMaskPath)
}

fun projectInfoFieldFromGrpc(fieldMaskPath: String): ProjectInfoField {
    fun getGrpcPaths(field: ProjectInfoField) = when (field) {
        ProjectInfoField.PROJECT_PROGRESS -> "project_progress"
        ProjectInfoField.CREATION_DATE -> "creation_date"
        ProjectInfoField.LAST_STAGE_STARTED -> "last_stage_started"
    }

    val allPaths = ProjectInfoField.entries.associateBy { getGrpcPaths(it) }
    return allPaths.getValue(fieldMaskPath)
}
