package se.uulm.snowballr.backend.service

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
    IReviewService,
    IUserService,
    IFetcherService,
    IReadingListService,
    IPaperService,
    IProjectPaperService,
    IAuthenticationService,
    IInvitationService

/**
 * The [MainService] class serves as the primary service implementation layer that aggregates multiple sub-services.
 * This class implements the [IMainService] interface and delegates the execution of specific functionality to the
 * sub-service implementations, e.g. [ProjectService].
 *
 * @constructor Initializes the [MainService] with the required services.
 * @param projectService The service responsible for handling business logic related to projects.
 * @param criterionService The service responsible for handling business logic related to criteria.
 * @param reviewService The service responsible for handling business logic related to reviews.
 * @param userService The service responsible for handling business logic related to users.
 * @param fetcherService The service responsible for handling business logic related to fetchers.
 * @param readingListService The service responsible for handling business logic related to reading lists.
 * @param paperService The service responsible for handling business logic related to normal papers.
 * @param projectPaperService The service responsible for handling business logic related to project papers.
 * @param authenticationService The service responsible for handling business logic related to user authentication
 * @param invitationService The service responsible for handling business logic related to user invitations
 */
@Suppress("LongParameterList")
class MainService(
    private val projectService: IProjectService,
    private val criterionService: ICriterionService,
    private val reviewService: IReviewService,
    private val userService: IUserService,
    private val fetcherService: IFetcherService,
    private val readingListService: IReadingListService,
    private val paperService: IPaperService,
    private val projectPaperService: IProjectPaperService,
    private val authenticationService: IAuthenticationService,
    private val invitationService: IInvitationService,
) : IMainService,
    IProjectService by projectService,
    ICriterionService by criterionService,
    IReviewService by reviewService,
    IUserService by userService,
    IFetcherService by fetcherService,
    IReadingListService by readingListService,
    IPaperService by paperService,
    IProjectPaperService by projectPaperService,
    IAuthenticationService by authenticationService,
    IInvitationService by invitationService
