package se.uulm.snowballr.backend.access

/**
 * The [IAccessChecker] interface defines the contract for access checking in the SnowballR backend.
 */
interface IAccessChecker :
    IProjectAccessChecker,
    ICriterionAccessChecker,
    IReviewAccessChecker,
    IUserAccessChecker,
    IProjectMemberAccessChecker,
    IProjectPaperAccessChecker

/**
 * The [AccessChecker] class is a concrete implementation of the [IAccessChecker] interface.
 * It delegates access checking responsibilities to specific access checkers for projects, criteria, reviews, and users.
 *
 * @param projectAccessChecker The access checker for project-related access control.
 * @param criterionAccessChecker The access checker for criterion-related access control.
 * @param reviewAccessChecker The access checker for review-related access control.
 * @param userAccessChecker The access checker for user-related access control.
 * @param projectMemberAccessChecker The access checker for project member-related access control.
 * @param projectPaperAccessChecker The access checker for project paper-related access control.
 */
class AccessChecker(
    private val projectAccessChecker: IProjectAccessChecker,
    private val criterionAccessChecker: ICriterionAccessChecker,
    private val reviewAccessChecker: IReviewAccessChecker,
    private val userAccessChecker: IUserAccessChecker,
    private val projectMemberAccessChecker: IProjectMemberAccessChecker,
    private val projectPaperAccessChecker: IProjectPaperAccessChecker,
) : IAccessChecker,
    IProjectAccessChecker by projectAccessChecker,
    ICriterionAccessChecker by criterionAccessChecker,
    IReviewAccessChecker by reviewAccessChecker,
    IUserAccessChecker by userAccessChecker,
    IProjectMemberAccessChecker by projectMemberAccessChecker,
    IProjectPaperAccessChecker by projectPaperAccessChecker
