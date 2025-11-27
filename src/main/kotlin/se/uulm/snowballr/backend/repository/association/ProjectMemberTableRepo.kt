package se.uulm.snowballr.backend.repository.association

import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.alias
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.selectAll
import se.uulm.snowballr.backend.db.IDatabase
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.dto.ProjectMember
import se.uulm.snowballr.backend.model.dto.ProjectMemberWithUser
import se.uulm.snowballr.backend.model.exception.NotFoundException
import se.uulm.snowballr.backend.repository.doesEntityExist
import se.uulm.snowballr.backend.repository.getEntities
import se.uulm.snowballr.backend.repository.getEntityByKeysAsResult
import se.uulm.snowballr.backend.repository.getEntityOrNull
import se.uulm.snowballr.backend.repository.insertAndGet
import se.uulm.snowballr.backend.repository.updateAndGet
import se.uulm.snowballr.backend.table.UserTable
import se.uulm.snowballr.backend.table.association.ProjectMemberTable
import se.uulm.snowballr.backend.table.association.toProjectMember
import se.uulm.snowballr.backend.table.association.toProjectMemberWithUser
import se.uulm.snowballr.backend.table.toUser
import snowballr.ProjectOuterClass.MemberRole
import snowballr.UserOuterClass.UserStatus
import java.util.UUID

/**
 * Defines an interface for repository operations related to the [ProjectMemberTable].
 *
 * This interface provides abstraction for handling persistence and retrieval
 * operations for project members. By using this interface, the functionality for creating
 * project members can remain decoupled from the specifics of the database layer.
 *
 * **Note**: A user that was soft-deleted is still in the project member relation in case the user is restored but
 * is not considered an active member of a project anymore and thus not returned when project members / admin are queried.
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
     * Updates the role of a project member.
     *
     * @param projectId The unique identifier of the project to which the member belongs.
     * @param userId The unique identifier of the user whose role is to be updated.
     * @param role The new role to be assigned to the project member.
     * @return The updated [ProjectMember] with the new role.
     */
    suspend fun updateProjectMemberRole(projectId: UUID, userId: UUID, role: MemberRole): ProjectMember

    /**
     * Retrieves a list of all project members along with their associated user details for a given project.
     *
     * @param projectId The unique identifier of the project whose members and their associated user details are to be retrieved.
     * @return A list of [ProjectMemberWithUser] objects, each containing a project member and the corresponding user information.
     */
    suspend fun getProjectMembersWithUsers(projectId: UUID): List<ProjectMemberWithUser>

    /**
     * Removes a project member with the given [userId] from the project with the given [projectId].
     *
     * @param projectId The unique identifier of the project whose member should be removed.
     * @param userId The unique identifier of the user, who should be removed.
     */
    suspend fun removeProjectMember(projectId: UUID, userId: UUID)

    /**
     * Checks whether the user with the given [userId] is a member of the project with the given [projectId].
     *
     * @param projectId The unique identifier of the project.
     * @param userId The unique identifier of the user.
     * @return `true` if the user is a member of the project, `false` otherwise.
     */
    suspend fun isProjectMember(projectId: UUID, userId: UUID): Boolean
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
     * Retrieves a list of UUIDs from these users that are marked as deleted.
     * As users are automatically hard-deleted after some time, this list should not be that long.
     *
     * @return A list of UUIDs representing users that are soft-deleted.
     */
    private fun getSoftDeletedUserIds(): List<UUID> = UserTable
        .getEntities(ResultRow::toUser) { UserTable.status eq UserStatus.USER_STATUS_DELETED }
        .map { it.id }

    /**
     * Requesting a project member from the database.
     *
     * @param projectId The ID of the requested project.
     * @param userId The ID of the requested user.
     * @return The [ProjectMember] object or null, if no project with the given [projectId] and [userId] was found.
     */
    private fun getProjectMemberByComposedIdOrNull(projectId: UUID, userId: UUID): ProjectMember? = ProjectMemberTable
        .getEntityOrNull(ResultRow::toProjectMember) {
            (ProjectMemberTable.projectId eq projectId) and
                (ProjectMemberTable.userId eq userId) and
                (ProjectMemberTable.userId notInList getSoftDeletedUserIds())
        }

    override suspend fun getProjectMemberByComposedId(projectId: UUID, userId: UUID): Result<ProjectMember> = db.query {
        getEntityByKeysAsResult(::getProjectMemberByComposedIdOrNull, EntityType.PROJECT_MEMBER, projectId, userId)
    }

    override suspend fun addUserToProject(userId: UUID, projectId: UUID) = db.query {
        // Return when the user is already a project member
        val projectMembers = getProjectMembers(projectId)
        val existentMember = projectMembers.find { it.userId == userId }
        if (existentMember != null) {
            return@query existentMember
        }

        ProjectMemberTable.insertAndGet(ResultRow::toProjectMember, EntityType.PROJECT_MEMBER) {
            it[this.userId] = userId
            it[this.projectId] = projectId
            it[role] = MemberRole.MEMBER_ROLE_DEFAULT
        }
    }

    override suspend fun getProjectMembers(projectId: UUID): List<ProjectMember> = db.query {
        ProjectMemberTable.getEntities(ResultRow::toProjectMember) {
            (ProjectMemberTable.projectId eq projectId) and
                (ProjectMemberTable.userId notInList getSoftDeletedUserIds())
        }
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
            .where { ProjectMemberTable.userId neq userId }
            .andWhere { ProjectMemberTable.userId notInList getSoftDeletedUserIds() }
            .map { it.toProjectMember() }
    }

    override suspend fun getAllProjectAdmins(projectId: UUID): List<ProjectMember> = db.query {
        ProjectMemberTable.getEntities(ResultRow::toProjectMember) {
            (ProjectMemberTable.projectId eq projectId) and
                (ProjectMemberTable.role eq MemberRole.MEMBER_ROLE_ADMIN) and
                (ProjectMemberTable.userId notInList getSoftDeletedUserIds())
        }
    }

    override suspend fun updateProjectMemberRole(projectId: UUID, userId: UUID, role: MemberRole): ProjectMember =
        db.query {
            ProjectMemberTable.updateAndGet(
                mapper = ResultRow::toProjectMember,
                entityType = EntityType.PROJECT_MEMBER,
                id = projectId.toString(),
                where = {
                    (ProjectMemberTable.projectId eq projectId) and (ProjectMemberTable.userId eq userId)
                },
            ) {
                it[ProjectMemberTable.role] = role
            }
        }

    override suspend fun getProjectMembersWithUsers(projectId: UUID): List<ProjectMemberWithUser> = db.query {
        (ProjectMemberTable innerJoin UserTable)
            .selectAll()
            .where { ProjectMemberTable.projectId eq projectId }
            .andWhere { UserTable.status neq UserStatus.USER_STATUS_DELETED }
            .map { it.toProjectMemberWithUser() }
    }

    override suspend fun removeProjectMember(projectId: UUID, userId: UUID) {
        db.query {
            ProjectMemberTable
                .deleteWhere { (ProjectMemberTable.projectId eq projectId) and (ProjectMemberTable.userId eq userId) }
        }
    }

    override suspend fun isProjectMember(projectId: UUID, userId: UUID): Boolean = db.query {
        ProjectMemberTable.doesEntityExist {
            (ProjectMemberTable.projectId eq projectId) and
                (ProjectMemberTable.userId eq userId) and
                (ProjectMemberTable.userId notInList getSoftDeletedUserIds())
        }
    }
}
