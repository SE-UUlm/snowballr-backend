package se.uulm.snowballr.backend.service.paper

import io.mockk.mockk
import se.uulm.snowballr.backend.repository.IPaperTableRepo
import se.uulm.snowballr.backend.repository.association.ICitationTableRepo
import se.uulm.snowballr.backend.service.BaseServiceTest
import se.uulm.snowballr.backend.service.PaperService

/**
 * Base test class for the [PaperService].
 */
sealed class PaperServiceTest : BaseServiceTest {
    val paperRepoMock = mockk<IPaperTableRepo>()
    val citationRepoMock = mockk<ICitationTableRepo>()

    private val allMocks = arrayOf(paperRepoMock, citationRepoMock)

    val service = PaperService(
        repo = paperRepoMock,
        citationRepo = citationRepoMock,
    )

    override fun getAllMocks(): Array<Any> = allMocks
}
