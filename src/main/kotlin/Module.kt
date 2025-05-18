package se.uulm.snowballr.backend

import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import se.uulm.snowballr.backend.db.Database
import se.uulm.snowballr.backend.db.IDatabase
import se.uulm.snowballr.backend.env.Env
import se.uulm.snowballr.backend.service.IMainService
import se.uulm.snowballr.backend.service.MainService

val snowballRModule =
    module {
        // First comes the database as it has no dependencies
        single<IDatabase>(createdAtStart = true) { Database(Env().database) }
        // Here come all repos and other definitions, e.g., the http client

        // The main service comes last
        singleOf(::MainService) { bind<IMainService>() }
    }
