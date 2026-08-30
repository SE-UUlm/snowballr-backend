@file:Suppress("TooManyFunctions")

package se.uulm.snowballr.backend.grpc

import com.google.protobuf.kotlin.toByteString
import com.google.protobuf.timestamp
import se.uulm.snowballr.backend.model.auth.AuthenticationStatus
import se.uulm.snowballr.backend.model.dto.criterion.Criterion
import se.uulm.snowballr.backend.model.dto.criterion.CriterionCategory
import se.uulm.snowballr.backend.model.dto.paper.Author
import se.uulm.snowballr.backend.model.dto.paper.ExternalId
import se.uulm.snowballr.backend.model.dto.project.DecisionMatrixPattern
import se.uulm.snowballr.backend.model.dto.project.DecisionMatrixPatternEntry
import se.uulm.snowballr.backend.model.dto.project.ProjectSettings
import se.uulm.snowballr.backend.model.dto.project.ProjectStatus
import se.uulm.snowballr.backend.model.dto.project.ReviewDecisionMatrix
import se.uulm.snowballr.backend.model.dto.project.SnowballingType
import se.uulm.snowballr.backend.model.dto.projectmember.MemberRole
import se.uulm.snowballr.backend.model.dto.projectmember.ProjectMemberWithUser
import se.uulm.snowballr.backend.model.dto.projectpaper.PaperDecision
import se.uulm.snowballr.backend.model.dto.review.ReviewDecision
import se.uulm.snowballr.backend.model.dto.user.User
import se.uulm.snowballr.backend.model.dto.user.UserRole
import se.uulm.snowballr.backend.model.dto.user.UserSettingsWithCriteria
import se.uulm.snowballr.backend.model.dto.user.UserStatus
import se.uulm.snowballr.backend.model.export.ExportFormat
import se.uulm.snowballr.backend.model.export.FileExport
import se.uulm.snowballr.backend.model.fetcher.FetcherInformation
import se.uulm.snowballr.backend.model.fetcher.FetcherInformationWithId
import se.uulm.snowballr.backend.model.fetcher.FetcherOptionsSchema
import se.uulm.snowballr.backend.model.fetcher.Link
import se.uulm.snowballr.backend.model.outgoing.invitation.InvitationResponse
import se.uulm.snowballr.backend.model.outgoing.paper.FetcherPaperResponse
import se.uulm.snowballr.backend.model.outgoing.paper.PaperResponse
import se.uulm.snowballr.backend.model.outgoing.project.ProjectDecisionCount
import se.uulm.snowballr.backend.model.outgoing.project.ProjectDecisionStatistics
import se.uulm.snowballr.backend.model.outgoing.project.ProjectInformation
import se.uulm.snowballr.backend.model.outgoing.project.ProjectResponse
import se.uulm.snowballr.backend.model.outgoing.projectpaper.ProjectPaperResponse
import se.uulm.snowballr.backend.model.outgoing.review.ReviewResponse
import snowballr.Authentication
import snowballr.Base
import snowballr.CriterionOuterClass
import snowballr.Export
import snowballr.Fetcher
import snowballr.PaperOuterClass
import snowballr.ProjectOuterClass
import snowballr.ReviewOuterClass
import snowballr.UserOuterClass
import snowballr.UserSettingsOuterClass

fun Criterion.toGrpc(): CriterionOuterClass.Criterion = CriterionOuterClass.Criterion
    .newBuilder()
    .setId(this.id.toString())
    .setTag(this.tag)
    .setName(this.name)
    .setDescription(this.description)
    .setCategory(this.category.toGrpc())
    .build()

@JvmName("toGrpcCriterionList")
fun List<Criterion>.toGrpc(): CriterionOuterClass.Criterion.List {
    val builder = CriterionOuterClass.Criterion.List.newBuilder()
    this.forEach { builder.addCriteria(it.toGrpc()) }
    return builder.build()
}

fun Author.toGrpc(): PaperOuterClass.Author = PaperOuterClass.Author.newBuilder()
    .setFirstName(firstName)
    .setLastName(lastName)
    .build()

fun List<Author>.toGrpc(): List<PaperOuterClass.Author> = this.map(Author::toGrpc)

fun ProjectSettings.toGrpc(): ProjectOuterClass.Project.Settings = ProjectOuterClass.Project.Settings
    .newBuilder()
    .setSimilarityThreshold(this.similarityThreshold)
    .setDecisionMatrix(this.reviewDecisionMatrix.toGrpc())
    .setSnowballingType(this.snowballingType.toGrpc())
    .setReviewMaybeAllowed(this.reviewMaybeAllowed)
    .putAllFetchers(
        this.fetchers.mapValues {
            Fetcher.FetcherOptions
                .newBuilder()
                .putAllOptions(it.value)
                .build()
        },
    )
    .build()

fun ProjectResponse.toGrpc(): ProjectOuterClass.Project = ProjectOuterClass.Project
    .newBuilder()
    .setId(this.id.toString())
    .setName(this.name)
    .setStatus(this.status.toGrpc())
    .setCurrentStage(this.currentStage.toLong())
    .setMaxStage(this.maxStage.toLong())
    .setSettings(this.settings.toGrpc())
    .build()

@JvmName("toGrpcProjectList")
fun List<ProjectResponse>.toGrpc(): ProjectOuterClass.Project.List {
    val builder = ProjectOuterClass.Project.List.newBuilder()
    this.forEach { builder.addProjects(it.toGrpc()) }
    return builder.build()
}

fun ProjectMemberWithUser.toGrpc(): ProjectOuterClass.Project.Member = ProjectOuterClass.Project.Member
    .newBuilder()
    .setRole(this.projectMember.role.toGrpc())
    .setUser(this.user.toGrpc())
    .build()

@JvmName("toGrpcProjectMemberList")
fun List<ProjectMemberWithUser>.toGrpc(): ProjectOuterClass.Project.Member.List = ProjectOuterClass.Project.Member.List
    .newBuilder()
    .addAllMembers(this.map { it.toGrpc() })
    .build()

fun User.toGrpc(): UserOuterClass.User = UserOuterClass.User
    .newBuilder()
    .setId(this.id.toString())
    .setEmail(this.email)
    .setFirstName(this.firstName)
    .setLastName(this.lastName)
    .setRole(this.role.toGrpc())
    .setStatus(this.status.toGrpc())
    .build()

@JvmName("toGrpcUserList")
fun List<User>.toGrpc(): UserOuterClass.User.List {
    val builder = UserOuterClass.User.List.newBuilder()
    this.forEach { builder.addUsers(it.toGrpc()) }
    return builder.build()
}

fun UserSettingsWithCriteria.toGrpc(): UserSettingsOuterClass.UserSettings =
    UserSettingsOuterClass.UserSettings.newBuilder()
        .setShowHotkeys(settings.areHotkeysShown)
        .setReviewMode(settings.isReviewModeEnabled)
        .setDefaultCriteria(criteria.toGrpc())
        .setDefaultProjectSettings(
            ProjectOuterClass.Project.Settings.newBuilder()
                .setSimilarityThreshold(settings.similarityThreshold)
                .setDecisionMatrix(settings.decisionMatrix.toGrpc())
                .putAllFetchers(
                    settings.fetchers.mapValues {
                        Fetcher.FetcherOptions
                            .newBuilder()
                            .putAllOptions(it.value)
                            .build()
                    },
                )
                .setSnowballingType(settings.snowballingType.toGrpc())
                .setReviewMaybeAllowed(settings.reviewMaybeAllowed)
                .build(),
        )
        .build()

fun InvitationResponse.toGrpc(): UserOuterClass.User = UserOuterClass.User.newBuilder()
    .setId(userId?.toString().orEmpty())
    .setEmail(email)
    .setFirstName(firstName)
    .setLastName(lastName)
    .setRole(role.toGrpc())
    .setStatus(status.toGrpc())
    .build()

@JvmName("toGrpcInviteesList")
fun List<InvitationResponse>.toGrpc(): UserOuterClass.User.List = UserOuterClass.User.List.newBuilder()
    .addAllUsers(this.map { it.toGrpc() })
    .build()

fun FetcherPaperResponse.toGrpc(): PaperOuterClass.Paper = PaperOuterClass.Paper.newBuilder()
    .setId(id?.toString().orEmpty())
    .addAllExternalIds(externalIds.map { it.toGrpc() })
    .setTitle(title)
    .setAbstrakt(abstract)
    .setYear(year)
    .setPublisher(publisher)
    .setPublicationName(publicationName)
    .setPublicationType(publicationType)
    .addAllAuthors(authors.toGrpc())
    .putAllFetcherMetadata(fetcherMetadata)
    .addAllBackwardReferencedIds(backwardReferencedIds.map { it.toString() })
    .build()

@JvmName("toGrpcFetcherPaperList")
fun List<FetcherPaperResponse>.toGrpc(): PaperOuterClass.Paper.List = PaperOuterClass.Paper.List.newBuilder()
    .addAllPapers(this.map { it.toGrpc() })
    .build()

fun ProjectDecisionCount.toGrpc(): ProjectOuterClass.Project.Information.DecisionStatistics.Statistic =
    ProjectOuterClass.Project.Information.DecisionStatistics.Statistic.newBuilder()
        .setDecision(decision.toGrpc())
        .setCount(count.toLong())
        .build()

fun PaperResponse.toGrpc(): PaperOuterClass.Paper = PaperOuterClass.Paper.newBuilder()
    .setId(id.toString())
    .addAllExternalIds(externalIds.map { it.toGrpc() })
    .setTitle(title)
    .setAbstrakt(abstract)
    .setYear(year)
    .setPublisher(publisher)
    .setPublicationName(publicationName)
    .setPublicationType(publicationType)
    .addAllAuthors(authors.toGrpc())
    .putAllFetcherMetadata(fetcherMetadata)
    .addAllBackwardReferencedIds(backwardReferencedIds.map { it.toString() })
    .build()

@JvmName("toGrpcPaperList")
fun List<PaperResponse>.toGrpc(): PaperOuterClass.Paper.List = PaperOuterClass.Paper.List.newBuilder()
    .addAllPapers(this.map { it.toGrpc() })
    .build()

fun ProjectDecisionStatistics.toGrpc(): ProjectOuterClass.Project.Information.DecisionStatistics =
    ProjectOuterClass.Project.Information.DecisionStatistics.newBuilder()
        .addAllStatistics(statistics.map { it.toGrpc() })
        .build()

fun ProjectInformation.toGrpc(): ProjectOuterClass.Project.Information =
    ProjectOuterClass.Project.Information.newBuilder()
        .setProjectProgress(progress)
        .setCreationDate(timestamp { seconds = creationDate.toEpochSecond() })
        .setLastStageStarted(timestamp { seconds = lastStageStarted.toEpochSecond() })
        .build()

fun ProjectPaperResponse.toGrpc(): ProjectOuterClass.Project.Paper = ProjectOuterClass.Project.Paper.newBuilder()
    .setId(this.id.toString())
    .setPaper(this.paper.toGrpc())
    .setStage(this.stage.toLong())
    .setDecision(this.decision.toGrpc())
    .addAllReviews(reviews.map { it.toGrpc() })
    .setLocalId(this.localPaperId.toString())
    .build()

@JvmName("toGrpcProjectPaperList")
fun List<ProjectPaperResponse>.toGrpc(): ProjectOuterClass.Project.Paper.List =
    ProjectOuterClass.Project.Paper.List.newBuilder()
        .addAllProjectPapers(this.map { it.toGrpc() })
        .build()

fun ReviewResponse.toGrpc(): ReviewOuterClass.Review = ReviewOuterClass.Review
    .newBuilder()
    .setId(id.toString())
    .setUserId(userId.toString())
    .setDecision(decision.toGrpc())
    .addAllSelectedCriteriaIds(selectedCriteriaIds.map { it.toString() })
    .build()

@JvmName("toGrpcReviewList")
fun List<ReviewResponse>.toGrpc(): ReviewOuterClass.Review.List = ReviewOuterClass.Review.List
    .newBuilder()
    .addAllReviews(this.map { it.toGrpc() })
    .build()

fun FileExport.toGrpc(): Export.ExportResponse = Export.ExportResponse.newBuilder()
    .setData(data.toByteString())
    .setFileName(filename)
    .build()

fun FetcherInformation.toGrpc(fetcherId: String): Fetcher.FetcherInformation = Fetcher.FetcherInformation.newBuilder()
    .setId(fetcherId)
    .setName(name)
    .setDescription(description)
    .addAllLinks(links.map { it.toGrpc() })
    .putAllOptionsSchema(optionsSchema.mapValues { it.value.toGrpc() })
    .build()

fun FetcherInformationWithId.toGrpc(): Fetcher.FetcherInformation = information.toGrpc(id)
fun FetcherOptionsSchema.toGrpc(): Fetcher.FetcherOptionSchema = Fetcher.FetcherOptionSchema.newBuilder()
    .setName(name)
    .setDescription(description)
    .setRequired(isRequired)
    .setIsSecret(isSecret)
    .apply { if (defaultValue != null) setDefaultValue(defaultValue) }
    .build()

fun Link.toGrpc(): Base.Link = Base.Link.newBuilder()
    .setLabel(label)
    .setUrl(url)
    .build()

fun Set<ExportFormat>.toGrpc(): Export.AvailableExportFormatsResponse = Export.AvailableExportFormatsResponse
    .newBuilder()
    .addAllFormats(this.map { it.toString() })
    .build()

fun CriterionCategory.toGrpc() = when (this) {
    CriterionCategory.INCLUSION -> CriterionOuterClass.CriterionCategory.CRITERION_CATEGORY_INCLUSION
    CriterionCategory.EXCLUSION -> CriterionOuterClass.CriterionCategory.CRITERION_CATEGORY_EXCLUSION
    CriterionCategory.HARD_EXCLUSION -> CriterionOuterClass.CriterionCategory.CRITERION_CATEGORY_HARD_EXCLUSION
}

fun DecisionMatrixPattern.toGrpc(): ProjectOuterClass.ReviewDecisionMatrix.Pattern =
    ProjectOuterClass.ReviewDecisionMatrix.Pattern.newBuilder()
        .setDecision(decision.toGrpc())
        .addAllEntries(entries.map { it.toGrpc() })
        .build()

fun DecisionMatrixPatternEntry.toGrpc(): ProjectOuterClass.ReviewDecisionMatrix.Pattern.Entry =
    ProjectOuterClass.ReviewDecisionMatrix.Pattern.Entry.newBuilder()
        .setReviewDecision(decision.toGrpc())
        .setCount(count.toLong())
        .build()

fun ReviewDecisionMatrix.toGrpc(): ProjectOuterClass.ReviewDecisionMatrix =
    ProjectOuterClass.ReviewDecisionMatrix.newBuilder()
        .setNumberOfReviewers(numberOfReviewers)
        .addAllPatterns(patterns.map { it.toGrpc() })
        .build()

fun ProjectStatus.toGrpc() = when (this) {
    ProjectStatus.ACTIVE -> ProjectOuterClass.ProjectStatus.PROJECT_STATUS_ACTIVE
    ProjectStatus.ACTIVE_LOCKED -> ProjectOuterClass.ProjectStatus.PROJECT_STATUS_ACTIVE_LOCKED
    ProjectStatus.ARCHIVED -> ProjectOuterClass.ProjectStatus.PROJECT_STATUS_ARCHIVED
    ProjectStatus.DELETED -> ProjectOuterClass.ProjectStatus.PROJECT_STATUS_DELETED
    ProjectStatus.CLEARED -> ProjectOuterClass.ProjectStatus.PROJECT_STATUS_UNSPECIFIED
}

fun SnowballingType.toGrpc(): ProjectOuterClass.SnowballingType = when (this) {
    SnowballingType.FORWARD -> ProjectOuterClass.SnowballingType.SNOWBALLING_TYPE_FORWARD
    SnowballingType.BACKWARD -> ProjectOuterClass.SnowballingType.SNOWBALLING_TYPE_BACKWARD
    SnowballingType.BOTH -> ProjectOuterClass.SnowballingType.SNOWBALLING_TYPE_BOTH
}

fun MemberRole.toGrpc() = when (this) {
    MemberRole.DEFAULT -> ProjectOuterClass.MemberRole.MEMBER_ROLE_DEFAULT
    MemberRole.ADMIN -> ProjectOuterClass.MemberRole.MEMBER_ROLE_ADMIN
}

fun PaperDecision.toGrpc(): ProjectOuterClass.PaperDecision = when (this) {
    PaperDecision.UNREVIEWED -> ProjectOuterClass.PaperDecision.PAPER_DECISION_UNREVIEWED
    PaperDecision.IN_REVIEW -> ProjectOuterClass.PaperDecision.PAPER_DECISION_IN_REVIEW
    PaperDecision.DECLINED -> ProjectOuterClass.PaperDecision.PAPER_DECISION_DECLINED
    PaperDecision.ACCEPTED -> ProjectOuterClass.PaperDecision.PAPER_DECISION_ACCEPTED
}

fun ReviewDecision.toGrpc() = when (this) {
    ReviewDecision.DECLINED -> ReviewOuterClass.ReviewDecision.REVIEW_DECISION_DECLINED
    ReviewDecision.MAYBE -> ReviewOuterClass.ReviewDecision.REVIEW_DECISION_MAYBE
    ReviewDecision.ACCEPTED -> ReviewOuterClass.ReviewDecision.REVIEW_DECISION_ACCEPTED
}

fun UserRole.toGrpc() = when (this) {
    UserRole.DEFAULT -> UserOuterClass.UserRole.USER_ROLE_DEFAULT
    UserRole.ADMIN -> UserOuterClass.UserRole.USER_ROLE_ADMIN
}

fun UserStatus.toGrpc() = when (this) {
    UserStatus.ACTIVE_UNCONFIRMED -> UserOuterClass.UserStatus.USER_STATUS_ACTIVE_UNCONFIRMED
    UserStatus.ACTIVE -> UserOuterClass.UserStatus.USER_STATUS_ACTIVE
    UserStatus.DELETED -> UserOuterClass.UserStatus.USER_STATUS_DELETED
    UserStatus.CLEARED -> UserOuterClass.UserStatus.USER_STATUS_UNSPECIFIED
}

fun AuthenticationStatus.toGrpc() = when (this) {
    AuthenticationStatus.UNAUTHENTICATED -> Authentication.AuthenticationStatus.AUTHENTICATION_STATUS_UNAUTHENTICATED
    AuthenticationStatus.ACCESS_TOKEN_EXPIRED ->
        Authentication.AuthenticationStatus.AUTHENTICATION_STATUS_ACCESS_TOKEN_EXPIRED

    AuthenticationStatus.AUTHENTICATED -> Authentication.AuthenticationStatus.AUTHENTICATION_STATUS_AUTHENTICATED
}

fun ExternalId.toGrpc(): PaperOuterClass.Paper.ExternalId = PaperOuterClass.Paper.ExternalId
    .newBuilder()
    .setType(type.name)
    .setDisplayType(type.displayName)
    .setValue(value)
    .build()
