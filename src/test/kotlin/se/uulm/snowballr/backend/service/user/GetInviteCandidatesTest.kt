package se.uulm.snowballr.backend.service.user

import io.mockk.coEvery
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.service.MainServiceTest
import snowballr.UserOuterClass

@DelicateCoroutinesApi
@ExperimentalCoroutinesApi
class GetInviteCandidatesTest : MainServiceTest() {
    private val validSearchQuery = UserOuterClass.User.SearchQuery.newBuilder().setQuery("john").build()

    @Test
    fun `When the search query is too short, then an empty list is returned`() = runTest {
        val shortSearchQuery = UserOuterClass.User.SearchQuery.newBuilder().setQuery("ab").build()
        assertDoesNotThrow { mainService.getInviteCandidates(shortSearchQuery) }
    }

    @Test
    fun `When the retrieving the invite candidates fails, then exception is thrown`() = runTest {
        coEvery { userRepoMock.getUsersMatchingSearchQuery(any()) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.getInviteCandidates(validSearchQuery) }
    }

    @Test
    fun `When the retrieving the invite candidates is successful, then no exception is thrown`() = runTest {
        coEvery { userRepoMock.getUsersMatchingSearchQuery(any()) } returns emptyList()

        assertDoesNotThrow { mainService.getInviteCandidates(validSearchQuery) }
    }
}
