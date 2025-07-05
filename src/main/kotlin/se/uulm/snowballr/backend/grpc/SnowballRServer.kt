package se.uulm.snowballr.backend.grpc

import io.github.oshai.kotlinlogging.KotlinLogging
import io.grpc.Server
import io.grpc.ServerBuilder
import io.grpc.health.v1.HealthCheckResponse.ServingStatus
import io.grpc.protobuf.services.HealthStatusManager
import io.grpc.protobuf.services.ProtoReflectionService
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import se.uulm.snowballr.backend.auth.GrpcContext
import se.uulm.snowballr.backend.grpc.interceptor.authenticationInterceptor
import se.uulm.snowballr.backend.grpc.interceptor.exceptionInterceptor
import se.uulm.snowballr.backend.grpc.interceptor.loggingInterceptor
import se.uulm.snowballr.backend.grpc.interceptor.validationInterceptor
import se.uulm.snowballr.backend.service.IMainService
import snowballr.Authentication
import snowballr.Base
import snowballr.CriterionOuterClass
import snowballr.Export
import snowballr.Main
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
    private val reflectionService = ProtoReflectionService.newInstance()

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
     * - Registers a shutdown hook to handle the server shutdown process when the JVM is shutting down.
     *   The shutdown hook stops the server and confirms the server has been shut down.
     */
    fun start() {
        server.start()
        logger.info { "Server started, listening on $port" }
        Runtime.getRuntime().addShutdownHook(
            Thread {
                logger.info { "*** shutting down gRPC server since JVM is shutting down" }
                healthManager.enterTerminalState()
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
        private val mainService: IMainService by inject()

        override suspend fun getAvailableFetcherApis(request: Base.Nothing): Main.AvailableFetcherApis =
            super.getAvailableFetcherApis(request)

        override suspend fun register(request: Authentication.RegisterRequest): Base.Nothing {
            val (accessToken, refreshToken) = mainService.register(request)

            GrpcContext.setAuthCookiesInContext(accessToken, refreshToken)

            return Base.Nothing.getDefaultInstance()
        }

        override suspend fun login(request: Authentication.LoginRequest): Base.Nothing = super.login(request)

        override suspend fun logout(request: Base.Nothing): Base.Nothing = super.logout(request)

        override suspend fun getAuthenticationStatus(
            request: Base.Nothing,
        ): Authentication.AuthenticationStatusResponse = super.getAuthenticationStatus(request)

        override suspend fun renewSession(request: Base.Nothing): Base.Nothing = super.renewSession(request)

        override suspend fun requestPasswordReset(request: Authentication.RequestPasswordResetRequest): Base.Nothing =
            super.requestPasswordReset(request)

        override suspend fun resetPassword(request: Authentication.PasswordResetRequest): Base.Nothing =
            super.resetPassword(request)

        override suspend fun changePassword(request: Authentication.PasswordChangeRequest): Base.Nothing =
            super.changePassword(request)

        override suspend fun getAllUsers(request: Base.Nothing): UserOuterClass.User.List = mainService.getAllUsers()

        override suspend fun getCurrentUser(request: Base.Nothing): UserOuterClass.User = super.getCurrentUser(request)

        override suspend fun getUserById(request: Base.Id): UserOuterClass.User = mainService.getUserById(request)

        override suspend fun getUserByEmail(request: Base.Email): UserOuterClass.User =
            mainService.getUserByEmail(request)

        override suspend fun updateUser(request: UserOuterClass.User.Update): UserOuterClass.User =
            mainService.updateUser(request)

        override suspend fun softDeleteUser(request: Base.Id): Base.Nothing = super.softDeleteUser(request)

        override suspend fun softUndeleteUser(request: Base.Id): Base.Nothing = super.softUndeleteUser(request)

        override suspend fun getAllPapersToReview(request: Base.Nothing): ProjectOuterClass.Project.Paper.List =
            super.getAllPapersToReview(request)

        override suspend fun getPapersToReviewForProject(request: Base.Id): ProjectOuterClass.Project.Paper.List =
            super.getPapersToReviewForProject(request)

        override suspend fun getNextPaper(request: Base.Id): ProjectOuterClass.Project.Paper =
            super.getNextPaper(request)

        override suspend fun getNextPaperToReview(request: Base.Id): ProjectOuterClass.Project.Paper =
            super.getNextPaperToReview(request)

        override suspend fun getPreviousPaper(request: Base.Id): ProjectOuterClass.Project.Paper =
            super.getPreviousPaper(request)

        override suspend fun getUserSettings(request: Base.Nothing): UserSettingsOuterClass.UserSettings =
            super.getUserSettings(request)

        override suspend fun updateUserSettings(
            request: UserSettingsOuterClass.UserSettings.Update,
        ): UserSettingsOuterClass.UserSettings = super.updateUserSettings(request)

        override suspend fun getReadingList(request: Base.Nothing): PaperOuterClass.Paper.List =
            super.getReadingList(request)

        override suspend fun isPaperOnReadingList(request: Base.Id): Base.BoolValue =
            super.isPaperOnReadingList(request)

        override suspend fun addPaperToReadingList(request: Base.Id): Base.Nothing =
            super.addPaperToReadingList(request)

        override suspend fun removePaperFromReadingList(request: Base.Id): Base.Nothing =
            super.removePaperFromReadingList(request)

        override suspend fun getPendingInvitationsForUser(request: Base.Id): ProjectOuterClass.Project.List =
            super.getPendingInvitationsForUser(request)

        override suspend fun getInviteCandidates(request: UserOuterClass.User.SearchQuery): UserOuterClass.User.List =
            super.getInviteCandidates(request)

        override suspend fun inviteUserToProject(request: ProjectOuterClass.Project.Member.Invite): Base.Nothing =
            super.inviteUserToProject(request)

        override suspend fun getPendingInvitationsForProject(request: Base.Id): UserOuterClass.User.List =
            super.getPendingInvitationsForProject(request)

        override suspend fun getProjectMembers(request: Base.Id): ProjectOuterClass.Project.Member.List =
            super.getProjectMembers(request)

        override suspend fun removeProjectMember(request: ProjectOuterClass.Project.Member.Remove): Base.Nothing =
            super.removeProjectMember(request)

        override suspend fun getAllProjects(request: Base.Nothing): ProjectOuterClass.Project.List =
            mainService.getAllProjects()

        override suspend fun getAllDeletedProjects(request: Base.Nothing): ProjectOuterClass.Project.List =
            super.getAllDeletedProjects(request)

        override suspend fun getAllDeletedProjectsForUser(request: Base.Id): ProjectOuterClass.Project.List =
            super.getAllDeletedProjectsForUser(request)

        override suspend fun getAllArchivedProjects(request: Base.Nothing): ProjectOuterClass.Project.List =
            super.getAllArchivedProjects(request)

        override suspend fun getAllProjectsForUser(request: Base.Id): ProjectOuterClass.Project.List =
            super.getAllProjectsForUser(request)

        override suspend fun getAllArchivedProjectsForUser(request: Base.Id): ProjectOuterClass.Project.List =
            super.getAllArchivedProjectsForUser(request)

        override suspend fun createProject(request: ProjectOuterClass.Project.Create): ProjectOuterClass.Project =
            mainService.createProject(request)

        override suspend fun getProjectById(request: Base.Id): ProjectOuterClass.Project = super.getProjectById(request)

        override suspend fun updateProject(request: ProjectOuterClass.Project.Update): ProjectOuterClass.Project =
            super.updateProject(request)

        override suspend fun exportProject(request: Export.ExportRequest): Base.Blob = super.exportProject(request)

        override suspend fun softDeleteProject(request: Base.Id): Base.Nothing = super.softDeleteProject(request)

        override suspend fun softUndeleteProject(request: Base.Id): Base.Nothing = super.softUndeleteProject(request)

        override suspend fun getProjectInformation(
            request: ProjectOuterClass.Project.Information.Get,
        ): ProjectOuterClass.Project.Information = super.getProjectInformation(request)

        override suspend fun getDecisionStatisticsForStage(
            request: ProjectOuterClass.Project.Information.DecisionStatistics.Get,
        ): ProjectOuterClass.Project.Information.DecisionStatistics = super.getDecisionStatisticsForStage(request)

        override suspend fun updateProjectMemberRole(request: ProjectOuterClass.Project.Member.Update): Base.Nothing =
            super.updateProjectMemberRole(request)

        override suspend fun getCriterionById(request: Base.Id): CriterionOuterClass.Criterion =
            super.getCriterionById(request)

        override suspend fun getAllCriteriaForProject(request: Base.Id): CriterionOuterClass.Criterion.List =
            super.getAllCriteriaForProject(request)

        override suspend fun createCriterion(
            request: CriterionOuterClass.Criterion.Create,
        ): CriterionOuterClass.Criterion = mainService.createCriterion(request)

        override suspend fun updateCriterion(
            request: CriterionOuterClass.Criterion.Update,
        ): CriterionOuterClass.Criterion = super.updateCriterion(request)

        override suspend fun deleteCriterion(request: Base.Id): Base.Nothing = super.deleteCriterion(request)

        override suspend fun getProjectPaperById(request: Base.Id): ProjectOuterClass.Project.Paper =
            super.getProjectPaperById(request)

        override suspend fun getProjectPaperByRelativeId(
            request: ProjectOuterClass.Project.Paper.Get,
        ): ProjectOuterClass.Project.Paper = super.getProjectPaperByRelativeId(request)

        override suspend fun getAllProjectPapersForProject(request: Base.Id): ProjectOuterClass.Project.Paper.List =
            super.getAllProjectPapersForProject(request)

        override suspend fun addPaperToProject(
            request: ProjectOuterClass.Project.Paper.Add,
        ): ProjectOuterClass.Project.Paper = super.addPaperToProject(request)

        override suspend fun updateProjectPaper(
            request: ProjectOuterClass.Project.Paper.Update,
        ): ProjectOuterClass.Project.Paper = super.updateProjectPaper(request)

        override suspend fun removePaperFromProject(request: Base.Id): Base.Nothing =
            super.removePaperFromProject(request)

        override suspend fun getReviewById(request: Base.Id): ReviewOuterClass.Review = super.getReviewById(request)

        override suspend fun getAllReviewsForProjectPaper(request: Base.Id): ReviewOuterClass.Review.List =
            super.getAllReviewsForProjectPaper(request)

        override suspend fun createReview(request: ReviewOuterClass.Review.Create): ReviewOuterClass.Review =
            super.createReview(request)

        override suspend fun updateReview(request: ReviewOuterClass.Review.Update): ReviewOuterClass.Review =
            super.updateReview(request)

        override suspend fun deleteReview(request: Base.Id): Base.Nothing = super.deleteReview(request)

        override suspend fun getPaperById(request: Base.Id): PaperOuterClass.Paper = super.getPaperById(request)

        override suspend fun createPaper(request: PaperOuterClass.Paper): PaperOuterClass.Paper =
            super.createPaper(request)

        override suspend fun updatePaper(request: PaperOuterClass.Paper.Update): PaperOuterClass.Paper =
            super.updatePaper(request)

        override suspend fun getForwardReferencedPapers(request: Base.Id): PaperOuterClass.Paper.List =
            super.getForwardReferencedPapers(request)

        override suspend fun getBackwardReferencedPapers(request: Base.Id): PaperOuterClass.Paper.List =
            super.getBackwardReferencedPapers(request)

        override suspend fun getPaperPdf(request: Base.Id): Base.Blob = super.getPaperPdf(request)

        override suspend fun setPaperPdf(request: PaperOuterClass.Paper.PdfUpdate): Base.Nothing =
            super.setPaperPdf(request)
    }
}
