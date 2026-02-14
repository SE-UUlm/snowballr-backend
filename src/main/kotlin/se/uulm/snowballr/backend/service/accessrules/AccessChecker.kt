package se.uulm.snowballr.backend.service.accessrules

/**
 * The [IAccessChecker] interface defines the contract for access checking in the SnowballR backend.
 */
interface IAccessChecker :
    IProjectAccessChecker,
    ICriterionAccessChecker

class AccessChecker(
    private val projectAccessChecker: IProjectAccessChecker,
    private val criterionAccessChecker: ICriterionAccessChecker,
) : IAccessChecker,
    IProjectAccessChecker by projectAccessChecker,
    ICriterionAccessChecker by criterionAccessChecker
