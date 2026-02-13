package se.uulm.snowballr.backend.service.accessrules

/**
 * The [IAccessChecker] interface defines the contract for access checking in the SnowballR backend.
 */
interface IAccessChecker :
    IProjectAccessChecker

class AccessChecker(
    private val projectAccessChecker: IProjectAccessChecker,
) : IAccessChecker,
    IProjectAccessChecker by projectAccessChecker
