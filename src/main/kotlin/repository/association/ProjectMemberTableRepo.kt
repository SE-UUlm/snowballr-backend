package se.uulm.snowballr.backend.repository.association

import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.alias
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import se.uulm.snowballr.backend.db.IDatabase
import se.uulm.snowballr.backend.model.SnowballRException.EntityNotPersistedException
import se.uulm.snowballr.backend.model.SnowballRException.NotFoundException
import se.uulm.snowballr.backend.model.dto.ProjectMember
import se.uulm.snowballr.backend.model.parseUUID
import se.uulm.snowballr.backend.table.ProjectTable
import se.uulm.snowballr.backend.table.UserTable
import se.uulm.snowballr.backend.table.association.ProjectMemberTable
import se.uulm.snowballr.backend.table.association.ProjectMemberTable.toProjectMember
import se.uulm.snowballr.backend.table.getEntityId
import snowballr.ProjectOuterClass

/**
 * Defines an interface for repository operations related to the [ProjectMemberTable].
 *
 * This interface provides abstraction for handling persistence and retrieval
 * operations for project members. By using this interface, the functionality for creating
 * project members can remain decoupled from the specifics of the database layer.
 */
interface IProjectMemberTableRepo {
    /**
     * Adds a user with the passed [userId] as member to the project with the passed [projectId].
     *
     * @return The added [ProjectMember].
     */
    suspend fun addUserToProject(userId: String, projectId: String): ProjectMember

    /**
     * Returns all project members of the project with the passed [projectId].
     */
    suspend fun getProjectMembersOfProject(projectId: String): List<ProjectMember>

    /**
     * Returns all project members, which are in the same projects as the user with the passed [userId].
     *
     * The user itself is not part of the resulting list.
     */
    suspend fun getProjectMembersInSameProjectsAsUser(userId: String): List<ProjectMember>
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
    override suspend fun addUserToProject(userId: String, projectId: String) = db.dbQuery {
        // Get user reference
        val userEntityId = UserTable.getEntityId(userId) ?: throw NotFoundException.User(userId)

        // Get project reference
        val projectEntityId =
            ProjectTable.getEntityId(projectId) ?: throw NotFoundException.Project(projectId)

        // Return when the user is already a project member
        val projectMembers = getProjectMembersOfProject(projectId)
        val existingMember = projectMembers.find { it.userId == userEntityId.value }
        if (existingMember != null) {
            return@dbQuery existingMember
        }

        val projectMemberId =
            ProjectMemberTable
                .insertAndGetId {
                    it[this.userId] = userEntityId
                    it[this.projectId] = projectEntityId
                    it[role] = ProjectOuterClass.MemberRole.MEMBER_ROLE_DEFAULT
                }

        ProjectMemberTable
            .selectAll()
            .where { ProjectMemberTable.id eq projectMemberId }
            .map { it.toProjectMember() }
            .singleOrNull()
            ?: throw EntityNotPersistedException.ProjectMember(projectMemberId.toString())
    }

    override suspend fun getProjectMembersOfProject(projectId: String): List<ProjectMember> = db.dbQuery {
        val uuid = parseUUID(projectId, "project")

        ProjectMemberTable
            .selectAll()
            .where { ProjectMemberTable.projectId eq uuid }
            .map { it.toProjectMember() }
    }

    override suspend fun getProjectMembersInSameProjectsAsUser(userId: String): List<ProjectMember> = db.dbQuery {
        val uuid = parseUUID(userId, "project")

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
                userMembership[ProjectMemberTable.userId] eq uuid
            }.selectAll()
            // Filter out the calling user
            .where { ProjectMemberTable.userId neq uuid }
            .map { it.toProjectMember() }
    }
}
