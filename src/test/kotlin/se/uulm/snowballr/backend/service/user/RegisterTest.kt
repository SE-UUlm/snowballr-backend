package se.uulm.snowballr.backend.service.user

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.RandomKeyGenerator
import se.uulm.snowballr.backend.env.EnvReader
import se.uulm.snowballr.backend.model.SnowballRException
import se.uulm.snowballr.backend.service.MainServiceTest
import se.uulm.snowballr.backend.testCoroutine
import snowballr.Authentication

@ExperimentalCoroutinesApi
@DelicateCoroutinesApi
class RegisterTest : MainServiceTest(), KoinTest {
    private val envReaderMock = mockk<EnvReader>()

    @BeforeEach
    override fun setUpTest() {
        super.setUpTest()
        // Stop any existing Koin context
        stopKoin()

        // Start Koin context with a mock module
        startKoin {
            modules(
                module {
                    single { envReaderMock }
                },
            )
        }

        // Mock JWT key pair
        val (privateKeyBase64, publicKeyBase64) = RandomKeyGenerator.generateKeyPair()
        every { envReaderMock.env.encryption.jwtPrivateKeyBase64 } returns privateKeyBase64
        every { envReaderMock.env.encryption.jwtPublicKeyBase64 } returns publicKeyBase64
    }

    @AfterEach
    override fun tearDownTest() {
        stopKoin()
        super.tearDownTest()
    }

    @Test
    fun `When a user is registered, then no exception is thrown`() = testCoroutine {
        val request = Authentication.RegisterRequest.newBuilder().build()
        val user = DataBuilder.createExampleUser()

        coEvery { userRepoMock.doesUserExistByEmail(any()) } returns false
        coEvery { userRepoMock.createUser(any(), any()) } returns user

        assertDoesNotThrow { mainService.register(request) }
    }

    @Test
    fun `When a user already exists, then an exception is thrown`() = testCoroutine {
        val request = Authentication.RegisterRequest.newBuilder().build()

        coEvery { userRepoMock.doesUserExistByEmail(any()) } returns true

        assertThrows<SnowballRException.DuplicateEntityException.UserEmail> { mainService.register(request) }
    }

    @Test
    fun `When an error occurs while creating a user, then an exception is thrown`() = testCoroutine {
        val request = Authentication.RegisterRequest.newBuilder().build()

        coEvery { userRepoMock.doesUserExistByEmail(any()) } returns false
        coEvery { userRepoMock.createUser(any(), any()) } throws Exception("Failed to create user")

        assertThrows<Exception> { mainService.register(request) }
    }
}
