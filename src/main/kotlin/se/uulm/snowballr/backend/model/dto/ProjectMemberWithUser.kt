package se.uulm.snowballr.backend.model.dto

import snowballr.ProjectOuterClass

/**
 * Represents a data transfer object that combines information about a project member
 * and the associated user.
 *
 * This class encapsulates the relationship between a project member's attributes
 * and the user details, making it useful for cases where both sets of information
 * are required together.
 *
 * @property projectMember The project member's details, including their role and
 * association with the project.
 * @property user The user information associated with the project member, including
 * personal details and account status.
 */
data class ProjectMemberWithUser(
    val projectMember: ProjectMember,
    val user: User,
)

/**
 * Converts this instance of [ProjectMemberWithUser] to a [ProjectOuterClass.Project.Member].
 *
 * @return an instance of [ProjectOuterClass.Project.Member] with the role and user fields populated.
 */
fun ProjectMemberWithUser.toGrpcProjectMember(): ProjectOuterClass.Project.Member = ProjectOuterClass.Project.Member
    .newBuilder()
    .setRole(this.projectMember.role.toGrpc())
    .setUser(this.user.toGrpcUser())
    .build()

/**
 * Converts a list of [ProjectMemberWithUser] objects into a gRPC list of project members.
 *
 * @return A [ProjectOuterClass.Project.Member.List] containing the gRPC representation of the project members.
 */
fun List<ProjectMemberWithUser>.toGrpcProjectMembers(): ProjectOuterClass.Project.Member.List =
    ProjectOuterClass.Project.Member.List
        .newBuilder()
        .addAllMembers(this.map { it.toGrpcProjectMember() })
        .build()
