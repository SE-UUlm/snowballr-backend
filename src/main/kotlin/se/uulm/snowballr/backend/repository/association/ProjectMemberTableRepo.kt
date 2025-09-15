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
import se.uulm.snowballr.backend.model.dto.ProjectMemberWithUser
import se.uulm.snowballr.backend.repository.getEntities
import se.uulm.snowballr.backend.repository.getEntityByKeysAsResult
import se.uulm.snowballr.backend.repository.getEntityOrNull
import se.uulm.snowballr.backend.repository.insertAndGet
import se.uulm.snowballr.backend.repository.updateAndGet
import se.uulm.snowballr.backend.table.UserTable
import se.uulm.snowballr.backend.table.association.ProjectMemberTable
import se.uulm.snowballr.backend.table.association.toProjectMember
import se.uulm.snowballr.backend.table.association.toProjectMemberWithUser
import snowballr.ProjectOuterClass.MemberRole
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
     * Returns a [Result] containing the project member by its composed ID or a [NotFoundException] if the project with
     * the passed [projectId] or the user with the passed [userId] doesn't exist.
     */
    suspend fun getProjectMemberByComposedId(projectId: UUID, userId: UUID): Result<ProjectMember>

    /**
     * Adds a user with the passed [userId] as member to the project with the passed [projectId].
     *
     * @return The added [ProjectMember].
     */
    suspend fun addUserToProject(userId: UUID, projectId: UUID): ProjectMember

    /**
     * Returns all project members of the project with the passed [projectId].
     */
    suspend fun getProjectMembers(projectId: UUID): List<ProjectMember>

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

    /**
     * Retrieves a list of all project members along with their associated user details for a given project.
     *
     * @param projectId The unique identifier of the project whose members and their associated user details are to be retrieved.
     * @return A list of [ProjectMemberWithUser] objects, each containing a project member and the corresponding user information.
     */
    suspend fun getProjectMembersWithUsers(projectId: UUID): List<ProjectMemberWithUser>
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

    override suspend fun getProjectMemberByComposedId(projectId: UUID, userId: UUID): Result<ProjectMember> = db.query {
        getEntityByKeysAsResult(::getProjectMemberByComposedIdOrNull, EntityType.PROJECT_MEMBER, projectId, userId)
    }

    override suspend fun addUserToProject(userId: UUID, projectId: UUID) = db.query {
        // Return when the user is already a project member
        val projectMembers = getProjectMembers(projectId)
        val existingMember = projectMembers.find { it.userId == userId }
        if (existingMember != null) {
            return@query existingMember
        }

        ProjectMemberTable.insertAndGet(ResultRow::toProjectMember, EntityType.PROJECT_MEMBER) {
            it[this.userId] = userId
            it[this.projectId] = projectId
            it[role] = MemberRole.MEMBER_ROLE_DEFAULT
        }
    }

    override suspend fun getProjectMembers(projectId: UUID): List<ProjectMember> = db.query {
        ProjectMemberTable.getEntities(ResultRow::toProjectMember) { ProjectMemberTable.projectId eq projectId }
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
        ProjectMemberTable.getEntities(ResultRow::toProjectMember) {
            (ProjectMemberTable.projectId eq projectId) and
                (ProjectMemberTable.role eq MemberRole.MEMBER_ROLE_ADMIN)
        }
    }

    override suspend fun promoteProjectMemberToAdmin(projectId: UUID, userId: UUID): ProjectMember = db.query {
        ProjectMemberTable.updateAndGet(
            mapper = ResultRow::toProjectMember,
            entityType = EntityType.PROJECT_MEMBER,
            id = projectId.toString(),
            where = {
                (ProjectMemberTable.projectId eq projectId) and (ProjectMemberTable.userId eq userId)
            },
        ) {
            it[role] = MemberRole.MEMBER_ROLE_ADMIN
        }
    }

    override suspend fun getProjectMembersWithUsers(projectId: UUID): List<ProjectMemberWithUser> = db.query {
        ProjectMemberTable
            .join(UserTable, JoinType.INNER, onColumn = ProjectMemberTable.userId, otherColumn = UserTable.id)
            .selectAll()
            .where { ProjectMemberTable.projectId eq projectId }
            .map { it.toProjectMemberWithUser() }
    }
}
