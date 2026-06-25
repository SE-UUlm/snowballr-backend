package se.uulm.snowballr.backend.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import se.uulm.snowballr.backend.model.dto.criterion.CriterionCategory
import se.uulm.snowballr.backend.model.dto.project.ProjectStatus
import se.uulm.snowballr.backend.model.dto.project.SnowballingType
import se.uulm.snowballr.backend.model.dto.projectmember.MemberRole
import se.uulm.snowballr.backend.model.dto.projectpaper.PaperDecision
import se.uulm.snowballr.backend.model.dto.review.ReviewDecision
import se.uulm.snowballr.backend.model.dto.user.UserRole
import se.uulm.snowballr.backend.model.dto.user.UserStatus

/**
 * This class contains tests that ensure that the ordinal values of the enums have the same value as they were expected.
 * This prevents the values from changing over time, which might cause problems with database migration, if the enum is
 * stored in the database.
 *
 * If a new enum value is added, then it should be put at the end so that it has the next greater ordinal value.
 */
class EnumOrdinalTest {
    @Test
    fun `When the UserStatus values are read, then they have the expected ordinal values`() {
        for (value in UserStatus.entries) {
            val expectedOrdinal =
                when (value) {
                    UserStatus.USER_STATUS_ACTIVE_UNCONFIRMED -> 0
                    UserStatus.USER_STATUS_ACTIVE -> 1
                    UserStatus.USER_STATUS_DELETED -> 2
                    UserStatus.USER_STATUS_CLEARED -> 3
                }

            assertEquals(expectedOrdinal, value.ordinal)
        }
    }

    @Test
    fun `When the UserRole values are read, then they have the expected ordinal values`() {
        for (value in UserRole.entries) {
            val expectedOrdinal =
                when (value) {
                    UserRole.USER_ROLE_DEFAULT -> 0
                    UserRole.USER_ROLE_ADMIN -> 1
                }

            assertEquals(expectedOrdinal, value.ordinal)
        }
    }

    @Test
    fun `When the ProjectStatus values are read, then they have the expected ordinal values`() {
        for (value in ProjectStatus.entries) {
            val expectedOrdinal =
                when (value) {
                    ProjectStatus.PROJECT_STATUS_ACTIVE -> 0
                    ProjectStatus.PROJECT_STATUS_ACTIVE_LOCKED -> 1
                    ProjectStatus.PROJECT_STATUS_ARCHIVED -> 2
                    ProjectStatus.PROJECT_STATUS_DELETED -> 3
                    ProjectStatus.PROJECT_STATUS_CLEARED -> 4
                }

            assertEquals(expectedOrdinal, value.ordinal)
        }
    }

    @Test
    fun `When the SnowballingType values are read, then they have the expected ordinal values`() {
        for (value in SnowballingType.entries) {
            val expectedOrdinal =
                when (value) {
                    SnowballingType.SNOWBALLING_TYPE_FORWARD -> 0
                    SnowballingType.SNOWBALLING_TYPE_BACKWARD -> 1
                    SnowballingType.SNOWBALLING_TYPE_BOTH -> 2
                }

            assertEquals(expectedOrdinal, value.ordinal)
        }
    }

    @Test
    fun `When the CriterionCategory values are read, then they have the expected ordinal values`() {
        for (value in CriterionCategory.entries) {
            val expectedOrdinal =
                when (value) {
                    CriterionCategory.CRITERION_CATEGORY_INCLUSION -> 0
                    CriterionCategory.CRITERION_CATEGORY_EXCLUSION -> 1
                    CriterionCategory.CRITERION_CATEGORY_HARD_EXCLUSION -> 2
                }

            assertEquals(expectedOrdinal, value.ordinal)
        }
    }

    @Test
    fun `When the ReviewDecision values are read, then they have the expected ordinal values`() {
        for (value in ReviewDecision.entries) {
            val expectedOrdinal =
                when (value) {
                    ReviewDecision.REVIEW_DECISION_DECLINED -> 0
                    ReviewDecision.REVIEW_DECISION_MAYBE -> 1
                    ReviewDecision.REVIEW_DECISION_ACCEPTED -> 2
                }

            assertEquals(expectedOrdinal, value.ordinal)
        }
    }

    @Test
    fun `When the PaperDecision values are read, then they have the expected ordinal values`() {
        for (value in PaperDecision.entries) {
            val expectedOrdinal =
                when (value) {
                    PaperDecision.PAPER_DECISION_UNREVIEWED -> 0
                    PaperDecision.PAPER_DECISION_IN_REVIEW -> 1
                    PaperDecision.PAPER_DECISION_DECLINED -> 2
                    PaperDecision.PAPER_DECISION_ACCEPTED -> 3
                }

            assertEquals(expectedOrdinal, value.ordinal)
        }
    }

    @Test
    fun `When the MemberRole values are read, then they have the expected ordinal values`() {
        for (value in MemberRole.entries) {
            val expectedOrdinal =
                when (value) {
                    MemberRole.MEMBER_ROLE_DEFAULT -> 0
                    MemberRole.MEMBER_ROLE_ADMIN -> 1
                }

            assertEquals(expectedOrdinal, value.ordinal)
        }
    }
}
