package se.uulm.snowballr.backend.grpc

import com.google.protobuf.util.FieldMaskUtil
import io.github.oshai.kotlinlogging.KotlinLogging
import io.grpc.Server
import io.grpc.ServerBuilder
import io.grpc.health.v1.HealthCheckResponse.ServingStatus
import io.grpc.protobuf.services.HealthStatusManager
import io.grpc.protobuf.services.ProtoReflectionService
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import se.uulm.snowballr.backend.auth.GrpcContext
import se.uulm.snowballr.backend.grpc.GrpcHelpers.parseCriterionId
import se.uulm.snowballr.backend.grpc.GrpcHelpers.parsePaperId
import se.uulm.snowballr.backend.grpc.GrpcHelpers.parseProjectId
import se.uulm.snowballr.backend.grpc.GrpcHelpers.parseProjectPaperId
import se.uulm.snowballr.backend.grpc.GrpcHelpers.parseReviewId
import se.uulm.snowballr.backend.grpc.GrpcHelpers.parseUserId
import se.uulm.snowballr.backend.grpc.GrpcHelpers.returnBoolValue
import se.uulm.snowballr.backend.grpc.GrpcHelpers.returnNothing
import se.uulm.snowballr.backend.grpc.interceptor.authenticationInterceptor
import se.uulm.snowballr.backend.grpc.interceptor.exceptionInterceptor
import se.uulm.snowballr.backend.grpc.interceptor.loggingInterceptor
import se.uulm.snowballr.backend.grpc.interceptor.validationInterceptor
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.dto.criterion.CriterionCategory
import se.uulm.snowballr.backend.model.dto.criterion.toGrpcCriteria
import se.uulm.snowballr.backend.model.dto.criterion.toGrpcCriterion
import se.uulm.snowballr.backend.model.dto.user.UserRole
import se.uulm.snowballr.backend.model.dto.user.UserStatus
import se.uulm.snowballr.backend.model.incoming.criterion.CreateCriterionRequest
import se.uulm.snowballr.backend.model.incoming.criterion.UpdateCriterionRequest
import se.uulm.snowballr.backend.model.incoming.user.RegisterRequest
import se.uulm.snowballr.backend.model.incoming.user.UpdateUserRequest
import se.uulm.snowballr.backend.model.parseUUID
import se.uulm.snowballr.backend.scheduler.SchedulerManager
import se.uulm.snowballr.backend.service.IAuthenticationService
import se.uulm.snowballr.backend.service.ICriterionService
import se.uulm.snowballr.backend.service.IExportService
import se.uulm.snowballr.backend.service.IFetcherService
import se.uulm.snowballr.backend.service.IInvitationService
import se.uulm.snowballr.backend.service.IPaperService
import se.uulm.snowballr.backend.service.IProjectMemberService
import se.uulm.snowballr.backend.service.IProjectPaperService
import se.uulm.snowballr.backend.service.IProjectService
import se.uulm.snowballr.backend.service.IReadingListService
import se.uulm.snowballr.backend.service.IReviewService
import se.uulm.snowballr.backend.service.IUserService
import snowballr.Authentication
import snowballr.Base
import snowballr.CriterionOuterClass
import snowballr.Export
import snowballr.Fetcher
import snowballr.PaperOuterClass
import snowballr.ProjectOuterClass
import snowballr.ReviewOuterClass
import snowballr.SnowballRGrpcKt
import snowballr.UserOuterClass
import snowballr.UserSettingsOuterClass
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}

/**
 * Name of the main gRPC service.
 */
private val SERVICE_NAME = SnowballRGrpcKt.serviceDescriptor.name

/**
 * The maximum time to wait for the termination of the server in seconds.
 */
private const val TERMINATION_TIMEOUT_SECONDS = 30L

/**
 * Represents a gRPC server to handle requests and responses for the SnowballR application.
 *
 * @param port The port on which the server should run.
 */
class SnowballRServer(
    private val port: Int,
) {
    /**
     * Manages the health status of the gRPC server.
     *
     * This manager is responsible for tracking and updating the health status
     * of the server to ensure proper monitoring and diagnostics. It interacts
     * with gRPC health checks to report the server's state, such as
     * [ServingStatus.SERVING] and [ServingStatus.NOT_SERVING].
     */
    private val healthManager: HealthStatusManager = HealthStatusManager()

    /**
     * Enables and provides gRPC reflection for the server.
     *
     * gRPC reflection allows clients and tools to dynamically discover the API of the server at runtime,
     * providing metadata about the available services, methods, and message types. This is particularly useful
     * for debugging, development, and command-line tools that interact with gRPC servers without requiring
     * precompiled service definitions.
     *
     * **Note:** ProtoReflectionServiceV1 does not work - calls are not registered by the server.
     */
    @Suppress("Deprecated")
    private val reflectionService = ProtoReflectionService.newInstance()

    /**
     * Manages the scheduling of tasks in the application.
     *
     * This manager is responsible for scheduling tasks in the application, such as periodic maintenance jobs.
     * It is used to start and stop the scheduler when the server starts and stops, respectively.
     */
    private val schedulerManager = SchedulerManager()

    /**
     * Represents the gRPC server instance used for handling incoming requests.
     *
     * This server is configured to:
     * - Listen on the specified port.
     * - Apply a logging interceptor to intercept and log incoming requests.
     * - Apply an authentication interceptor to handle user authentication.
     * - Apply a validation interceptor to intercept and validate incoming requests.
     * - Apply an exception interceptor to catch exceptions and provide appropriate status codes.
     * - Register the gRPC service implementation [SnowballRService].
     * - Register the gRPC health service implementation [HealthStatusManager.healthService].
     */
    private val server: Server =
        ServerBuilder
            .forPort(port)
            // Interceptors in reverse order of execution
            .intercept(exceptionInterceptor)
            .intercept(validationInterceptor)
            .intercept(authenticationInterceptor)
            .intercept(loggingInterceptor)
            // Services
            .addService(SnowballRService())
            .addService(healthManager.healthService)
            .addService(reflectionService)
            .build()

    /**
     * Starts the gRPC server and adds a shutdown hook to cleanly stop the server when the JVM shuts down.
     *
     * This method performs the following operations:
     * - Starts the gRPC server, making it listen on the specified port.
     * - Starts the [SchedulerManager] to handle scheduling of tasks.
     * - Registers a shutdown hook to handle the server shutdown process when the JVM is shutting down.
     *   The shutdown hook stops the server and confirms the server has been shut down.
     */
    fun start() {
        server.start()
        schedulerManager.start()
        logger.info { "Server started, listening on $port" }
        Runtime.getRuntime().addShutdownHook(
            Thread {
                logger.info { "*** shutting down gRPC server since JVM is shutting down" }
                healthManager.enterTerminalState()
                schedulerManager.stop()
                this@SnowballRServer.stop()
                logger.info { "*** server shut down" }
            },
        )

        healthManager.setStatus(SERVICE_NAME, ServingStatus.SERVING)
    }

    /**
     * Stops the gRPC server.
     *
     * This method shuts down the server instance to release resources and
     * terminate any ongoing server operations. It is typically invoked
     * during application shutdown or in a dedicated cleanup process.
     */
    fun stop() {
        server.shutdown().awaitTermination(TERMINATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
    }

    /**
     * Blocks the current thread until the gRPC server is terminated.
     *
     * This method waits for the gRPC server instance to shut down, effectively
     * keeping the application running until the server stops. It is typically used
     * in the main application entry point to ensure that the server stays alive and
     * processes incoming requests until explicitly stopped.
     */
    fun blockUntilShutdown() {
        server.awaitTermination()
    }

    /**
     * Service implementation for handling SnowballR operations.
     *
     * This class extends [SnowballRGrpcKt.SnowballRCoroutineImplBase] and provides coroutine-based
     * implementations for various SnowballR service methods. These methods cover functionalities like
     * user management, project management, settings management, paper management, review management,
     * and criterion management.
     *
     * **Note:** Each method corresponds to a gRPC endpoint, taking specific request types and returning
     * appropriate response types as defined by the respective protocol buffers.
     */
    @Suppress("TooManyFunctions")
    class SnowballRService :
        SnowballRGrpcKt.SnowballRCoroutineImplBase(),
        KoinComponent {
        private val authenticationService: IAuthenticationService by inject()
        private val criterionService: ICriterionService by inject()
        private val exportService: IExportService by inject()
        private val fetcherService: IFetcherService by inject()
        private val invitationService: IInvitationService by inject()
        private val paperService: IPaperService by inject()
        private val projectMemberService: IProjectMemberService by inject()
        private val projectPaperService: IProjectPaperService by inject()
        private val projectService: IProjectService by inject()
        private val readingListService: IReadingListService by inject()
        private val reviewService: IReviewService by inject()
        private val userService: IUserService by inject()

        override suspend fun getAvailableFetchers(request: Base.Nothing): Fetcher.AvailableFetchers =
            fetcherService.getAvailableFetchers()

        override suspend fun register(request: Authentication.RegisterRequest) = returnNothing {
            userService.register(
                RegisterRequest(
                    firstName = request.firstName,
                    lastName = request.lastName,
                    email = request.email,
                    password = request.password,
                ),
            )
        }

        override suspend fun verifyEmail(request: Authentication.VerifyEmailRequest) = returnNothing {
            authenticationService.verifyEmail(request)
        }

        override suspend fun login(request: Authentication.LoginRequest) = returnNothing {
            authenticationService.login(request)
        }

        override suspend fun logout(request: Base.Nothing) = returnNothing {
            authenticationService.logout()
        }

        override suspend fun getAuthenticationStatus(
            request: Base.Nothing,
        ): Authentication.AuthenticationStatusResponse = Authentication.AuthenticationStatusResponse.newBuilder()
            .setAuthenticationStatus(GrpcContext.getAuthenticationStatusFromContext()).build()

        /** Renew Session is handled in the [authenticationInterceptor]. */
        override suspend fun renewSession(request: Base.Nothing): Base.Nothing = Base.Nothing.getDefaultInstance()

        override suspend fun requestPasswordReset(request: Authentication.RequestPasswordResetRequest): Base.Nothing =
            super.requestPasswordReset(request)

        override suspend fun resetPassword(request: Authentication.PasswordResetRequest): Base.Nothing =
            super.resetPassword(request)

        override suspend fun changePassword(request: Authentication.PasswordChangeRequest) = returnNothing {
            authenticationService.changePassword(request)
        }

        override suspend fun getAllUsers(request: Base.Nothing): UserOuterClass.User.List = userService.getAllUsers()

        override suspend fun getCurrentUser(request: Base.Nothing): UserOuterClass.User = userService.getCurrentUser()

        override suspend fun getUserById(request: Base.Id): UserOuterClass.User =
            userService.getUserById(parseUserId(request))

        override suspend fun getUserByEmail(request: Base.Email): UserOuterClass.User =
            userService.getUserByEmail(request.email)

        override suspend fun updateUser(request: UserOuterClass.User.Update): UserOuterClass.User =
            userService.updateUser(
                UpdateUserRequest(
                    userId = parseUUID(request.user.id, EntityType.USER),
                    firstName = request.user.firstName,
                    lastName = request.user.lastName,
                    email = request.user.email,
                    role = UserRole.fromGrpc(request.user.role),
                    status = UserStatus.fromGrpc(request.user.status),
                ),
                FieldMaskUtil.normalize(request.mask).pathsList,
            )

        override suspend fun softDeleteUser(request: Base.Id) = returnNothing {
            userService.softDeleteUser(parseUserId(request))
        }

        override suspend fun softUndeleteUser(request: Base.Id): Base.Nothing = super.softUndeleteUser(request)

        override suspend fun getAllPapersToReview(request: Base.Nothing): ProjectOuterClass.Project.Paper.List =
            super.getAllPapersToReview(request)

        override suspend fun getPapersToReviewForProject(request: Base.Id): ProjectOuterClass.Project.Paper.List =
            projectPaperService.getPapersToReviewForProject(parseProjectId(request))

        override suspend fun getNextPaper(request: Base.Id): ProjectOuterClass.Project.Paper =
            projectPaperService.getNextPaper(parseProjectPaperId(request))

        override suspend fun getNextPaperToReview(request: Base.Id): ProjectOuterClass.Project.Paper =
            projectPaperService.getNextPaperToReview(parseProjectPaperId(request))

        override suspend fun getPreviousPaper(request: Base.Id): ProjectOuterClass.Project.Paper =
            projectPaperService.getPreviousPaper(parseProjectPaperId(request))

        override suspend fun getUserSettings(request: Base.Nothing): UserSettingsOuterClass.UserSettings =
            userService.getUserSettings()

        override suspend fun updateUserSettings(
            request: UserSettingsOuterClass.UserSettings.Update,
        ): UserSettingsOuterClass.UserSettings = super.updateUserSettings(request)

        override suspend fun getReadingList(request: Base.Nothing): PaperOuterClass.Paper.List =
            readingListService.getReadingList()

        override suspend fun isPaperOnReadingList(request: Base.Id) = returnBoolValue {
            readingListService.isPaperOnReadingList(parsePaperId(request))
        }

        override suspend fun addPaperToReadingList(request: Base.Id) = returnNothing {
            readingListService.addPaperToReadingList(parsePaperId(request))
        }

        override suspend fun removePaperFromReadingList(request: Base.Id) = returnNothing {
            readingListService.removePaperFromReadingList(parsePaperId(request))
        }

        override suspend fun getInviteCandidates(
            request: ProjectOuterClass.Project.InviteCandidatesRequest,
        ): UserOuterClass.User.List = invitationService.getInviteCandidates(request)

        override suspend fun inviteUserToProject(request: ProjectOuterClass.Project.Member.Invite) = returnNothing {
            invitationService.inviteUserToProject(request)
        }

        override suspend fun acceptProjectInvitation(request: ProjectOuterClass.Project.Member.Accept) = returnNothing {
            invitationService.acceptProjectInvitation(request)
        }

        override suspend fun getPendingInvitationsForProject(request: Base.Id): UserOuterClass.User.List =
            invitationService.getPendingInvitationsForProject(parseProjectId(request))

        override suspend fun getProjectMembers(request: Base.Id): ProjectOuterClass.Project.Member.List =
            projectMemberService.getProjectMembers(parseProjectId(request))

        override suspend fun removeProjectMember(request: ProjectOuterClass.Project.Member.Remove) = returnNothing {
            projectMemberService.removeProjectMember(request)
        }

        override suspend fun getAllProjects(request: Base.Nothing): ProjectOuterClass.Project.List =
            projectService.getAllProjects()

        override suspend fun getAllDeletedProjects(request: Base.Nothing): ProjectOuterClass.Project.List =
            super.getAllDeletedProjects(request)

        override suspend fun getAllDeletedProjectsForUser(request: Base.Id): ProjectOuterClass.Project.List =
            projectService.getAllDeletedProjectsForUser(parseUserId(request))

        override suspend fun getAllArchivedProjects(request: Base.Nothing): ProjectOuterClass.Project.List =
            super.getAllArchivedProjects(request)

        override suspend fun getAllProjectsForUser(request: Base.Id): ProjectOuterClass.Project.List =
            projectService.getAllProjectsForUser(parseUserId(request))

        override suspend fun getAllArchivedProjectsForUser(request: Base.Id): ProjectOuterClass.Project.List =
            projectService.getAllArchivedProjectsForUser(parseUserId(request))

        override suspend fun createProject(request: ProjectOuterClass.Project.Create): ProjectOuterClass.Project =
            projectService.createProject(request)

        override suspend fun getProjectById(request: Base.Id): ProjectOuterClass.Project =
            projectService.getProjectById(parseProjectId(request))

        override suspend fun updateProject(request: ProjectOuterClass.Project.Update): ProjectOuterClass.Project =
            projectService.updateProject(request)

        override suspend fun getAvailableExportFormats(request: Base.Nothing): Export.AvailableExportFormatsResponse =
            exportService.getAvailableExportFormats()

        override suspend fun exportProject(request: Export.ExportRequest): Export.ExportResponse =
            exportService.exportProject(request)

        override suspend fun softDeleteProject(request: Base.Id) = returnNothing {
            projectService.softDeleteProject(parseProjectId(request))
        }

        override suspend fun softUndeleteProject(request: Base.Id): Base.Nothing = super.softUndeleteProject(request)

        override suspend fun getProjectInformation(
            request: ProjectOuterClass.Project.Information.Get,
        ): ProjectOuterClass.Project.Information = projectService.getProjectInformation(request)

        override suspend fun getDecisionStatisticsForStage(
            request: ProjectOuterClass.Project.Information.DecisionStatistics.Get,
        ): ProjectOuterClass.Project.Information.DecisionStatistics =
            projectService.getDecisionStatisticsForStage(request)

        override suspend fun updateProjectMemberRole(request: ProjectOuterClass.Project.Member.Update) = returnNothing {
            projectMemberService.updateProjectMemberRole(request)
        }

        override suspend fun getCriterionById(request: Base.Id): CriterionOuterClass.Criterion =
            criterionService.getCriterionById(parseCriterionId(request)).toGrpcCriterion()

        override suspend fun getAllCriteriaForProject(request: Base.Id): CriterionOuterClass.Criterion.List =
            criterionService.getAllCriteriaForProject(parseProjectId(request)).toGrpcCriteria()

        override suspend fun createCriterion(
            request: CriterionOuterClass.Criterion.Create,
        ): CriterionOuterClass.Criterion {
            val projectId = if (request.projectId.isNotEmpty()) {
                parseUUID(request.projectId, EntityType.PROJECT)
            } else {
                null
            }

            return criterionService.createCriterion(
                CreateCriterionRequest(
                    tag = request.tag,
                    name = request.name,
                    description = request.description,
                    category = CriterionCategory.fromGrpc(request.category),
                    projectId = projectId,
                ),
            ).toGrpcCriterion()
        }

        override suspend fun updateCriterion(
            request: CriterionOuterClass.Criterion.Update,
        ): CriterionOuterClass.Criterion = criterionService.updateCriterion(
            UpdateCriterionRequest(
                criterionId = parseUUID(request.criterion.id, EntityType.CRITERION),
                tag = request.criterion.tag,
                name = request.criterion.name,
                description = request.criterion.description,
                category = CriterionCategory.fromGrpc(request.criterion.category),
            ),
            FieldMaskUtil.normalize(request.mask).pathsList,
        ).toGrpcCriterion()

        override suspend fun deleteCriterion(request: Base.Id): Base.Nothing = super.deleteCriterion(request)

        override suspend fun getProjectPaperById(request: Base.Id): ProjectOuterClass.Project.Paper =
            projectPaperService.getProjectPaperById(parseProjectPaperId(request))

        override suspend fun getProjectPaperByRelativeId(
            request: ProjectOuterClass.Project.Paper.Get,
        ): ProjectOuterClass.Project.Paper = projectPaperService.getProjectPaperByRelativeId(request)

        override suspend fun getAllProjectPapersForProject(request: Base.Id): ProjectOuterClass.Project.Paper.List =
            projectPaperService.getAllProjectPapersForProject(parseProjectId(request))

        override suspend fun addPaperToProject(
            request: ProjectOuterClass.Project.Paper.Add,
        ): ProjectOuterClass.Project.Paper = projectPaperService.addPaperToProject(request)

        override suspend fun updateProjectPaper(
            request: ProjectOuterClass.Project.Paper.Update,
        ): ProjectOuterClass.Project.Paper = super.updateProjectPaper(request)

        override suspend fun removePaperFromProject(request: Base.Id): Base.Nothing =
            super.removePaperFromProject(request)

        override suspend fun getReviewById(request: Base.Id): ReviewOuterClass.Review =
            reviewService.getReviewById(parseReviewId(request))

        override suspend fun getAllReviewsForProjectPaper(request: Base.Id): ReviewOuterClass.Review.List =
            reviewService.getAllReviewsForProjectPaper(parseProjectPaperId(request))

        override suspend fun createReview(request: ReviewOuterClass.Review.Create): ReviewOuterClass.Review =
            reviewService.createReview(request)

        override suspend fun updateReview(request: ReviewOuterClass.Review.Update): ReviewOuterClass.Review =
            super.updateReview(request)

        override suspend fun deleteReview(request: Base.Id): Base.Nothing = super.deleteReview(request)

        override suspend fun getPaperById(request: Base.Id): PaperOuterClass.Paper =
            paperService.getPaperById(parsePaperId(request))

        override suspend fun searchLocalProjectPaperCandidates(
            request: ProjectOuterClass.Project.Paper.SearchQuery,
        ): PaperOuterClass.Paper.List = fetcherService.searchLocalProjectPaperCandidates(request)

        override suspend fun searchFetcherProjectPaperCandidates(
            request: ProjectOuterClass.Project.Paper.SearchQuery,
        ): PaperOuterClass.Paper.List = fetcherService.searchFetcherProjectPaperCandidates(request)

        override suspend fun createPaper(request: PaperOuterClass.Paper): PaperOuterClass.Paper =
            paperService.createPaper(request)

        override suspend fun updatePaper(request: PaperOuterClass.Paper.Update): PaperOuterClass.Paper =
            paperService.updatePaper(request)

        override suspend fun getForwardReferencedPapers(request: Base.Id): PaperOuterClass.Paper.List =
            paperService.getForwardReferencedPapers(parsePaperId(request))

        override suspend fun getBackwardReferencedPapers(request: Base.Id): PaperOuterClass.Paper.List =
            paperService.getBackwardReferencedPapers(parsePaperId(request))

        override suspend fun getPaperPdf(request: Base.Id): Base.Blob = super.getPaperPdf(request)

        override suspend fun setPaperPdf(request: PaperOuterClass.Paper.PdfUpdate): Base.Nothing =
            super.setPaperPdf(request)
    }
}
