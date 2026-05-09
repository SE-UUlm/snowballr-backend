package se.uulm.snowballr.backend.service.paper

import io.mockk.mockk
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.test.inject
import se.uulm.snowballr.backend.repository.IPaperTableRepo
import se.uulm.snowballr.backend.repository.association.ICitationTableRepo
import se.uulm.snowballr.backend.service.BaseServiceTest
import se.uulm.snowballr.backend.service.IPaperService
import se.uulm.snowballr.backend.service.PaperService

/**
 * Base test class for the [PaperService].
 */
sealed class PaperServiceTest : BaseServiceTest() {
    val paperRepoMock = mockk<IPaperTableRepo>()
    val citationRepoMock = mockk<ICitationTableRepo>()

    private val allMocks = arrayOf(paperRepoMock, citationRepoMock)

    val service: IPaperService by inject()

    private val module = module {
        single { paperRepoMock }
        single { citationRepoMock }
    }

    override fun getModule(): Module = module

    override fun getAllMocks(): Array<Any> = allMocks
}
