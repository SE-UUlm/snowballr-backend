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
import se.uulm.snowballr.backend.model.SnowballRException.InvalidIdException
import se.uulm.snowballr.backend.service.MainServiceTest
import snowballr.Base
import java.util.UUID
import java.util.stream.Stream
import kotlin.reflect.KFunction

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetPaperByIdTest : MainServiceTest() {
    private val requestId = UUID.randomUUID()

    private fun getExampleRequest() = Base.Id
        .newBuilder()
        .setId(requestId.toString())
        .build()

    fun failingFunctions(): Stream<Arguments?> = Stream.of(
        Arguments.of(paperRepoMock::getPaperById),
        Arguments.of(authorOfPaperRepoMock::getAuthorsOfPaperById),
        Arguments.of(citationRepoMock::getBackwardsReferencedPaperIdsOfPaperById),
    )

    @Suppress("ReturnCount")
    private fun mockHappyPathUntil(failAt: KFunction<*>?) {
        val paper = DataBuilder.createExamplePaper(id = requestId)
        val author = DataBuilder.createExampleAuthor()

        if (failAt == paperRepoMock::getPaperById) {
            coEvery { paperRepoMock.getPaperById(paper.id) } throws TestSpecificException()
            return
        }
        coEvery { paperRepoMock.getPaperById(paper.id) } returns paper

        if (failAt == authorOfPaperRepoMock::getAuthorsOfPaperById) {
            coEvery { authorOfPaperRepoMock.getAuthorsOfPaperById(paper.id) } throws TestSpecificException()
            return
        }
        coEvery { authorOfPaperRepoMock.getAuthorsOfPaperById(paper.id) } returns listOf(author)

        if (failAt == citationRepoMock::getBackwardsReferencedPaperIdsOfPaperById) {
            coEvery {
                citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paper.id)
            } throws TestSpecificException()
            return
        }
        coEvery {
            citationRepoMock.getBackwardsReferencedPaperIdsOfPaperById(paper.id)
        } returns listOf(UUID.randomUUID())
    }

    @Test
    fun `When parsing the paper ID fails, then an exception is thrown`() = runTest {
        val request = Base.Id.newBuilder().setId("invalid-uuid").build()

        assertThrows<InvalidIdException> { mainService.getPaperById(request) }
    }

    @ParameterizedTest
    @MethodSource("failingFunctions")
    fun `When a step fails, then an exception is thrown`(failAt: KFunction<*>) = runTest {
        mockHappyPathUntil(failAt)
        assertThrows<TestSpecificException> {
            mainService.getPaperById(getExampleRequest())
        }
    }

    @Test
    fun `When a paper is retrieved, then no error is thrown`() = runTest {
        mockHappyPathUntil(null)
        assertDoesNotThrow { mainService.getPaperById(getExampleRequest()) }
    }
}
