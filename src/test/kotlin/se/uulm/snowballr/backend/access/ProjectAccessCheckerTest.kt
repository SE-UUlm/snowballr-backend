package se.uulm.snowballr.backend.access

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.access.rules.checkFor
import se.uulm.snowballr.backend.model.AccessType
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.exception.FailedPreconditionException
import se.uulm.snowballr.backend.model.exception.failedprecondition.EntityNotActiveException
import se.uulm.snowballr.backend.model.exception.notfound.entity.ProjectNotFoundException
import se.uulm.snowballr.backend.model.exception.unauthorized.UnauthorizedDeleteException
import se.uulm.snowballr.backend.model.exception.unauthorized.UnauthorizedReadAllException
import se.uulm.snowballr.backend.model.exception.unauthorized.UnauthorizedReadException
import se.uulm.snowballr.backend.model.exception.unauthorized.UnauthorizedUpdateException
import se.uulm.snowballr.backend.repository.IProjectTableRepo
import se.uulm.snowballr.backend.repository.association.IProjectMemberTableRepo
import snowballr.ProjectOuterClass.MemberRole
import snowballr.ProjectOuterClass.ProjectStatus
import snowballr.UserOuterClass.UserRole
import java.util.UUID

class ProjectAccessCheckerTest {
    private val projectRepo = mockk<IProjectTableRepo>()
    private val projectMemberRepo = mockk<IProjectMemberTableRepo>()

    private val accessChecker = ProjectAccessChecker(projectRepo, projectMemberRepo)

    @Nested
    inner class IsAllowedToReadProject {
        @Test
        fun `When the project doesn't exist, then a ProjectNotFoundException is thrown`() = runTest {
            val user = DataBuilder.createExampleUser()
            val projectId = UUID.randomUUID()

            coEvery { projectRepo.getProjectById(projectId) } returns Result.failure(TestSpecificException())

            assertThrows<ProjectNotFoundException> { accessChecker.isAllowedToReadProject(user, projectId) }
        }

        @Test
        fun `When the project is deleted and the user is a regular user, then a ProjectNotFoundException is thrown`() =
            runTest {
                val user = DataBuilder.createExampleUser()
                val projectId = UUID.randomUUID()
                val project = DataBuilder.createExampleProject(
                    id = projectId,
                    status = ProjectStatus.PROJECT_STATUS_DELETED,
                )

                coEvery { projectRepo.getProjectById(projectId) } returns Result.success(project)

                assertThrows<ProjectNotFoundException> { accessChecker.isAllowedToReadProject(user, projectId) }
            }

        @Test
        fun `When the project is deleted and the user is a server admin, then access is allowed`() = runTest {
            val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
            val projectId = UUID.randomUUID()
            val project = DataBuilder.createExampleProject(
                id = projectId,
                status = ProjectStatus.PROJECT_STATUS_DELETED,
            )

            coEvery { projectRepo.getProjectById(projectId) } returns Result.success(project)
            coEvery { projectMemberRepo.isProjectMember(projectId, user.id) } returns false

            assertDoesNotThrow { accessChecker.isAllowedToReadProject(user, projectId) }
        }

        @Test
        fun `When the project exists and the user is a project member, then access is allowed`() = runTest {
            val user = DataBuilder.createExampleUser()
            val projectId = UUID.randomUUID()
            val project = DataBuilder.createExampleProject(id = projectId)

            coEvery { projectRepo.getProjectById(projectId) } returns Result.success(project)
            coEvery { projectMemberRepo.isProjectMember(projectId, user.id) } returns true

            assertDoesNotThrow { accessChecker.isAllowedToReadProject(user, projectId) }
        }

        @Test
        fun `When the project exists and the user is a server admin but not a member, then access is allowed`() =
            runTest {
                val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
                val projectId = UUID.randomUUID()
                val project = DataBuilder.createExampleProject(id = projectId)

                coEvery { projectRepo.getProjectById(projectId) } returns Result.success(project)
                coEvery { projectMemberRepo.isProjectMember(projectId, user.id) } returns false

                assertDoesNotThrow { accessChecker.isAllowedToReadProject(user, projectId) }
            }

        @Test
        fun `When the project exists but the user is neither a member nor a server admin, then access is denied`() =
            runTest {
                val user = DataBuilder.createExampleUser()
                val projectId = UUID.randomUUID()
                val project = DataBuilder.createExampleProject(id = projectId)

                coEvery { projectRepo.getProjectById(projectId) } returns Result.success(project)
                coEvery { projectMemberRepo.isProjectMember(projectId, user.id) } returns false

                assertThrows<UnauthorizedReadException> { accessChecker.isAllowedToReadProject(user, projectId) }
            }
    }

    @Nested
    inner class IsAllowedToReadUserProjects {
        @Test
        fun `When the current user is the same user, then access is allowed`() = runTest {
            val user = DataBuilder.createExampleUser()

            assertDoesNotThrow { accessChecker.isAllowedToReadUserProjects(user, user.id) }
        }

        @Test
        fun `When the current user is a server admin, then access is allowed`() = runTest {
            val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
            val targetUserId = UUID.randomUUID()

            assertDoesNotThrow { accessChecker.isAllowedToReadUserProjects(user, targetUserId) }
        }

        @Test
        fun `When the current user is neither the same user nor a server admin, then access is denied`() = runTest {
            val user = DataBuilder.createExampleUser()
            val targetUserId = UUID.randomUUID()

            assertThrows<UnauthorizedReadException> { accessChecker.isAllowedToReadUserProjects(user, targetUserId) }
        }
    }

    @Nested
    inner class IsAllowedToReadAllProjects {
        @Test
        fun `When the current user is a server admin, then access is allowed`() = runTest {
            val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)

            assertDoesNotThrow { accessChecker.isAllowedToReadAllProjects(user) }
        }

        @Test
        fun `When the current user is not a server admin, then access is denied`() = runTest {
            val user = DataBuilder.createExampleUser()

            assertThrows<UnauthorizedReadAllException> { accessChecker.isAllowedToReadAllProjects(user) }
        }
    }

    @Nested
    inner class IsNotLastProjectAdmin {
        @Test
        fun `When the user is the only project admin, then a FailedPreconditionException is thrown`() = runTest {
            val user = DataBuilder.createExampleUser()
            val projectId = UUID.randomUUID()
            val projectAdmin = DataBuilder.createExampleProjectMember(
                userId = user.id,
                projectId = projectId,
                role = MemberRole.MEMBER_ROLE_ADMIN,
            )

            coEvery { projectMemberRepo.getAllProjectAdmins(projectId) } returns listOf(projectAdmin)

            assertThrows<FailedPreconditionException> {
                accessChecker.isNotLastProjectAdmin(user, projectId, "Cannot perform action")
            }
        }

        @Test
        fun `When the user is one of multiple project admins, then the check passes`() = runTest {
            val user = DataBuilder.createExampleUser()
            val projectId = UUID.randomUUID()
            val projectAdmin = DataBuilder.createExampleProjectMember(
                userId = user.id,
                projectId = projectId,
                role = MemberRole.MEMBER_ROLE_ADMIN,
            )
            val otherAdmin = DataBuilder.createExampleProjectMember(
                projectId = projectId,
                role = MemberRole.MEMBER_ROLE_ADMIN,
            )

            coEvery { projectMemberRepo.getAllProjectAdmins(projectId) } returns listOf(projectAdmin, otherAdmin)

            assertDoesNotThrow { accessChecker.isNotLastProjectAdmin(user, projectId, "Cannot perform action") }
        }

        @Test
        fun `When the user is not a project admin at all, then the check passes`() = runTest {
            val user = DataBuilder.createExampleUser()
            val projectId = UUID.randomUUID()
            val otherAdmin = DataBuilder.createExampleProjectMember(
                projectId = projectId,
                role = MemberRole.MEMBER_ROLE_ADMIN,
            )

            coEvery { projectMemberRepo.getAllProjectAdmins(projectId) } returns listOf(otherAdmin)

            assertDoesNotThrow { accessChecker.isNotLastProjectAdmin(user, projectId, "Cannot perform action") }
        }

        @Test
        fun `When there are no project admins, then the check passes`() = runTest {
            val user = DataBuilder.createExampleUser()
            val projectId = UUID.randomUUID()

            coEvery { projectMemberRepo.getAllProjectAdmins(projectId) } returns emptyList()

            assertDoesNotThrow { accessChecker.isNotLastProjectAdmin(user, projectId, "Cannot perform action") }
        }
    }

    @Nested
    inner class IsProjectOrServerAdminSuspend {
        @Test
        fun `When the user is a project admin, then access is allowed`() = runTest {
            val user = DataBuilder.createExampleUser()
            val projectId = UUID.randomUUID()
            val projectAdmin = DataBuilder.createExampleProjectMember(
                userId = user.id,
                projectId = projectId,
                role = MemberRole.MEMBER_ROLE_ADMIN,
            )

            coEvery { projectMemberRepo.getAllProjectAdmins(projectId) } returns listOf(projectAdmin)

            assertDoesNotThrow { accessChecker.isProjectOrServerAdmin(user, projectId, AccessType.READ) }
        }

        @Test
        fun `When the user is a server admin but not a project admin, then access is allowed`() = runTest {
            val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
            val projectId = UUID.randomUUID()

            coEvery { projectMemberRepo.getAllProjectAdmins(projectId) } returns emptyList()

            assertDoesNotThrow { accessChecker.isProjectOrServerAdmin(user, projectId, AccessType.READ) }
        }

        @Test
        fun `When the user is neither a project admin nor a server admin, then an UnauthorizedReadException is thrown`() =
            runTest {
                val user = DataBuilder.createExampleUser()
                val projectId = UUID.randomUUID()

                coEvery { projectMemberRepo.getAllProjectAdmins(projectId) } returns emptyList()

                assertThrows<UnauthorizedReadException> {
                    accessChecker.isProjectOrServerAdmin(user, projectId, AccessType.READ)
                }
            }

        @Test
        fun `When the user is neither a project admin nor a server admin, the thrown exception reflects the access type`() =
            runTest {
                val user = DataBuilder.createExampleUser()
                val projectId = UUID.randomUUID()

                coEvery { projectMemberRepo.getAllProjectAdmins(projectId) } returns emptyList()

                assertThrows<UnauthorizedDeleteException> {
                    accessChecker.isProjectOrServerAdmin(user, projectId, AccessType.DELETE)
                }
            }
    }

    @Nested
    inner class IsProjectExistent {
        @Test
        fun `When the project doesn't exist, then a ProjectNotFoundException is thrown`() = runTest {
            val user = DataBuilder.createExampleUser()
            val projectId = UUID.randomUUID()

            coEvery { projectRepo.getProjectById(projectId) } returns Result.failure(TestSpecificException())

            assertThrows<ProjectNotFoundException> { accessChecker.isProjectExistent().checkFor(user, projectId) }
        }

        @Test
        fun `When the project exists and is not deleted, then the check passes`() = runTest {
            val user = DataBuilder.createExampleUser()
            val projectId = UUID.randomUUID()
            val project = DataBuilder.createExampleProject(id = projectId)

            coEvery { projectRepo.getProjectById(projectId) } returns Result.success(project)

            assertDoesNotThrow { accessChecker.isProjectExistent().checkFor(user, projectId) }
        }

        @Test
        fun `When the project is deleted and the user is not a server admin, then a ProjectNotFoundException is thrown`() =
            runTest {
                val user = DataBuilder.createExampleUser()
                val projectId = UUID.randomUUID()
                val project = DataBuilder.createExampleProject(
                    id = projectId,
                    status = ProjectStatus.PROJECT_STATUS_DELETED,
                )

                coEvery { projectRepo.getProjectById(projectId) } returns Result.success(project)

                assertThrows<ProjectNotFoundException> { accessChecker.isProjectExistent().checkFor(user, projectId) }
            }

        @Test
        fun `When the project is deleted and the user is a server admin, then the check passes`() = runTest {
            val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
            val projectId = UUID.randomUUID()
            val project = DataBuilder.createExampleProject(
                id = projectId,
                status = ProjectStatus.PROJECT_STATUS_DELETED,
            )

            coEvery { projectRepo.getProjectById(projectId) } returns Result.success(project)

            assertDoesNotThrow { accessChecker.isProjectExistent().checkFor(user, projectId) }
        }
    }

    @Nested
    inner class IsProjectActiveById {
        @Test
        fun `When the project doesn't exist, then an EntityNotActiveException is thrown`() = runTest {
            val user = DataBuilder.createExampleUser()
            val projectId = UUID.randomUUID()

            coEvery { projectRepo.getProjectById(projectId) } returns Result.failure(TestSpecificException())

            assertThrows<EntityNotActiveException> { accessChecker.isProjectActiveById().checkFor(user, projectId) }
        }

        @Test
        fun `When the project exists and is active, then the check passes`() = runTest {
            val user = DataBuilder.createExampleUser()
            val projectId = UUID.randomUUID()
            val project = DataBuilder.createExampleProject(
                id = projectId,
                status = ProjectStatus.PROJECT_STATUS_ACTIVE,
            )

            coEvery { projectRepo.getProjectById(projectId) } returns Result.success(project)

            assertDoesNotThrow { accessChecker.isProjectActiveById().checkFor(user, projectId) }
        }

        @Test
        fun `When the project exists but is deleted, then an EntityNotActiveException is thrown`() = runTest {
            val user = DataBuilder.createExampleUser()
            val projectId = UUID.randomUUID()
            val project = DataBuilder.createExampleProject(
                id = projectId,
                status = ProjectStatus.PROJECT_STATUS_DELETED,
            )

            coEvery { projectRepo.getProjectById(projectId) } returns Result.success(project)

            assertThrows<EntityNotActiveException> { accessChecker.isProjectActiveById().checkFor(user, projectId) }
        }
    }

    @Nested
    inner class IsProjectOrServerAdminRule {
        @Test
        fun `When the user is a project admin, then the check passes`() = runTest {
            val user = DataBuilder.createExampleUser()
            val projectId = UUID.randomUUID()
            val projectAdmin = DataBuilder.createExampleProjectMember(
                userId = user.id,
                projectId = projectId,
                role = MemberRole.MEMBER_ROLE_ADMIN,
            )

            coEvery { projectMemberRepo.getAllProjectAdmins(projectId) } returns listOf(projectAdmin)

            assertDoesNotThrow {
                accessChecker.isProjectOrServerAdmin(AccessType.READ).checkFor(user, projectId)
            }
        }

        @Test
        fun `When the user is a server admin but not a project admin, then the check passes`() = runTest {
            val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
            val projectId = UUID.randomUUID()

            coEvery { projectMemberRepo.getAllProjectAdmins(projectId) } returns emptyList()

            assertDoesNotThrow {
                accessChecker.isProjectOrServerAdmin(AccessType.READ).checkFor(user, projectId)
            }
        }

        @Test
        fun `When the user is neither a project admin nor a server admin, then an UnauthorizedReadException is thrown`() =
            runTest {
                val user = DataBuilder.createExampleUser()
                val projectId = UUID.randomUUID()

                coEvery { projectMemberRepo.getAllProjectAdmins(projectId) } returns emptyList()

                assertThrows<UnauthorizedReadException> {
                    accessChecker.isProjectOrServerAdmin(AccessType.READ).checkFor(user, projectId)
                }
            }

        @Test
        fun `When the user is neither a project admin nor a server admin, the thrown exception reflects the entity type`() =
            runTest {
                val user = DataBuilder.createExampleUser()
                val projectId = UUID.randomUUID()

                coEvery { projectMemberRepo.getAllProjectAdmins(projectId) } returns emptyList()

                assertThrows<UnauthorizedUpdateException> {
                    accessChecker.isProjectOrServerAdmin(AccessType.UPDATE, EntityType.REVIEW).checkFor(user, projectId)
                }
            }
    }
}
