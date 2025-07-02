package se.uulm.snowballr.backend.service

import se.uulm.snowballr.backend.auth.IJwtService
import se.uulm.snowballr.backend.fetcher.FetcherManager
import se.uulm.snowballr.backend.repository.ICriterionTableRepo
import se.uulm.snowballr.backend.repository.IProjectTableRepo
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
    IFetcherService

/**
 * The [MainService] class serves as the primary service implementation layer that aggregates multiple sub-services.
 * This class implements the [IMainService] interface and delegates the execution of specific functionality to the
 * sub-service implementations, e.g. [ProjectService].
 *
 * @constructor Initializes the [MainService] with the required repositories.
 * @param projectRepo The repository responsible for handling persistence operations related to projects.
 * @param criterionRepo The repository responsible for handling persistence operations related to criteria.
 * @param userRepo The repository responsible for handling persistence operations related to users.
 * @param projectMemberRepo The repository responsible for handling persistence operations related to project members.
 * @param jwtService The utility for handling JWT operations, such as token parsing and validation.
 * @param fetcherManager The manager responsible for making fetchers available for use.
 */
class MainService(
    private val projectRepo: IProjectTableRepo,
    private val criterionRepo: ICriterionTableRepo,
    private val userRepo: IUserTableRepo,
    private val projectMemberRepo: IProjectMemberTableRepo,
    private val jwtService: IJwtService,
    private val fetcherManager: FetcherManager,
) : IMainService,
    IProjectService by ProjectService(projectRepo, userRepo, projectMemberRepo),
    ICriterionService by CriterionService(criterionRepo, userRepo, projectRepo, projectMemberRepo),
    IUserService by UserService(userRepo, projectMemberRepo, jwtService),
    IFetcherService by FetcherService(fetcherManager)
