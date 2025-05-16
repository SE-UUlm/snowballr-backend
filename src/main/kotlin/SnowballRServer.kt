package se.uulm.snowballr.backend

import io.grpc.Server
import io.grpc.ServerBuilder
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

class SnowballRServer(
    private val port: Int,
) {
    val server: Server =
        ServerBuilder
            .forPort(port)
            .addService(SnowballRService())
            .build()

    fun start() {
        server.start()
        println("Server started, listening on $port")
        Runtime.getRuntime().addShutdownHook(
            Thread {
                println("*** shutting down gRPC server since JVM is shutting down")
                this@SnowballRServer.stop()
                println("*** server shut down")
            },
        )
    }

    fun stop() {
        server.shutdown()
    }

    fun blockUntilShutdown() {
        server.awaitTermination()
    }

    @Suppress("TooManyFunctions")
    internal class SnowballRService : SnowballRGrpcKt.SnowballRCoroutineImplBase() {
        override suspend fun getAvailableFetcherApis(request: Base.Nothing): Main.AvailableFetcherApis =
            super.getAvailableFetcherApis(request)

        override suspend fun register(request: Authentication.RegisterRequest): Base.Nothing = super.register(request)

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

        override suspend fun getAllUsers(request: Base.Nothing): UserOuterClass.User.List = super.getAllUsers(request)

        override suspend fun getCurrentUser(request: Base.Nothing): UserOuterClass.User = super.getCurrentUser(request)

        override suspend fun getUserById(request: Base.Id): UserOuterClass.User = super.getUserById(request)

        override suspend fun getUserByEmail(request: Base.Id): UserOuterClass.User = super.getUserByEmail(request)

        override suspend fun updateUser(request: UserOuterClass.User.Update): UserOuterClass.User =
            super.updateUser(request)

        override suspend fun softDeleteUser(request: Base.Id): Base.Nothing = super.softDeleteUser(request)

        override suspend fun softUndeleteUser(request: Base.Id): Base.Nothing = super.softUndeleteUser(request)

        override suspend fun getAllPapersToReview(request: Base.Nothing): ProjectOuterClass.Project.Paper.List =
            super.getAllPapersToReview(request)

        override suspend fun getPapersToReviewForProject(request: Base.Id): ProjectOuterClass.Project.Paper.List =
            super.getPapersToReviewForProject(request)

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

        override suspend fun inviteUserToProject(request: ProjectOuterClass.Project.Member.Invite): Base.Nothing =
            super.inviteUserToProject(request)

        override suspend fun getPendingInvitationsForProject(request: Base.Id): UserOuterClass.User.List =
            super.getPendingInvitationsForProject(request)

        override suspend fun getProjectMembers(request: Base.Id): ProjectOuterClass.Project.Member.List =
            super.getProjectMembers(request)

        override suspend fun removeProjectMember(request: ProjectOuterClass.Project.Member.Remove): Base.Nothing =
            super.removeProjectMember(request)

        override suspend fun getAllProjects(request: Base.Nothing): ProjectOuterClass.Project.List =
            super.getAllProjects(request)

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
            super.createProject(request)

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
        ): CriterionOuterClass.Criterion = super.createCriterion(request)

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
