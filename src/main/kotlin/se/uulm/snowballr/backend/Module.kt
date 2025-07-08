package se.uulm.snowballr.backend

import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.createdAtStart
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import se.uulm.snowballr.backend.auth.CookieUtils
import se.uulm.snowballr.backend.auth.ICookieUtils
import se.uulm.snowballr.backend.auth.IJwtUtils
import se.uulm.snowballr.backend.auth.JwtUtils
import se.uulm.snowballr.backend.db.Database
import se.uulm.snowballr.backend.db.IDatabase
import se.uulm.snowballr.backend.env.EnvReader
import se.uulm.snowballr.backend.env.EnvService
import se.uulm.snowballr.backend.env.IEnvService
import se.uulm.snowballr.backend.repository.CriterionTableRepo
import se.uulm.snowballr.backend.repository.ICriterionTableRepo
import se.uulm.snowballr.backend.repository.IProjectTableRepo
import se.uulm.snowballr.backend.repository.IUserTableRepo
import se.uulm.snowballr.backend.repository.ProjectTableRepo
import se.uulm.snowballr.backend.repository.UserTableRepo
import se.uulm.snowballr.backend.repository.association.IProjectMemberTableRepo
import se.uulm.snowballr.backend.repository.association.ProjectMemberTableRepo
import se.uulm.snowballr.backend.service.AuthenticationService
import se.uulm.snowballr.backend.service.IAuthenticationService
import se.uulm.snowballr.backend.service.IMainService
import se.uulm.snowballr.backend.service.MainService

/**
 * Defines the Koin dependency injection module for the application.
 *
 * This module includes the following components in a defined order of initialization:
 * - The environment service ([IEnvService]) and its reader ([EnvReader]), which are initialized first to provide
 *  access to environment variables.
 * - The JWT utility ([IJwtUtils]), which depends on the environment reader to access necessary environment variables.
 * - The cookie utility ([ICookieUtils]), which relies on the JWT utility for token handling.
 * - The database implementation ([IDatabase]), which is initialized with no external dependencies.
 * - The repository layer (e.g. [IProjectTableRepo]), which uses the [IDatabase] implementation for database operations.
 * - The [IAuthenticationService] is also included to handle authentication logic, which may be used by the main service.
 * - The main service ([IMainService]), which depends on the repository layer to provide higher-level functionality.
 *
 * The ordering ensures proper dependency resolution and initialization.
 */
val snowballRModule =
    module {
        // First come the env service and reader
        single<IEnvService> { EnvService() }
        singleOf(::EnvReader)
        // Then the JWT utils, which depend on the env reader
        singleOf(::JwtUtils) { bind<IJwtUtils>() }
        // Then the cookie utils, which depend on the JWT utils
        singleOf(::CookieUtils) { bind<ICookieUtils>() }
        // Then the database, which only needs access to some env variables
        singleOf(::Database) {
            createdAtStart()
            bind<IDatabase>()
        }
        // Here come all repos and other definitions, e.g., the http client
        singleOf(::ProjectTableRepo) { bind<IProjectTableRepo>() }
        singleOf(::CriterionTableRepo) { bind<ICriterionTableRepo>() }
        singleOf(::UserTableRepo) { bind<IUserTableRepo>() }
        singleOf(::ProjectMemberTableRepo) { bind<IProjectMemberTableRepo>() }
        singleOf(::AuthenticationService) { bind<IAuthenticationService>() }
        // The main service comes last
        singleOf(::MainService) { bind<IMainService>() }
    }
