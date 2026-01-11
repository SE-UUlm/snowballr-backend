package se.uulm.snowballr.backend

import com.github.jknack.handlebars.Template
import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.simplejavamail.api.mailer.Mailer
import org.simplejavamail.mailer.MailerBuilder
import se.uulm.snowballr.backend.access.CriterionAccessChecker
import se.uulm.snowballr.backend.access.ICriterionAccessChecker
import se.uulm.snowballr.backend.access.IInvitationAccessChecker
import se.uulm.snowballr.backend.access.IProjectAccessChecker
import se.uulm.snowballr.backend.access.IProjectMemberAccessChecker
import se.uulm.snowballr.backend.access.IProjectPaperAccessChecker
import se.uulm.snowballr.backend.access.IReviewAccessChecker
import se.uulm.snowballr.backend.access.IUserAccessChecker
import se.uulm.snowballr.backend.access.InvitationAccessChecker
import se.uulm.snowballr.backend.access.ProjectAccessChecker
import se.uulm.snowballr.backend.access.ProjectMemberAccessChecker
import se.uulm.snowballr.backend.access.ProjectPaperAccessChecker
import se.uulm.snowballr.backend.access.ReviewAccessChecker
import se.uulm.snowballr.backend.access.UserAccessChecker
import se.uulm.snowballr.backend.auth.AuthenticationManager
import se.uulm.snowballr.backend.auth.CookieManager
import se.uulm.snowballr.backend.auth.IAuthenticationManager
import se.uulm.snowballr.backend.auth.ICookieManager
import se.uulm.snowballr.backend.auth.IJwtManager
import se.uulm.snowballr.backend.auth.JwtManager
import se.uulm.snowballr.backend.db.Database
import se.uulm.snowballr.backend.db.IDatabase
import se.uulm.snowballr.backend.env.EnvReader
import se.uulm.snowballr.backend.env.EnvService
import se.uulm.snowballr.backend.env.IEnvService
import se.uulm.snowballr.backend.fetcher.IFetcherManager
import se.uulm.snowballr.backend.fetcher.PythonPluginFetcherManager
import se.uulm.snowballr.backend.mail.EmailManager
import se.uulm.snowballr.backend.mail.EmailTemplateManager
import se.uulm.snowballr.backend.mail.IEmailManager
import se.uulm.snowballr.backend.repository.CriterionTableRepo
import se.uulm.snowballr.backend.repository.ICriterionTableRepo
import se.uulm.snowballr.backend.repository.IInvitationTokenTableRepo
import se.uulm.snowballr.backend.repository.IPaperTableRepo
import se.uulm.snowballr.backend.repository.IProjectTableRepo
import se.uulm.snowballr.backend.repository.IReviewTableRepo
import se.uulm.snowballr.backend.repository.IUserTableRepo
import se.uulm.snowballr.backend.repository.IVerificationTokenTableRepo
import se.uulm.snowballr.backend.repository.InvitationTokenTableRepo
import se.uulm.snowballr.backend.repository.PaperTableRepo
import se.uulm.snowballr.backend.repository.ProjectTableRepo
import se.uulm.snowballr.backend.repository.ReviewTableRepo
import se.uulm.snowballr.backend.repository.UserTableRepo
import se.uulm.snowballr.backend.repository.VerificationTokenTableRepo
import se.uulm.snowballr.backend.repository.association.CitationTableRepo
import se.uulm.snowballr.backend.repository.association.ICitationTableRepo
import se.uulm.snowballr.backend.repository.association.IProjectMemberTableRepo
import se.uulm.snowballr.backend.repository.association.IProjectPaperTableRepo
import se.uulm.snowballr.backend.repository.association.IReadingListTableRepo
import se.uulm.snowballr.backend.repository.association.IReviewHasCriterionTableRepo
import se.uulm.snowballr.backend.repository.association.ProjectMemberTableRepo
import se.uulm.snowballr.backend.repository.association.ProjectPaperTableRepo
import se.uulm.snowballr.backend.repository.association.ReadingListTableRepo
import se.uulm.snowballr.backend.repository.association.ReviewHasCriterionTableRepo
import se.uulm.snowballr.backend.scheduler.IProjectMaintenanceService
import se.uulm.snowballr.backend.scheduler.ITokenMaintenanceService
import se.uulm.snowballr.backend.scheduler.IUserMaintenanceService
import se.uulm.snowballr.backend.scheduler.ProjectMaintenanceService
import se.uulm.snowballr.backend.scheduler.TokenMaintenanceService
import se.uulm.snowballr.backend.scheduler.UserMaintenanceService
import se.uulm.snowballr.backend.service.AuthenticationService
import se.uulm.snowballr.backend.service.CriterionService
import se.uulm.snowballr.backend.service.ExportService
import se.uulm.snowballr.backend.service.FetcherService
import se.uulm.snowballr.backend.service.IAuthenticationService
import se.uulm.snowballr.backend.service.ICriterionService
import se.uulm.snowballr.backend.service.IExportService
import se.uulm.snowballr.backend.service.IFetcherService
import se.uulm.snowballr.backend.service.IInvitationService
import se.uulm.snowballr.backend.service.IMainService
import se.uulm.snowballr.backend.service.IPaperService
import se.uulm.snowballr.backend.service.IProjectMemberService
import se.uulm.snowballr.backend.service.IProjectPaperService
import se.uulm.snowballr.backend.service.IProjectService
import se.uulm.snowballr.backend.service.IReadingListService
import se.uulm.snowballr.backend.service.IReviewService
import se.uulm.snowballr.backend.service.IUserService
import se.uulm.snowballr.backend.service.InvitationService
import se.uulm.snowballr.backend.service.MainService
import se.uulm.snowballr.backend.service.PaperService
import se.uulm.snowballr.backend.service.ProjectMemberService
import se.uulm.snowballr.backend.service.ProjectPaperService
import se.uulm.snowballr.backend.service.ProjectService
import se.uulm.snowballr.backend.service.ReadingListService
import se.uulm.snowballr.backend.service.ReviewService
import se.uulm.snowballr.backend.service.UserService

/**
 * Defines the Koin dependency injection module for the application.
 *
 * This module includes the following components in a defined order of initialization:
 * - The environment service ([IEnvService]) and its reader ([EnvReader]), which are initialized first to provide
 *  access to environment variables.
 * - The database ([IDatabase]), which only requires some env variables provided by the [EnvReader].
 * - The repository layer (e.g. [IProjectTableRepo]), which uses the [IDatabase] implementation for database operations.
 * - Custom services / manager / clients that are used by the core service layer.
 * - The core service layer, which consists of entity-related services (e.g. [IProjectService]) and the [IMainService],
 * which combines all services into one access point.
 *
 * The ordering ensures proper dependency resolution and initialization.
 */
val snowballRModule =
    module(createdAtStart = true) {
        envDeps()
        dbDeps()
        repositoryLayerDeps()
        mailServiceDeps()
        customServicesDeps()
        serviceLayerDeps()
    }

/**
 * Module declaration of the [IEnvService] and [EnvReader].
 */
private fun Module.envDeps() {
    single<IEnvService> { EnvService() }
    singleOf(::EnvReader)
}

/**
 * Module declaration of the [IDatabase].
 */
private fun Module.dbDeps() {
    singleOf(::Database) {
        bind<IDatabase>()
    }
    single<ITokenMaintenanceService> { TokenMaintenanceService() }
    single<IUserMaintenanceService> { UserMaintenanceService() }
    single<IProjectMaintenanceService> { ProjectMaintenanceService() }
}

/**
 * Module declaration of the repository layer.
 *
 * Consists of all repositories.
 */
fun Module.repositoryLayerDeps() {
    singleOf(::ProjectTableRepo) { bind<IProjectTableRepo>() }
    singleOf(::CriterionTableRepo) { bind<ICriterionTableRepo>() }
    singleOf(::UserTableRepo) { bind<IUserTableRepo>() }
    singleOf(::ProjectMemberTableRepo) { bind<IProjectMemberTableRepo>() }
    singleOf(::PaperTableRepo) { bind<IPaperTableRepo>() }
    singleOf(::CitationTableRepo) { bind<ICitationTableRepo>() }
    singleOf(::ReadingListTableRepo) { bind<IReadingListTableRepo>() }
    singleOf(::VerificationTokenTableRepo) { bind<IVerificationTokenTableRepo>() }
    singleOf(::InvitationTokenTableRepo) { bind<IInvitationTokenTableRepo>() }
    singleOf(::ProjectPaperTableRepo) { bind<IProjectPaperTableRepo>() }
    singleOf(::ReviewTableRepo) { bind<IReviewTableRepo>() }
    singleOf(::ReviewHasCriterionTableRepo) { bind<IReviewHasCriterionTableRepo>() }
}

/**
 * Module declaration of the mail service dependencies.
 *
 * Consists of the [Mailer] and the [Template]s used for sending mails.
 */
fun Module.mailServiceDeps() {
    single<Mailer> { createMailer(get()) }
    single<EmailTemplateManager> { EmailTemplateManager() }
}

/**
 * Creates the Mailer instance based on the environment configuration.
 */
private fun createMailer(envReader: EnvReader): Mailer {
    val env = envReader.env

    // Enable debug logging if the log level is DEBUG or TRACE
    val isDebugLogging = env.miscellaneous.logLevel == "DEBUG" || env.miscellaneous.logLevel == "TRACE"

    return MailerBuilder
        .withSMTPServer(env.smtp.smtpHost, env.smtp.smtpPort)
        .withTransportModeLoggingOnly(env.smtp.smtpTransportLoggingOnlyEnabled)
        .withDebugLogging(isDebugLogging)
        .apply {
            env.smtp.smtpUser?.let { withSMTPServerUsername(it) }
            env.smtp.smtpPassword?.let { withSMTPServerPassword(it) }
        }
        .async()
        .buildMailer()
}

/**
 * Module declaration of all custom services / managers / clients.
 *
 * Consists of all dependencies that are used by the core service layer.
 */
private fun Module.customServicesDeps() {
    singleOf(::JwtManager) { bind<IJwtManager>() }
    singleOf(::PythonPluginFetcherManager) { bind<IFetcherManager>() }
    singleOf(::CookieManager) { bind<ICookieManager>() }
    singleOf(::EmailManager) { bind<IEmailManager>() }
    singleOf(::AuthenticationManager) { bind<IAuthenticationManager>() }
}

/**
 * Module declaration of the core service layer.
 *
 * Consists of the [MainService] and all its direct service dependencies.
 */
fun Module.serviceLayerDeps() {
    // Access Checkers
    singleOf(::ProjectAccessChecker) { bind<IProjectAccessChecker>() }
    singleOf(::UserAccessChecker) { bind<IUserAccessChecker>() }
    singleOf(::CriterionAccessChecker) { bind<ICriterionAccessChecker>() }
    singleOf(::ReviewAccessChecker) { bind<IReviewAccessChecker>() }
    singleOf(::ProjectMemberAccessChecker) { bind<IProjectMemberAccessChecker>() }
    singleOf(::ProjectPaperAccessChecker) { bind<IProjectPaperAccessChecker>() }
    singleOf(::InvitationAccessChecker) { bind<IInvitationAccessChecker>() }

    // All services that are directly used by the MainService
    singleOf(::ProjectService) { bind<IProjectService>() }
    singleOf(::CriterionService) { bind<ICriterionService>() }
    singleOf(::UserService) { bind<IUserService>() }
    singleOf(::FetcherService) { bind<IFetcherService>() }
    singleOf(::ReadingListService) { bind<IReadingListService>() }
    singleOf(::ReviewService) { bind<IReviewService>() }
    singleOf(::PaperService) { bind<IPaperService>() }
    singleOf(::ProjectPaperService) { bind<IProjectPaperService>() }
    singleOf(::AuthenticationService) { bind<IAuthenticationService>() }
    singleOf(::InvitationService) { bind<IInvitationService>() }
    singleOf(::ProjectMemberService) { bind<IProjectMemberService>() }
    singleOf(::ExportService) { bind<IExportService>() }
    // The main service comes last
    singleOf(::MainService) { bind<IMainService>() }
}
