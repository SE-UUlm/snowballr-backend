package se.uulm.snowballr.backend.service.paper

import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import se.uulm.snowballr.backend.model.dto.paper.Author
import se.uulm.snowballr.backend.model.dto.paper.Paper
import se.uulm.snowballr.backend.repository.IPaperTableRepo
import se.uulm.snowballr.backend.repository.association.ICitationTableRepo
import se.uulm.snowballr.backend.service.BaseServiceTest
import se.uulm.snowballr.backend.service.PaperService
import snowballr.PaperOuterClass

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

    protected fun assertPaperEquality(expected: Paper, actual: PaperOuterClass.Paper) {
        assertEquals(expected.title, actual.title)
        assertEquals(expected.externalId, actual.externalId)
        assertEquals(expected.abstract, actual.abstrakt)
        assertEquals(expected.year, actual.year)
        assertEquals(expected.publisher, actual.publisher)
        assertEquals(expected.publicationType, actual.publicationType)
        assertEquals(expected.publicationName, actual.publicationName)
        for (i in expected.authors.indices) {
            assertAuthorEquality(expected.authors[i], actual.authorsList[i])
        }
        assertEquals(expected.fetcherMetadata, actual.fetcherMetadataMap)
    }

    private fun assertAuthorEquality(expected: Author, actual: PaperOuterClass.Author) {
        assertEquals(expected.firstName, actual.firstName)
        assertEquals(expected.lastName, actual.lastName)
    }
}
