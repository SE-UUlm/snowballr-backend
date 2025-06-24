package se.uulm.snowballr.backend

import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import se.uulm.snowballr.backend.db.Database
import se.uulm.snowballr.backend.db.IDatabase
import se.uulm.snowballr.backend.env.Env
import se.uulm.snowballr.backend.repository.CriterionTableRepo
import se.uulm.snowballr.backend.repository.ICriterionTableRepo
import se.uulm.snowballr.backend.repository.IProjectTableRepo
import se.uulm.snowballr.backend.repository.IUserTableRepo
import se.uulm.snowballr.backend.repository.ProjectTableRepo
import se.uulm.snowballr.backend.repository.UserTableRepo
import se.uulm.snowballr.backend.service.IMainService
import se.uulm.snowballr.backend.service.MainService

/**
 * Defines the Koin dependency injection module for the application.
 *
 * This module includes the following components in a defined order of initialization:
 * - The database implementation ([IDatabase]), which is initialized with no external dependencies.
 * - The repository layer (e.g. [IProjectTableRepo]), which uses the [IDatabase] implementation for database operations.
 * - The main service ([IMainService]), which depends on the repository layer to provide higher-level functionality.
 *
 * The ordering ensures proper dependency resolution and initialization.
 */
val snowballRModule =
    module {
        // First comes the database as it has no dependencies
        single<IDatabase>(createdAtStart = true) { Database(Env().database) }
        // Here come all repos and other definitions, e.g., the http client
        singleOf(::ProjectTableRepo) { bind<IProjectTableRepo>() }
        singleOf(::CriterionTableRepo) { bind<ICriterionTableRepo>() }
        singleOf(::UserTableRepo) { bind<IUserTableRepo>() }
        // The main service comes last
        singleOf(::MainService) { bind<IMainService>() }
    }
