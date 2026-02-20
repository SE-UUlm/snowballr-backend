package se.uulm.snowballr.backend.validation

import com.google.protobuf.FieldMask
import com.google.protobuf.util.FieldMaskUtil
import `in`.rcard.assertj.arrowcore.EitherAssert
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import se.uulm.snowballr.backend.model.BlankField
import se.uulm.snowballr.backend.model.EnumUnspecified
import se.uulm.snowballr.backend.model.InvalidFieldMask
import se.uulm.snowballr.backend.model.InvalidId
import se.uulm.snowballr.backend.model.OutOfRangeValue
import se.uulm.snowballr.backend.model.TooLongField
import se.uulm.snowballr.backend.validation.ProjectValidator.NAME_MAX_LENGTH
import se.uulm.snowballr.backend.validation.ProjectValidator.NUMBER_OF_REVIEWERS_MAX_VALUE
import se.uulm.snowballr.backend.validation.ProjectValidator.NUMBER_OF_REVIEWERS_MIN_VALUE
import se.uulm.snowballr.backend.validation.ProjectValidator.SIMILARITY_THRESHOLD_MAX_VALUE
import se.uulm.snowballr.backend.validation.ProjectValidator.SIMILARITY_THRESHOLD_MIN_VALUE
import snowballr.ProjectOuterClass
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
        private val validUpdatedDecisionMatrix: ProjectOuterClass.ReviewDecisionMatrix.Builder =
            ProjectOuterClass.ReviewDecisionMatrix.newBuilder()
                .setNumberOfReviewers(3)

        private val validUpdatedProjectSettings: Project.Settings.Builder = Project.Settings.newBuilder()
            .setSimilarityThreshold(1F)
            .setSnowballingType(SnowballingType.SNOWBALLING_TYPE_FORWARD)
            .setReviewMaybeAllowed(false)
            .setDecisionMatrix(validUpdatedDecisionMatrix.build())

        private val validUpdatedProject: Project.Builder = Project.newBuilder()
            .setId(UUID.randomUUID().toString())
            .setName("Test Project")
            .setStatus(ProjectStatus.PROJECT_STATUS_ARCHIVED)
            .setSettings(validUpdatedProjectSettings.build())

        private val validFieldMask: FieldMask = FieldMaskUtil
            .fromStringList(
                listOf(
                    "project.name",
                    "project.status",
                    "project.settings.similarity_threshold",
                    "project.settings.snowballing_type",
                    "project.settings.review_maybe_allowed",
                    "project.settings.decision_matrix.number_of_reviewers",
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
                validUpdatedProjectSettings
                    .setSnowballingType(SnowballingType.SNOWBALLING_TYPE_UNSPECIFIED)
                    .build(),
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
                validUpdatedProjectSettings
                    .setSnowballingType(SnowballingType.SNOWBALLING_TYPE_UNSPECIFIED)
                    .build(),
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
                validUpdatedProjectSettings
                    .setSimilarityThreshold(SIMILARITY_THRESHOLD_MIN_VALUE - 1f)
                    .build(),
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
                validUpdatedProjectSettings
                    .setSimilarityThreshold(SIMILARITY_THRESHOLD_MAX_VALUE + 1f)
                    .build(),
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
                validUpdatedProjectSettings
                    .setSimilarityThreshold(SIMILARITY_THRESHOLD_MAX_VALUE + 1f)
                    .build(),
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
        fun `When a too low number of reviewers is provided and specified in the field mask, then the 'OutOfRangeValue' issue is returned`() {
            val project = validUpdatedProject.setSettings(
                validUpdatedProjectSettings.setDecisionMatrix(
                    validUpdatedDecisionMatrix
                        .setNumberOfReviewers(NUMBER_OF_REVIEWERS_MIN_VALUE - 1)
                        .build(),
                ).build(),
            ).build()
            val request = validUpdateRequestBuilder
                .setProject(project)
                .build()
            val result = validateRequest(request)

            assertInvalidResult<OutOfRangeValue<Int>>(result)
        }

        @Test
        fun `When a too high number of reviewers is provided and specified in the field mask, then the 'OutOfRangeValue' issue is returned`() {
            val project = validUpdatedProject.setSettings(
                validUpdatedProjectSettings.setDecisionMatrix(
                    validUpdatedDecisionMatrix
                        .setNumberOfReviewers(NUMBER_OF_REVIEWERS_MAX_VALUE + 1)
                        .build(),
                ).build(),
            ).build()
            val request = validUpdateRequestBuilder
                .setProject(project)
                .build()
            val result = validateRequest(request)

            assertInvalidResult<OutOfRangeValue<Int>>(result)
        }

        @Test
        fun `When an invalid number of reviewers is provided but not specified in the field mask, then no issue is returned`() {
            val project = validUpdatedProject.setSettings(
                validUpdatedProjectSettings.setDecisionMatrix(
                    validUpdatedDecisionMatrix
                        .setNumberOfReviewers(NUMBER_OF_REVIEWERS_MAX_VALUE + 1)
                        .build(),
                ).build(),
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
        fun `When an invalid number of reviewers is provided but only decision matrix patterns are specified in the field mask, then no issue is returned`() {
            val project = validUpdatedProject.setSettings(
                validUpdatedProjectSettings.setDecisionMatrix(
                    validUpdatedDecisionMatrix
                        .setNumberOfReviewers(NUMBER_OF_REVIEWERS_MAX_VALUE + 1)
                        .build(),
                ).build(),
            ).build()
            val fieldMask = FieldMaskUtil.fromStringList(listOf("project.settings.decision_matrix.patterns"))
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
