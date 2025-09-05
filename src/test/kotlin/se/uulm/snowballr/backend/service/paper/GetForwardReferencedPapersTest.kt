package se.uulm.snowballr.backend.service.paper

import io.mockk.coEvery
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.SnowballRException
import se.uulm.snowballr.backend.service.MainServiceTest
import snowballr.Base
import java.util.UUID
import java.util.stream.Stream
import kotlin.reflect.KFunction

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetForwardReferencedPapersTest : MainServiceTest() {
    private val requestId = UUID.randomUUID()

    private fun getExampleRequest() = Base.Id
        .newBuilder()
        .setId(requestId.toString())
        .build()

    fun failingFunctions(): Stream<Arguments?> = Stream.of(
        Arguments.of(paperRepoMock::doesPaperExistById),
        Arguments.of(citationRepoMock::getForwardReferencedPaperIdsOfPaperById),
        Arguments.of(authorOfPaperRepoMock::getAuthorsOfPaperById),
        Arguments.of(citationRepoMock::getBackwardsReferencedPaperIdsOfPaperById),
    )

    @Suppress("ReturnCount")
    private fun mockHappyPathUntil(failAt: KFunction<*>?) {
        val paper = DataBuilder.createExamplePaper(id = requestId)
        val forwardReferenceId = UUID.randomUUID()
        val citingPaper = DataBuilder.createExamplePaper(id = forwardReferenceId)
        val author = DataBuilder.createExampleAuthor()

        if (failAt == paperRepoMock::doesPaperExistById) {
            coEvery { paperRepoMock.doesPaperExistById(paper.id) } throws TestSpecificException()
            return
        }
        coEvery { paperRepoMock.doesPaperExistById(paper.id) } returns true

        if (failAt == citationRepoMock::getForwardReferencedPaperIdsOfPaperById) {
            coEvery {
                citationRepoMock.getForwardReferencedPaperIdsOfPaperById(paper.id)
            } throws TestSpecificException()
            return
        }
        coEvery {
            citationRepoMock.getForwardReferencedPaperIdsOfPaperById(paper.id)
        } returns listOf(forwardReferenceId)

        if (failAt == paperRepoMock::getPaperById) {
            coEvery { paperRepoMock.getPaperById(forwardReferenceId) } throws TestSpecificException()
            return
        }
        coEvery { paperRepoMock.getPaperById(forwardReferenceId) } returns citingPaper

        if (failAt == authorOfPaperRepoMock::getAuthorsOfPaperById) {
            coEvery { authorOfPaperRepoMock.getAuthorsOfPaperById(forwardReferenceId) } throws TestSpecificException()
            return
        }
        coEvery { authorOfPaperRepoMock.getAuthorsOfPaperById(forwardReferenceId) } returns listOf(author)

        if (failAt == citationRepoMock::getBackwardsReferencedPaperIdsOfPaperById) {
            coEvery {
                citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(forwardReferenceId)
            } throws TestSpecificException()
            return
        }
        coEvery {
            citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(forwardReferenceId)
        } returns listOf(UUID.randomUUID())
    }

    @ParameterizedTest
    @MethodSource("failingFunctions")
    fun `When a step fails, then an exception is thrown`(failAt: KFunction<*>) = runTest {
        mockHappyPathUntil(failAt)
        assertThrows<TestSpecificException> {
            mainService.getForwardReferencedPapers(getExampleRequest())
        }
    }

    @Test
    fun `When the paper doesn't exist, then a NotFoundException is thrown`() = runTest {
        coEvery { paperRepoMock.doesPaperExistById(requestId) } returns false
        assertThrows<SnowballRException.NotFoundException> {
            mainService.getForwardReferencedPapers(
                getExampleRequest(),
            )
        }
    }

    @Test
    fun `When one of the forward references doesn't exist, then a NotFoundException is thrown`() = runTest {
        val paper = DataBuilder.createExamplePaper(id = requestId)
        val forwardReferenceId = UUID.randomUUID()

        coEvery { paperRepoMock.doesPaperExistById(requestId) } returns true
        coEvery {
            citationRepoMock.getForwardReferencedPaperIdsOfPaperById(paper.id)
        } returns listOf(forwardReferenceId)
        coEvery { paperRepoMock.getPaperById(forwardReferenceId) } throws SnowballRException.NotFoundException(
            entityType = EntityType.PAPER, requestId.toString(),
        )

        assertThrows<SnowballRException.NotFoundException> {
            mainService.getForwardReferencedPapers(
                getExampleRequest(),
            )
        }
    }

    @Test
    fun `When the forward references of an existing paper are retrieved successfully, then no error is thrown`() =
        runTest {
            mockHappyPathUntil(null)
            assertDoesNotThrow { mainService.getForwardReferencedPapers(getExampleRequest()) }
        }
}
