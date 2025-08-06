package se.uulm.snowballr.backend

import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.createdAtStart
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import se.uulm.snowballr.backend.auth.CookieService
import se.uulm.snowballr.backend.auth.ICookieService
import se.uulm.snowballr.backend.auth.IJwtService
import se.uulm.snowballr.backend.auth.JwtService
import se.uulm.snowballr.backend.db.Database
import se.uulm.snowballr.backend.db.IDatabase
import se.uulm.snowballr.backend.env.EnvReader
import se.uulm.snowballr.backend.env.EnvService
import se.uulm.snowballr.backend.env.IEnvService
import se.uulm.snowballr.backend.fetcher.FetcherManager
import se.uulm.snowballr.backend.repository.AuthorTableRepo
import se.uulm.snowballr.backend.repository.CriterionTableRepo
import se.uulm.snowballr.backend.repository.IAuthorTableRepo
import se.uulm.snowballr.backend.repository.ICriterionTableRepo
import se.uulm.snowballr.backend.repository.IPaperTableRepo
import se.uulm.snowballr.backend.repository.IProjectTableRepo
import se.uulm.snowballr.backend.repository.IUserTableRepo
import se.uulm.snowballr.backend.repository.IVerificationTokenTableRepo
import se.uulm.snowballr.backend.repository.PaperTableRepo
import se.uulm.snowballr.backend.repository.ProjectTableRepo
import se.uulm.snowballr.backend.repository.UserTableRepo
import se.uulm.snowballr.backend.repository.VerificationTokenTableRepo
import se.uulm.snowballr.backend.repository.association.AuthorOfPaperTableRepo
import se.uulm.snowballr.backend.repository.association.CitationTableRepo
import se.uulm.snowballr.backend.repository.association.IAuthorOfPaperTableRepo
import se.uulm.snowballr.backend.repository.association.ICitationTableRepo
import se.uulm.snowballr.backend.repository.association.IProjectMemberTableRepo
import se.uulm.snowballr.backend.repository.association.IProjectPaperTableRepo
import se.uulm.snowballr.backend.repository.association.IReadingListTableRepo
import se.uulm.snowballr.backend.repository.association.ProjectMemberTableRepo
import se.uulm.snowballr.backend.repository.association.ProjectPaperTableRepo
import se.uulm.snowballr.backend.repository.association.ReadingListTableRepo
import se.uulm.snowballr.backend.service.AuthenticationService
import se.uulm.snowballr.backend.service.CriterionService
import se.uulm.snowballr.backend.service.EmailService
import se.uulm.snowballr.backend.service.FetcherService
import se.uulm.snowballr.backend.service.IAuthenticationService
import se.uulm.snowballr.backend.service.ICriterionService
import se.uulm.snowballr.backend.service.IEmailService
import se.uulm.snowballr.backend.service.IFetcherService
import se.uulm.snowballr.backend.service.IMainService
import se.uulm.snowballr.backend.service.IProjectService
import se.uulm.snowballr.backend.service.IReadingListService
import se.uulm.snowballr.backend.service.IUserService
import se.uulm.snowballr.backend.service.MainService
import se.uulm.snowballr.backend.service.ProjectService
import se.uulm.snowballr.backend.service.ReadingListService
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
    module {
        envDeps()
        dbDeps()
        repositoryLayerDeps()
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
        createdAtStart()
        bind<IDatabase>()
    }
}

/**
 * Module declaration of the repository layer.
 *
 * Consists of all repositories.
 */
private fun Module.repositoryLayerDeps() {
    singleOf(::ProjectTableRepo) { bind<IProjectTableRepo>() }
    singleOf(::CriterionTableRepo) { bind<ICriterionTableRepo>() }
    singleOf(::UserTableRepo) { bind<IUserTableRepo>() }
    singleOf(::ProjectMemberTableRepo) { bind<IProjectMemberTableRepo>() }
    singleOf(::PaperTableRepo) { bind<IPaperTableRepo>() }
    singleOf(::AuthorTableRepo) { bind<IAuthorTableRepo>() }
    singleOf(::AuthorOfPaperTableRepo) { bind<IAuthorOfPaperTableRepo>() }
    singleOf(::CitationTableRepo) { bind<ICitationTableRepo>() }
    singleOf(::ReadingListTableRepo) { bind<IReadingListTableRepo>() }
    singleOf(::VerificationTokenTableRepo) { bind<IVerificationTokenTableRepo>() }
    singleOf(::ProjectPaperTableRepo) { bind<IProjectPaperTableRepo>() }
}

/**
 * Module declaration of all custom services / managers / clients.
 *
 * Consists of all dependencies that are used by the core service layer.
 */
private fun Module.customServicesDeps() {
    singleOf(::JwtService) {
        createdAtStart()
        bind<IJwtService>()
    }
    singleOf(::FetcherManager)
    singleOf(::CookieService) { bind<ICookieService>() }
    singleOf(::EmailService) {
        createdAtStart()
        bind<IEmailService>()
    }
    singleOf(::AuthenticationService) { bind<IAuthenticationService>() }
}

/**
 * Module declaration of the core service layer.
 *
 * Consists of the [MainService] and all its direct service dependencies.
 */
fun Module.serviceLayerDeps() {
    // All services that are directly used by the MainService
    singleOf(::ProjectService) { bind<IProjectService>() }
    singleOf(::CriterionService) { bind<ICriterionService>() }
    singleOf(::UserService) { bind<IUserService>() }
    singleOf(::FetcherService) { bind<IFetcherService>() }
    singleOf(::ReadingListService) { bind<IReadingListService>() }
    // The main service comes last
    singleOf(::MainService) { bind<IMainService>() }
}
