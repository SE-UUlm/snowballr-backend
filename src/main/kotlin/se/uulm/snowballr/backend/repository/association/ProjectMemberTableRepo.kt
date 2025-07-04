package se.uulm.snowballr.backend.repository.association

import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.alias
import org.jetbrains.exposed.sql.selectAll
import se.uulm.snowballr.backend.db.IDatabase
import se.uulm.snowballr.backend.model.SnowballRException.EntityNotPersistedException
import se.uulm.snowballr.backend.model.dto.ProjectMember
import se.uulm.snowballr.backend.repository.insertAndGet
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
    override suspend fun addUserToProject(userId: UUID, projectId: UUID) = db.dbQuery {
        // Get user reference
        val userEntityId = getUserEntityId(userId)

        // Get project reference
        val projectEntityId = getProjectEntityId(projectId)

        // Return when the user is already a project member
        val projectMembers = getMembersOfProject(projectId)
        val existingMember = projectMembers.find { it.userId == userEntityId.value }
        if (existingMember != null) {
            return@dbQuery existingMember
        }

        ProjectMemberTable.insertAndGet(ResultRow::toProjectMember, { EntityNotPersistedException.ProjectMember(it) }) {
            it[this.userId] = userEntityId
            it[this.projectId] = projectEntityId
            it[role] = ProjectOuterClass.MemberRole.MEMBER_ROLE_DEFAULT
        }
    }

    override suspend fun getMembersOfProject(projectId: UUID): List<ProjectMember> = db.dbQuery {
        ProjectMemberTable
            .selectAll()
            .where { ProjectMemberTable.projectId eq projectId }
            .map { it.toProjectMember() }
    }

    override suspend fun getMembersInSameProjectsAsUser(userId: UUID): List<ProjectMember> = db.dbQuery {
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
}
