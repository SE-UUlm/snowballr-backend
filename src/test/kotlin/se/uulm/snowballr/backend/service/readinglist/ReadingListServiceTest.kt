package se.uulm.snowballr.backend.service.readinglist

import io.mockk.coEvery
import io.mockk.mockk
import se.uulm.snowballr.backend.context.RequestContext
import se.uulm.snowballr.backend.model.dto.user.User
import se.uulm.snowballr.backend.repository.IPaperTableRepo
import se.uulm.snowballr.backend.repository.IUserTableRepo
import se.uulm.snowballr.backend.repository.association.ICitationTableRepo
import se.uulm.snowballr.backend.repository.association.IReadingListTableRepo
import se.uulm.snowballr.backend.service.BaseServiceTest
import se.uulm.snowballr.backend.service.ReadingListService
import se.uulm.snowballr.backend.service.withUser

/**
 * Base test class for the [ReadingListService].
 */
sealed class ReadingListServiceTest : BaseServiceTest {
    val userRepoMock = mockk<IUserTableRepo>()
    val paperRepoMock = mockk<IPaperTableRepo>()
    val citationRepoMock = mockk<ICitationTableRepo>()
    val readingListRepoMock = mockk<IReadingListTableRepo>()

    private val allMocks = arrayOf(
        userRepoMock,
        paperRepoMock,
        citationRepoMock,
        readingListRepoMock,
    )

    val service = ReadingListService(
        userRepo = userRepoMock,
        paperRepo = paperRepoMock,
        citationRepo = citationRepoMock,
        repo = readingListRepoMock,
    )

    override fun getAllMocks(): Array<Any> = allMocks

    /**
     * Mock the current user that is passed through the [withUser] helper.
     */
    protected fun mockCurrentUser(currentUser: User) {
        RequestContext.current().userId = currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns Result.success(currentUser)
    }
}
