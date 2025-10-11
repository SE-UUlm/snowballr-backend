package se.uulm.snowballr.backend.validation

import com.google.protobuf.FieldMask
import com.google.protobuf.util.FieldMaskUtil
import `in`.rcard.assertj.arrowcore.EitherAssert
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import se.uulm.snowballr.backend.model.BlankField
import se.uulm.snowballr.backend.model.EnumUnspecified
import se.uulm.snowballr.backend.model.InvalidEmail
import se.uulm.snowballr.backend.model.InvalidFieldMask
import se.uulm.snowballr.backend.model.InvalidId
import se.uulm.snowballr.backend.model.OutOfRangeValue
import se.uulm.snowballr.backend.model.TooLongField
import se.uulm.snowballr.backend.validation.ProjectValidator.NAME_MAX_LENGTH
import snowballr.ProjectOuterClass.MemberRole
import snowballr.ProjectOuterClass.Project
import snowballr.ProjectOuterClass.Project.Create
import snowballr.ProjectOuterClass.Project.Update
import snowballr.ProjectOuterClass.ProjectStatus
import snowballr.ProjectOuterClass.SnowballingType
import java.util.UUID

class ProjectValidatorTest {
    @Nested
    inner class CreateRequest {
        private val validCreateRequestBuilder: Create.Builder =
            Create
                .newBuilder()
                .setName("Valid Project Name")

        @Test
        fun `When a valid request is validated, then no issue is returned`() {
            val request = validCreateRequestBuilder.build()
            val result = validateRequest(request)

            EitherAssert.assertThat(result).isRight()
        }

        @Test
        fun `When a blank name is validated, then the 'BlankField' issue is returned`() {
            val request =
                validCreateRequestBuilder
                    .setName("")
                    .build()
            val result = validateRequest(request)

            assertInvalidResult<BlankField>(result)
        }

        @Test
        fun `When a too long name is validated, then the 'TooLongField' issue is returned`() {
            val request =
                validCreateRequestBuilder
                    .setName("a".repeat(NAME_MAX_LENGTH + 1))
                    .build()
            val result = validateRequest(request)

            assertInvalidResult<TooLongField>(result)
        }
    }

    @Nested
    inner class UpdateRequest {
        private val validUpdatedProject: Project.Builder = Project.newBuilder()
            .setId(UUID.randomUUID().toString())
            .setName("Test Project")
            .setStatus(ProjectStatus.PROJECT_STATUS_ARCHIVED)
            .setSettings(
                Project.Settings.newBuilder()
                    .setSimilarityThreshold(1F)
                    .setSnowballingType(SnowballingType.SNOWBALLING_TYPE_FORWARD)
                    .setReviewMaybeAllowed(false),
            )
        private val validFieldMask: FieldMask = FieldMaskUtil
            .fromStringList(
                listOf(
                    "project.name",
                    "project.status",
                    "project.settings.similarity_threshold",
                    "project.settings.snowballing_type",
                    "project.settings.review_maybe_allowed",
                ),
            )

        private val validUpdateRequestBuilder: Update.Builder =
            Update
                .newBuilder()
                .setProject(validUpdatedProject)
                .setMask(validFieldMask)

        @Test
        fun `When a valid request is validated, then no issue is returned`() {
            val request = validUpdateRequestBuilder.build()
            val result = validateRequest(request)

            EitherAssert.assertThat(result).isRight()
        }

        @Test
        fun `When a blank field mask is validated, then the 'InvalidFieldMask' issue is returned`() {
            val inValidFieldMask = FieldMaskUtil.fromStringList(emptyList())
            val request =
                validUpdateRequestBuilder
                    .setMask(inValidFieldMask)
                    .build()
            val result = validateRequest(request)

            assertInvalidResult<InvalidFieldMask>(result)
        }

        @Test
        fun `When a field mask containing a nonexistent field is validated, then the 'InvalidFieldMask' issue is returned`() {
            val request =
                validUpdateRequestBuilder
                    .setMask(FieldMaskUtil.fromStringList(listOf("non_existent_field")))
                    .build()
            val result = validateRequest(request)

            assertInvalidResult<InvalidFieldMask>(result)
        }

        @Test
        fun `When a field mask contains the 'current_stage' or 'max_stage' field, then the 'InvalidFieldMask' issue is returned`() {
            val request = validUpdateRequestBuilder
                .setMask(FieldMaskUtil.fromStringList(listOf("project.current_stage", "project.max_stage")))
                .build()
            val result = validateRequest(request)

            assertInvalidResult<InvalidFieldMask>(result)
        }

        @Test
        fun `When an invalid ID is validated, then the 'InvalidId' issue is returned`() {
            val project = validUpdatedProject.setId("invalid-id").build()
            val request = validUpdateRequestBuilder
                .setProject(project)
                .build()
            val result = validateRequest(request)

            assertInvalidResult<InvalidId>(result)
        }

        @Test
        fun `When an invalid project name is provided and specified in the field mask, then the 'BlankField' issue is returned`() {
            val project = validUpdatedProject.setName("  ").build()
            val request = validUpdateRequestBuilder
                .setProject(project)
                .setMask(validFieldMask)
                .build()
            val result = validateRequest(request)

            assertInvalidResult<BlankField>(result)
        }

        @Test
        fun `When an invalid project name is provided but not specified in the field mask, then no issue is returned`() {
            val project = validUpdatedProject.setName("  ").build()
            val fieldMask = FieldMaskUtil.fromStringList(listOf("project.status"))
            val request = validUpdateRequestBuilder
                .setProject(project)
                .setMask(fieldMask)
                .build()
            val result = validateRequest(request)

            EitherAssert.assertThat(result).isRight()
        }

        @Test
        fun `When an invalid project status is provided and specified in the field mask, then the 'EnumUnspecified' issue is returned`() {
            val project = validUpdatedProject.setStatus(ProjectStatus.PROJECT_STATUS_UNSPECIFIED).build()
            val request = validUpdateRequestBuilder
                .setProject(project)
                .build()
            val result = validateRequest(request)

            assertInvalidResult<EnumUnspecified>(result)
        }

        @Test
        fun `When an invalid role is provided but not specified in the field mask, then no issue is returned`() {
            val project = validUpdatedProject.setStatus(ProjectStatus.PROJECT_STATUS_UNSPECIFIED).build()
            val fieldMask = FieldMaskUtil.fromStringList(listOf("project.name"))
            val request = validUpdateRequestBuilder
                .setProject(project)
                .setMask(fieldMask)
                .build()
            val result = validateRequest(request)

            EitherAssert.assertThat(result).isRight()
        }

        @Test
        fun `When an invalid snowballing type is provided and specified in the field mask, then the 'EnumUnspecified' issue is returned`() {
            val project = validUpdatedProject.setSettings(
                Project.Settings.newBuilder().setSnowballingType(SnowballingType.SNOWBALLING_TYPE_UNSPECIFIED).build(),
            ).build()
            val request = validUpdateRequestBuilder
                .setProject(project)
                .build()
            val result = validateRequest(request)

            assertInvalidResult<EnumUnspecified>(result)
        }

        @Test
        fun `When an invalid snowballing type is provided but not specified in the field mask, then no issue is returned`() {
            val project = validUpdatedProject.setSettings(
                Project.Settings.newBuilder().setSnowballingType(SnowballingType.SNOWBALLING_TYPE_UNSPECIFIED).build(),
            ).build()
            val fieldMask = FieldMaskUtil.fromStringList(listOf("project.name"))
            val request = validUpdateRequestBuilder
                .setProject(project)
                .setMask(fieldMask)
                .build()
            val result = validateRequest(request)

            EitherAssert.assertThat(result).isRight()
        }

        @Test
        fun `When a too low similarity threshold is provided and specified in the field mask, then the 'OutOfRangeValue' issue is returned`() {
            val project = validUpdatedProject.setSettings(
                Project.Settings.newBuilder().setSimilarityThreshold(-1f).build(),
            ).build()
            val request = validUpdateRequestBuilder
                .setProject(project)
                .build()
            val result = validateRequest(request)

            assertInvalidResult<OutOfRangeValue<Float>>(result)
        }

        @Test
        fun `When a too high similarity threshold is provided and specified in the field mask, then the 'OutOfRangeValue' issue is returned`() {
            val project = validUpdatedProject.setSettings(
                Project.Settings.newBuilder().setSimilarityThreshold(2f).build(),
            ).build()
            val request = validUpdateRequestBuilder
                .setProject(project)
                .build()
            val result = validateRequest(request)

            assertInvalidResult<OutOfRangeValue<Float>>(result)
        }

        @Test
        fun `When an invalid similarity threshold is provided but not specified in the field mask, then no issue is returned`() {
            val project = validUpdatedProject.setSettings(
                Project.Settings.newBuilder().setSimilarityThreshold(2f).build(),
            ).build()
            val fieldMask = FieldMaskUtil.fromStringList(listOf("project.name"))
            val request = validUpdateRequestBuilder
                .setProject(project)
                .setMask(fieldMask)
                .build()
            val result = validateRequest(request)

            EitherAssert.assertThat(result).isRight()
        }
    }

    @Nested
    inner class InviteCandidatesRequest {
        @Test
        fun `When an invite candidate request is validated, it is always valid`() {
            val request = Project.InviteCandidatesRequest.getDefaultInstance()
            val result = validateRequest(request)

            EitherAssert.assertThat(result).isRight()
        }
    }

    @Nested
    inner class InviteRequest {
        private val validInviteRequestBuilder = Project.Member.Invite.newBuilder()
            .setProjectId(UUID.randomUUID().toString())
            .setUserEmail("test.user@example.com")

        @Test
        fun `When a valid request is validated, then no issue is returned`() {
            val request = validInviteRequestBuilder.build()

            val result = validateRequest(request)

            EitherAssert.assertThat(result).isRight()
        }

        @Test
        fun `When the project ID is blank, then the 'BlankField' issue is returned`() {
            val request = validInviteRequestBuilder.setProjectId("").build()

            val result = validateRequest(request)

            assertInvalidResult<InvalidId>(result)
        }

        @Test
        fun `When the project ID is invalid, then the 'InvalidId' issue is returned`() {
            val request = validInviteRequestBuilder.setProjectId("invalid-id").build()

            val result = validateRequest(request)

            assertInvalidResult<InvalidId>(result)
        }

        @Test
        fun `When the user email is invalid, then the 'InvalidEmail' issue is returned`() {
            val request = validInviteRequestBuilder.setUserEmail("invalid-email").build()

            val result = validateRequest(request)

            assertInvalidResult<InvalidEmail>(result)
        }
    }

    @Nested
    inner class AcceptRequest {
        private val validAcceptRequestBuilder = Project.Member.Accept.newBuilder()
            .setToken("valid-token")

        @Test
        fun `When a valid request is validated, then no issue is returned`() {
            val request = validAcceptRequestBuilder.build()

            val result = validateRequest(request)

            EitherAssert.assertThat(result).isRight()
        }

        @Test
        fun `When the token is blank, then the 'BlankField' issue is returned`() {
            val request = validAcceptRequestBuilder.setToken("").build()

            val result = validateRequest(request)

            assertInvalidResult<BlankField>(result)
        }
    }

    @Nested
    inner class InformationGetRequest {
        private val validId = UUID.randomUUID().toString()
        private fun getRequest(projectId: String, paths: List<String>?) = Project.Information.Get
            .newBuilder()
            .setProjectId(projectId)
            .also { if (paths != null) it.setMask(FieldMaskUtil.fromStringList(paths)) }
            .build()

        @Test
        fun `When a valid field mask and project id are validated, then no issue is returned`() {
            val request = getRequest(validId, listOf("project_progress"))
            val result = validateRequest(request)

            EitherAssert.assertThat(result).isRight()
        }

        @Test
        fun `When a blank field mask is validated, then no issue is returned`() {
            val request = getRequest(validId, null)
            val result = validateRequest(request)

            EitherAssert.assertThat(result).isRight()
        }

        @Test
        fun `When a field mask containing a nonexistent field is validated, then the 'InvalidFieldMask' issue is returned`() {
            val request = getRequest(validId, listOf("non_existent_field"))
            val result = validateRequest(request)

            assertInvalidResult<InvalidFieldMask>(result)
        }

        @Test
        fun `When an invalid ID is validated, then the 'InvalidId' issue is returned`() {
            val request = getRequest("invalid-id", null)
            val result = validateRequest(request)

            assertInvalidResult<InvalidId>(result)
        }
    }

    @Nested
    inner class MemberUpdateRequest {
        private val validMemberUpdateRequestBuilder = Project.Member.Update.newBuilder()
            .setProjectId(UUID.randomUUID().toString())
            .setUserId(UUID.randomUUID().toString())
            .setNewRole(MemberRole.MEMBER_ROLE_ADMIN)

        @Test
        fun `When a valid request is validated, the no issue is returned`() {
            val request = validMemberUpdateRequestBuilder.build()

            val result = validateRequest(request)

            EitherAssert.assertThat(result).isRight()
        }

        @Test
        fun `When the project ID is blank, then the 'InvalidId' issue is returned`() {
            val request = validMemberUpdateRequestBuilder.setProjectId("").build()

            val result = validateRequest(request)

            assertInvalidResult<InvalidId>(result)
        }

        @Test
        fun `When the project ID is invalid, then the 'InvalidId' issue is returned`() {
            val request = validMemberUpdateRequestBuilder.setProjectId("invalid-id").build()

            val result = validateRequest(request)

            assertInvalidResult<InvalidId>(result)
        }

        @Test
        fun `When the user ID is blank, then the 'InvalidId' issue is returned`() {
            val request = validMemberUpdateRequestBuilder.setUserId("").build()

            val result = validateRequest(request)

            assertInvalidResult<InvalidId>(result)
        }

        @Test
        fun `When the user ID is invalid, then the 'InvalidId' issue is returned`() {
            val request = validMemberUpdateRequestBuilder.setUserId("invalid-id").build()

            val result = validateRequest(request)

            assertInvalidResult<InvalidId>(result)
        }

        @Test
        fun `When the new role is unspecified, then the 'EnumUnspecified' issue is returned`() {
            val request = validMemberUpdateRequestBuilder.setNewRole(MemberRole.MEMBER_ROLE_UNSPECIFIED).build()

            val result = validateRequest(request)

            assertInvalidResult<EnumUnspecified>(result)
        }
    }

    @Nested
    inner class GetDecisionStatisticsRequest {
        private val validGetDecisionStatisticsRequestBuilder = Project.Information.DecisionStatistics.Get.newBuilder()
            .setProjectId(UUID.randomUUID().toString())
            .setStage(0L)

        @Test
        fun `When a valid request is validated, then no issue is returned`() {
            val request = validGetDecisionStatisticsRequestBuilder.build()

            val result = validateRequest(request)

            EitherAssert.assertThat(result).isRight()
        }

        @Test
        fun `When the stage is negative, then the 'OutOfRangeValue' issue is returned`() {
            val request = validGetDecisionStatisticsRequestBuilder.setStage(-1L).build()

            val result = validateRequest(request)

            assertInvalidResult<OutOfRangeValue<Long>>(result)
        }

        @Test
        fun `When the project ID is blank, then the 'InvalidId' issue is returned`() {
            val request = validGetDecisionStatisticsRequestBuilder.setProjectId("").build()

            val result = validateRequest(request)

            assertInvalidResult<InvalidId>(result)
        }

        @Test
        fun `When the project ID is invalid, then the 'InvalidId' issue is returned`() {
            val request = validGetDecisionStatisticsRequestBuilder.setProjectId("invalid-id").build()

            val result = validateRequest(request)

            assertInvalidResult<InvalidId>(result)
        }
    }
}
