package se.uulm.snowballr.backend.access

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.access.rules.AccessRule
import se.uulm.snowballr.backend.model.AccessType
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.dto.criterion.Criterion
import se.uulm.snowballr.backend.model.dto.criterion.CriterionCategory
import se.uulm.snowballr.backend.model.dto.projectmember.MemberRole
import se.uulm.snowballr.backend.model.dto.user.User
import se.uulm.snowballr.backend.model.dto.user.UserRole
import se.uulm.snowballr.backend.model.exception.unauthorized.UnauthorizedDeleteException
import se.uulm.snowballr.backend.model.exception.unauthorized.UnauthorizedReadException
import se.uulm.snowballr.backend.model.exception.unauthorized.UnauthorizedUpdateException
import se.uulm.snowballr.backend.model.incoming.criterion.CreateCriterionRequest
import se.uulm.snowballr.backend.repository.association.IProjectMemberTableRepo
import java.util.UUID
import kotlin.reflect.KSuspendFunction3

class CriterionAccessCheckerTest {
    private val projectMemberRepo = mockk<IProjectMemberTableRepo>()
    private val projectAccessChecker = mockk<IProjectAccessChecker>()

    private val accessChecker = CriterionAccessChecker(projectMemberRepo, projectAccessChecker)

    private val successAccessRule = AccessRule<UUID> { _, _ -> true }
    private val failureAccessRule = AccessRule<UUID> { _, _ -> throw UnauthorizedTestException() }

    companion object {
        @JvmStatic
        fun getUpdateAndDeleteMethods(): List<KSuspendFunction3<CriterionAccessChecker, User, Criterion, Unit>> {
            suspend fun update(checker: CriterionAccessChecker, user: User, criterion: Criterion) {
                checker.isAllowedToUpdateCriterion(user, criterion)
            }

            suspend fun delete(checker: CriterionAccessChecker, user: User, criterion: Criterion) {
                checker.isAllowedToDeleteCriterion(user, criterion)
            }

            return listOf(::update, ::delete)
        }
    }

    @Nested
    inner class IsAllowedToCreateCriterion {
        private fun getProjectCriterionRequest() = CreateCriterionRequest(
            tag = "Tag",
            name = "Criterion",
            description = "Description",
            category = CriterionCategory.EXCLUSION,
            projectId = UUID.randomUUID(),
        )

        @Test
        fun `When isProjectOrServerAdmin and isProjectActiveById both allow access, then access is allowed`() =
            runTest {
                val user = DataBuilder.createExampleUser()
                val request = getProjectCriterionRequest()

                every {
                    projectAccessChecker.isProjectOrServerAdmin(AccessType.CREATE, EntityType.CRITERION)
                } returns successAccessRule
                every { projectAccessChecker.isProjectActiveButNotLockedById() } returns successAccessRule

                assertDoesNotThrow { accessChecker.isAllowedToCreateCriterion(user, request) }
            }

        @Test
        fun `When isProjectOrServerAdmin denies access, then access is denied`() = runTest {
            val user = DataBuilder.createExampleUser()
            val request = getProjectCriterionRequest()

            every {
                projectAccessChecker.isProjectOrServerAdmin(AccessType.CREATE, EntityType.CRITERION)
            } returns failureAccessRule

            assertThrows<UnauthorizedTestException> { accessChecker.isAllowedToCreateCriterion(user, request) }
        }

        @Test
        fun `When isProjectActiveById allows, but isProjectOrServerAdmin denies access, then access is denied`() =
            runTest {
                val user = DataBuilder.createExampleUser()
                val request = getProjectCriterionRequest()

                every {
                    projectAccessChecker.isProjectOrServerAdmin(AccessType.CREATE, EntityType.CRITERION)
                } returns failureAccessRule
                every { projectAccessChecker.isProjectActiveButNotLockedById() } returns successAccessRule

                assertThrows<UnauthorizedTestException> {
                    accessChecker.isAllowedToCreateCriterion(user, request)
                }
            }

        @Test
        fun `When a user criterion is passed, then it is unconditionally allowed`() = runTest {
            val user = DataBuilder.createExampleUser()
            val request = CreateCriterionRequest(
                tag = "Tag",
                name = "Criterion",
                description = "Description",
                category = CriterionCategory.EXCLUSION,
                projectId = null,
            )

            accessChecker.isAllowedToCreateCriterion(user, request)
        }
    }

    @Nested
    inner class IsAllowedToReadCriterion {
        @Test
        fun `When the user is the creator of the criterion, then access is allowed`() = runTest {
            val user = DataBuilder.createExampleUser()
            val criterion = DataBuilder.createExampleUserCriterion(createdBy = user.id)

            assertDoesNotThrow { accessChecker.isAllowedToReadCriterion(user, criterion) }
        }

        @Test
        fun `When the user is in the project of the criterion, then access is allowed`() = runTest {
            val user = DataBuilder.createExampleUser()
            val projectId = UUID.randomUUID()
            val criterion = DataBuilder.createExampleProjectCriterion(projectId = projectId)

            coEvery { projectMemberRepo.isProjectMember(projectId, user.id) } returns true

            assertDoesNotThrow { accessChecker.isAllowedToReadCriterion(user, criterion) }
        }

        @Test
        fun `When the user is a server admin, then access is allowed`() = runTest {
            val user = DataBuilder.createExampleUser(role = UserRole.ADMIN)
            val projectId = UUID.randomUUID()
            val criterion = DataBuilder.createExampleProjectCriterion(projectId = projectId)

            coEvery { projectMemberRepo.isProjectMember(projectId, user.id) } returns false

            assertDoesNotThrow { accessChecker.isAllowedToReadCriterion(user, criterion) }
        }

        @Test
        fun `When the user is not the creator, not in the project, and not a server admin, then access is denied`() =
            runTest {
                val user = DataBuilder.createExampleUser()
                val projectId = UUID.randomUUID()
                val criterion = DataBuilder.createExampleProjectCriterion(projectId = projectId)

                coEvery { projectMemberRepo.isProjectMember(projectId, user.id) } returns false

                assertThrows<UnauthorizedReadException> { accessChecker.isAllowedToReadCriterion(user, criterion) }
            }

        @Test
        fun `When the user is not the creator of a user criterion and not a server admin, then access is denied`() =
            runTest {
                val user = DataBuilder.createExampleUser()
                val criterion = DataBuilder.createExampleUserCriterion()

                assertThrows<UnauthorizedReadException> { accessChecker.isAllowedToReadCriterion(user, criterion) }
            }
    }

    @Nested
    inner class IsAllowedToUpdateOrDeleteCriterion {
        @ParameterizedTest
        @MethodSource("se.uulm.snowballr.backend.access.CriterionAccessCheckerTest#getUpdateAndDeleteMethods")
        fun `When the user is the creator of the criterion, then access is allowed`(
            accessMethod: KSuspendFunction3<CriterionAccessChecker, User, Criterion, Unit>,
        ) = runTest {
            val user = DataBuilder.createExampleUser()
            val criterion = DataBuilder.createExampleUserCriterion(createdBy = user.id)

            assertDoesNotThrow { accessMethod.invoke(accessChecker, user, criterion) }
        }

        @ParameterizedTest
        @MethodSource("se.uulm.snowballr.backend.access.CriterionAccessCheckerTest#getUpdateAndDeleteMethods")
        fun `When the user is an admin in the active project of the criterion, then access is allowed`(
            accessMethod: KSuspendFunction3<CriterionAccessChecker, User, Criterion, Unit>,
        ) = runTest {
            val user = DataBuilder.createExampleUser()
            val projectId = UUID.randomUUID()
            val projectAdmin = DataBuilder.createExampleProjectMember(
                userId = user.id,
                projectId = projectId,
                role = MemberRole.ADMIN,
            )
            val criterion = DataBuilder.createExampleProjectCriterion(projectId = projectId)

            coEvery { projectMemberRepo.getAllProjectAdmins(projectId) } returns listOf(projectAdmin)
            coEvery { projectAccessChecker.isProjectActiveButNotLockedById() } returns successAccessRule

            assertDoesNotThrow { accessMethod.invoke(accessChecker, user, criterion) }
        }

        @ParameterizedTest
        @MethodSource("se.uulm.snowballr.backend.access.CriterionAccessCheckerTest#getUpdateAndDeleteMethods")
        fun `When the user is an admin in the inactive project of the criterion, then access is allowed`(
            accessMethod: KSuspendFunction3<CriterionAccessChecker, User, Criterion, Unit>,
        ) = runTest {
            val user = DataBuilder.createExampleUser()
            val projectId = UUID.randomUUID()
            val projectAdmin = DataBuilder.createExampleProjectMember(
                userId = user.id,
                projectId = projectId,
                role = MemberRole.ADMIN,
            )
            val criterion = DataBuilder.createExampleProjectCriterion(projectId = projectId)

            coEvery { projectMemberRepo.getAllProjectAdmins(projectId) } returns listOf(projectAdmin)
            coEvery { projectAccessChecker.isProjectActiveButNotLockedById() } returns failureAccessRule

            assertThrows<UnauthorizedTestException> { accessMethod.invoke(accessChecker, user, criterion) }
        }

        @ParameterizedTest
        @MethodSource("se.uulm.snowballr.backend.access.CriterionAccessCheckerTest#getUpdateAndDeleteMethods")
        fun `When the user is a server admin, then access is allowed`(
            accessMethod: KSuspendFunction3<CriterionAccessChecker, User, Criterion, Unit>,
        ) = runTest {
            val user = DataBuilder.createExampleUser(role = UserRole.ADMIN)
            val criterion = DataBuilder.createExampleUserCriterion(createdBy = user.id)

            assertDoesNotThrow { accessMethod.invoke(accessChecker, user, criterion) }
        }

        @Test
        fun `When the user is not the creator, not an admin in the project, and not a server admin, then access is denied for updates`() =
            runTest {
                val user = DataBuilder.createExampleUser()
                val projectId = UUID.randomUUID()
                val criterion = DataBuilder.createExampleProjectCriterion(projectId = projectId)

                coEvery { projectMemberRepo.getAllProjectAdmins(projectId) } returns emptyList()

                assertThrows<UnauthorizedUpdateException> { accessChecker.isAllowedToUpdateCriterion(user, criterion) }
            }

        @Test
        fun `When the user is not the creator, not an admin in the project, and not a server admin, then access is denied for deletions`() =
            runTest {
                val user = DataBuilder.createExampleUser()
                val projectId = UUID.randomUUID()
                val criterion = DataBuilder.createExampleProjectCriterion(projectId = projectId)

                coEvery { projectMemberRepo.getAllProjectAdmins(projectId) } returns emptyList()

                assertThrows<UnauthorizedDeleteException> { accessChecker.isAllowedToDeleteCriterion(user, criterion) }
            }

        @Test
        fun `When the user is not the creator of a user criterion and not a server admin, then access is denied for updates`() =
            runTest {
                val user = DataBuilder.createExampleUser()
                val criterion = DataBuilder.createExampleUserCriterion()

                assertThrows<UnauthorizedUpdateException> { accessChecker.isAllowedToUpdateCriterion(user, criterion) }
            }

        @Test
        fun `When the user is not the creator of a user criterion and not a server admin, then access is denied for deletions`() =
            runTest {
                val user = DataBuilder.createExampleUser()
                val criterion = DataBuilder.createExampleUserCriterion()

                assertThrows<UnauthorizedDeleteException> { accessChecker.isAllowedToDeleteCriterion(user, criterion) }
            }
    }

    @Nested
    inner class IsAllowedToUseAsDefaultCriterion {
        @Test
        fun `When the user is the creator of a user criterion, then access is allowed`() = runTest {
            val user = DataBuilder.createExampleUser()
            val criterion = DataBuilder.createExampleUserCriterion(createdBy = user.id)

            assertDoesNotThrow { accessChecker.isAllowedToUseAsDefaultCriterion(user, criterion) }
        }

        @Test
        fun `When the user is not the creator of a user criterion, then access is denied`() = runTest {
            val user = DataBuilder.createExampleUser()
            val criterion = DataBuilder.createExampleUserCriterion()

            assertThrows<UnauthorizedReadException> { accessChecker.isAllowedToUseAsDefaultCriterion(user, criterion) }
        }

        @Test
        fun `When the criterion is a project criterion, then access is denied`() = runTest {
            val user = DataBuilder.createExampleUser()
            val criterion = DataBuilder.createExampleProjectCriterion(createdBy = user.id)

            assertThrows<UnauthorizedReadException> { accessChecker.isAllowedToUseAsDefaultCriterion(user, criterion) }
        }

        @Test
        fun `When the user is a server admin but not the creator of the criterion, then access is denied`() = runTest {
            val user = DataBuilder.createExampleUser(role = UserRole.ADMIN)
            val criterion = DataBuilder.createExampleUserCriterion()

            assertThrows<UnauthorizedReadException> { accessChecker.isAllowedToUseAsDefaultCriterion(user, criterion) }
        }
    }
}
