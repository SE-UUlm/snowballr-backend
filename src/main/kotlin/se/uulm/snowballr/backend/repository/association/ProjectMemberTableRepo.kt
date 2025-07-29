package se.uulm.snowballr.backend.repository.association

import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.alias
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import se.uulm.snowballr.backend.db.IDatabase
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.SnowballRException.NotFoundException
import se.uulm.snowballr.backend.model.dto.ProjectMember
import se.uulm.snowballr.backend.repository.getEntityOrNull
import se.uulm.snowballr.backend.repository.insertAndGet
import se.uulm.snowballr.backend.repository.updateAndGet
import se.uulm.snowballr.backend.table.association.ProjectMemberTable
import se.uulm.snowballr.backend.table.association.toProjectMember
import se.uulm.snowballr.backend.table.getProjectEntityId
import se.uulm.snowballr.backend.table.getUserEntityId
import snowballr.ProjectOuterClass
import java.util.UUID

/**
 * Defines an interface for repository operations related to the [ProjectMemberTable].
 *
 * This interface provides abstraction for handling persistence and retrieval
 * operations for project members. By using this interface, the functionality for creating
 * project members can remain decoupled from the specifics of the database layer.
 */
interface IProjectMemberTableRepo {
    /**
     * Returns a project member by its composed ID or throws a [NotFoundException] if the project with the passed [projectId] and [userId] doesn't exist.
     */
    suspend fun getProjectMemberByComposedId(projectId: UUID, userId: UUID): ProjectMember

    /**
     * Adds a user with the passed [userId] as member to the project with the passed [projectId].
     *
     * @return The added [ProjectMember].
     */
    suspend fun addUserToProject(userId: UUID, projectId: UUID): ProjectMember

    /**
     * Returns all project members of the project with the passed [projectId].
     */
    suspend fun getMembersOfProject(projectId: UUID): List<ProjectMember>

    /**
     * Returns all project members, which are in the same projects as the user with the passed [userId].
     *
     * The user itself is not part of the resulting list.
     */
    suspend fun getMembersInSameProjectsAsUser(userId: UUID): List<ProjectMember>

    /**
     * Returns the [List] of all [ProjectMember] with the role admin.
     */
    suspend fun getAllProjectAdmins(projectId: UUID): List<ProjectMember>

    /**
     * Promotes a project member to an admin role in the specified project.
     *
     * @param projectId The unique identifier of the project to which the member belongs.
     * @param userId The unique identifier of the user to be promoted to admin.
     * @return The updated [ProjectMember] including the new role as an admin.
     */
    suspend fun promoteProjectMemberToAdmin(projectId: UUID, userId: UUID): ProjectMember
}

/**
 * Repository implementation for managing the [ProjectMemberTable] in the database.
 *
 * This class provides functionality to handle persistence and retrieval operations
 * for project member data by leveraging the database abstraction defined in [IDatabase]. It
 * facilitates CRUD operations on project member records associated with a given project and
 * ensures database transactions are handled properly.
 *
 * @param db The database abstraction used for executing queries within a transaction.
 */
class ProjectMemberTableRepo(
    private val db: IDatabase,
) : IProjectMemberTableRepo {
    /**
     * Requesting a project member from the database.
     *
     * @param projectId The ID of the requested project.
     * @param userId The id of the requested user.
     * @return The [ProjectMember] object or null, if no project with the given [projectId] and [userId] was found.
     */
    private fun getProjectMemberByComposedIdOrNull(projectId: UUID, userId: UUID): ProjectMember? = ProjectMemberTable
        .getEntityOrNull(ResultRow::toProjectMember) {
            (ProjectMemberTable.projectId eq projectId) and (ProjectMemberTable.userId eq userId)
        }

    override suspend fun getProjectMemberByComposedId(projectId: UUID, userId: UUID): ProjectMember = db.query {
        getProjectMemberByComposedIdOrNull(projectId, userId)
            ?: throw NotFoundException(EntityType.PROJECT_MEMBER, projectId.toString(), userId.toString())
    }

    override suspend fun addUserToProject(userId: UUID, projectId: UUID) = db.query {
        // Get user reference
        val userEntityId = getUserEntityId(userId)

        // Get project reference
        val projectEntityId = getProjectEntityId(projectId)

        // Return when the user is already a project member
        val projectMembers = getMembersOfProject(projectId)
        val existingMember = projectMembers.find { it.userId == userEntityId.value }
        if (existingMember != null) {
            return@query existingMember
        }

        ProjectMemberTable.insertAndGet(ResultRow::toProjectMember, EntityType.PROJECT_MEMBER) {
            it[this.userId] = userEntityId
            it[this.projectId] = projectEntityId
            it[role] = ProjectOuterClass.MemberRole.MEMBER_ROLE_DEFAULT
        }
    }

    override suspend fun getMembersOfProject(projectId: UUID): List<ProjectMember> = db.query {
        ProjectMemberTable
            .selectAll()
            .where { ProjectMemberTable.projectId eq projectId }
            .map { it.toProjectMember() }
    }

    override suspend fun getMembersInSameProjectsAsUser(userId: UUID): List<ProjectMember> = db.query {
        // We join the ProjectMemberTable with itself but aliasing one instance to represent the user's membership.
        val userMembership = ProjectMemberTable.alias("userMembership")

        ProjectMemberTable
            .join(
                userMembership,
                JoinType.INNER,
                onColumn = ProjectMemberTable.projectId,
                otherColumn = userMembership[ProjectMemberTable.projectId],
            ) {
                // Condition to find projects where the specific user is a member
                userMembership[ProjectMemberTable.userId] eq userId
            }.selectAll()
            // Filter out the calling user
            .where { ProjectMemberTable.userId neq userId }
            .map { it.toProjectMember() }
    }

    override suspend fun getAllProjectAdmins(projectId: UUID): List<ProjectMember> = db.query {
        ProjectMemberTable
            .selectAll()
            .where {
                (ProjectMemberTable.projectId eq projectId) and
                    (ProjectMemberTable.role eq ProjectOuterClass.MemberRole.MEMBER_ROLE_ADMIN)
            }
            .map { it.toProjectMember() }
    }

    override suspend fun promoteProjectMemberToAdmin(projectId: UUID, userId: UUID): ProjectMember = db.query {
        ProjectMemberTable.updateAndGet(
            mapper = ResultRow::toProjectMember,
            entityType = EntityType.PROJECT_MEMBER,
            id = projectId.toString(),
            where = {
                (ProjectMemberTable.projectId eq projectId) and (ProjectMemberTable.userId eq userId)
            },
            body = {
                it[role] = ProjectOuterClass.MemberRole.MEMBER_ROLE_ADMIN
            },
        )
    }
}
