package se.uulm.snowballr.backend.service

import se.uulm.snowballr.backend.repository.ICriterionTableRepo
import se.uulm.snowballr.backend.repository.IProjectTableRepo
import se.uulm.snowballr.backend.repository.ISessionTableRepo
import se.uulm.snowballr.backend.repository.IUserTableRepo
import se.uulm.snowballr.backend.repository.association.IProjectMemberTableRepo

/**
 * The [IMainService] interface provides a unified contract that combines the responsibilities of all sub-services. It
 * inherits functionality from their interfaces, making it possible to handle CRUD operations related to various
 * entities through a single interface.
 *
 * Classes implementing [IMainService] can delegate the actual implementations of the sub-service interfaces for
 * modularity and separation of concerns. This design supports scalability and maintainability by consolidating the
 * service layer functionality.
 */
interface IMainService :
    IProjectService,
    ICriterionService,
    IUserService,
    ISessionService

/**
 * The [MainService] class serves as the primary service implementation layer that aggregates multiple sub-services.
 * This class implements the [IMainService] interface and delegates the execution of specific functionality to the
 * sub-service implementations, e.g. [ProjectService].
 *
 * @constructor Initializes the [MainService] with the required repositories.
 * @param projectRepo The repository responsible for handling persistence operations related to projects.
 * @param criterionRepo The repository responsible for handling persistence operations related to criteria.
 * @param userRepo The repository responsible for handling persistence operations related to users.
 * @param sessionRepo The repository responsible for handling persistence operations related to sessions.
 * @param projectMemberRepo The repository responsible for handling persistence operations related to project members.
 */
class MainService(
    private val projectRepo: IProjectTableRepo,
    private val criterionRepo: ICriterionTableRepo,
    private val userRepo: IUserTableRepo,
    private val sessionRepo: ISessionTableRepo,
    private val projectMemberRepo: IProjectMemberTableRepo,
) : IMainService,
    IProjectService by ProjectService(projectRepo, userRepo),
    ICriterionService by CriterionService(criterionRepo),
    IUserService by UserService(userRepo, sessionRepo, projectMemberRepo),
    ISessionService by SessionService(sessionRepo)
