package se.uulm.snowballr.backend.repository

import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insertAndGetId
import se.uulm.snowballr.backend.db.IDatabase
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.table.PaperTable
import se.uulm.snowballr.backend.table.ProjectTable
import se.uulm.snowballr.backend.table.UserTable
import se.uulm.snowballr.backend.table.association.ProjectMemberTable
import se.uulm.snowballr.backend.table.association.toProjectMember
import snowballr.ProjectOuterClass
import snowballr.UserOuterClass
import java.util.UUID

/**
 * This class acts as a collection of create methods to create database entries for testing purposes.
 */
object RepositoryHelper {
    lateinit var db: IDatabase

    /**
     * Creates an example user in the database with the specified email and fake user details
     *
     * @param email The email address for the example user to be created.
     * @return The uuid of the created user
     */
    suspend fun createExampleUser(email: String) = db.query {
        UserTable
            .insertAndGetId {
                it[UserTable.email] = email
                it[firstName] = "Test"
                it[lastName] = "User"
                it[passwordHash] = "1234"
                it[role] = UserOuterClass.UserRole.USER_ROLE_DEFAULT
                it[status] = UserOuterClass.UserStatus.USER_STATUS_ACTIVE
            }.value
    }

    /**
     * Assigns a user to a project by inserting an entry into the ProjectMemberTable with the default member role.
     *
     * @param userId The unique identifier of the user to be assigned to the project.
     * @param projectId The unique identifier of the project to which the user is being assigned.
     * @return The created project member instance
     */
    suspend fun assignUserToProject(userId: UUID, projectId: UUID) = db.query {
        ProjectMemberTable.insertAndGet(ResultRow::toProjectMember, EntityType.PROJECT_MEMBER) {
            it[ProjectMemberTable.userId] = userId
            it[ProjectMemberTable.projectId] = projectId
            it[role] = ProjectOuterClass.MemberRole.MEMBER_ROLE_DEFAULT
        }
    }

    /**
     * Creates a user with the specified email and assigns the user to a project.
     *
     * @param email The email address of the user to be created.
     * @param projectId The unique identifier of the project to which the user will be assigned.
     */
    suspend fun createAndAssignUserToProject(email: String, projectId: UUID) = db.query {
        val userId = createExampleUser(email)
        assignUserToProject(userId, projectId)
    }

    @Suppress("LongParameterList")
    suspend fun insertPaperAndGetId(
        title: String = "Title",
        externalId: String = "ExternalId",
        abstract: String = "Abstract",
        publishedAt: Instant = Instant.fromEpochSeconds(0),
        publisher: String = "Publisher",
        publicationType: String = "PublicationType",
        publicationName: String = "PublicationName",
        fetcherMetadata: Map<String, String> = emptyMap(),
    ): UUID = db.query {
        PaperTable.insertAndGetId {
            it[PaperTable.title] = title
            it[PaperTable.externalId] = externalId
            it[PaperTable.abstract] = abstract
            it[PaperTable.publishedAt] = publishedAt
            it[PaperTable.publisher] = publisher
            it[PaperTable.publicationType] = publicationType
            it[PaperTable.publicationName] = publicationName
            it[PaperTable.fetcherMetadata] = fetcherMetadata
        }.value
    }

    suspend fun insertTestProjectAndGetId(name: String, status: ProjectOuterClass.ProjectStatus, userId: UUID): UUID =
        db.query {
            ProjectTable
                .insertAndGetId {
                    it[ProjectTable.name] = name
                    it[ProjectTable.status] = status
                    it[currentStage] = 0
                    it[maxStage] = 0
                    it[similarityThreshold] = 0F
                    it[snowballingType] = ProjectOuterClass.SnowballingType.SNOWBALLING_TYPE_BOTH
                    it[reviewMaybeAllowed] = true
                    it[reviewDecisionMatrixBinary] =
                        ProjectOuterClass.ReviewDecisionMatrix.getDefaultInstance().toByteArray()
                    it[fetcherApis] = emptyList()
                    it[createdBy] = userId
                }.value
        }
}
