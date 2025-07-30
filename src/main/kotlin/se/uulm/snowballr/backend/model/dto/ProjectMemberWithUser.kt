package se.uulm.snowballr.backend.model.dto

import snowballr.ProjectOuterClass

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
    .setRole(this.projectMember.role)
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
