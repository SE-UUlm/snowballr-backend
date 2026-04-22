package se.uulm.snowballr.backend

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.AbstractOffsetDateTimeAssert
import se.uulm.snowballr.backend.env.Env
import java.nio.file.Path
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

/**
 * Same as [AbstractOffsetDateTimeAssert.isBetween] but with a delta in microseconds.
 *
 * @see AbstractOffsetDateTimeAssert.isBetween
 *
 * @param startInclusive Lower bound of the time interval (inclusive).
 * @param endInclusive Upper bound of the time interval (inclusive).
 * @param deltaInUs Delta in microseconds that the time must be within the interval. Defaults to 1000 (1 ms).
 */
fun AbstractOffsetDateTimeAssert<*>.isBetweenWithDelta(
    startInclusive: OffsetDateTime,
    endInclusive: OffsetDateTime,
    deltaInUs: Long = 1000,
): AbstractOffsetDateTimeAssert<*> {
    val start = startInclusive.minus(deltaInUs, ChronoUnit.MICROS)
    val end = endInclusive.plus(deltaInUs, ChronoUnit.MICROS)
    return this.isBetween(start, end)
}

/**
 * Same as [AbstractOffsetDateTimeAssert.isEqualTo] but with a delta in microseconds.
 *
 * @see AbstractOffsetDateTimeAssert.isEqualTo
 *
 * @param expected The expected value to compare against.
 * @param deltaInUs Delta in microseconds that the time must be within the expected value. Defaults to 1000000 (1 s).
 */
fun AbstractOffsetDateTimeAssert<*>.isEqualToWithDelta(
    expected: OffsetDateTime,
    deltaInUs: Long = 1_000_000,
): AbstractOffsetDateTimeAssert<*> = this.isBetweenWithDelta(expected, expected, deltaInUs)

/**
 * Creates a mock [Env] object with default values for all properties.
 *
 * This can be used in tests to provide a consistent environment configuration without relying on actual environment
 * variables or configuration files.
 */
fun mockEnvWithDefaultValues(): Env {
    val miscellaneousMock = mockk<Env.Miscellaneous>()
    every { miscellaneousMock.frontendBaseUrl } returns ""
    every { miscellaneousMock.logLevel } returns "DEBUG"

    val encryptionMock = mockk<Env.Encryption>()
    val (privateKeyBase64, publicKeyBase64) = RandomKeyGenerator.generateKeyPair()
    every { encryptionMock.jwtPrivateKeyBase64 } returns privateKeyBase64
    every { encryptionMock.jwtPublicKeyBase64 } returns publicKeyBase64

    val smtpMock = mockk<Env.SMTP>()
    every { smtpMock.smtpHost } returns ""
    every { smtpMock.smtpPort } returns 0
    every { smtpMock.smtpUser } returns ""
    every { smtpMock.smtpPassword } returns ""
    every { smtpMock.smtpTransportLoggingOnlyEnabled } returns true
    every { smtpMock.smtpSenderName } returns ""
    every { smtpMock.smtpSenderEmail } returns ""

    val lifetimeMock = mockk<Env.Lifetime>()
    every { lifetimeMock.sensitiveInformationRetentionDays } returns 30
    every { lifetimeMock.invitationTokenLifeTimeInDays } returns 7
    every { lifetimeMock.verificationTokenLifeTimeInDays } returns 1

    val pluginsMock = mockk<Env.Plugins>()
    every { pluginsMock.pluginDirectory } returns Path.of("plugins")
    every { pluginsMock.pythonExecutable } returns "python"

    val envMock = mockk<Env>()
    every { envMock.miscellaneous } returns miscellaneousMock
    every { envMock.encryption } returns encryptionMock
    every { envMock.smtp } returns smtpMock
    every { envMock.lifetime } returns lifetimeMock
    every { envMock.plugins } returns pluginsMock

    return envMock
}
