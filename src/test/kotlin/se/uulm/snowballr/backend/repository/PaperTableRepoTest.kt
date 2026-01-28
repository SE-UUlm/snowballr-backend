package se.uulm.snowballr.backend.repository

import com.google.protobuf.util.FieldMaskUtil
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertNull
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import se.uulm.snowballr.backend.isBetweenWithDelta
import se.uulm.snowballr.backend.model.dto.toGrpcPaper
import se.uulm.snowballr.backend.model.exception.NotFoundException
import se.uulm.snowballr.backend.model.exception.notfound.entity.PaperNotFoundException
import se.uulm.snowballr.backend.repository.RepositoryHelper.insertPaperAndGetId
import se.uulm.snowballr.backend.table.PaperTable
import se.uulm.snowballr.backend.utils.assertResultFailure
import se.uulm.snowballr.backend.utils.assertResultSuccess
import snowballr.PaperOuterClass.Author
import snowballr.PaperOuterClass.Paper
import snowballr.author
import java.sql.SQLException
import java.time.OffsetDateTime
import java.util.UUID

class PaperTableRepoTest : RepositoryTest(arrayOf(PaperTable), false) {
    private val repo = PaperTableRepo(db)

    companion object {
        @JvmStatic
        fun validFieldMasks(): List<Arguments> = listOf(
            Arguments.of(listOf("paper.external_id")),
            Arguments.of(listOf("paper.title", "paper.abstrakt")),
            Arguments.of(listOf("paper.year")),
            Arguments.of(listOf("paper.publisher")),
            Arguments.of(listOf("paper.publication_name", "paper.publication_type")),
            Arguments.of(listOf("paper.authors")),
        )
    }

    @Nested
    inner class GetPaperById {
        @Test
        fun `When a paper is found, then a successful result with the correct paper is returned`() = runTest {
            val paperId = insertPaperAndGetId(externalId = "ExternalId")
            val result = repo.getPaperById(paperId)

            val paper = assertResultSuccess(result)
            with(paper) {
                assertEquals("Title", title)
                assertEquals("ExternalId", externalId)
                assertEquals("Abstract", abstract)
                assertEquals(2025, year)
                assertEquals("Publisher", publisher)
                assertEquals("PublicationType", publicationType)
                assertEquals("PublicationName", publicationName)
                assertThat(fetcherMetadata).isEmpty()
                assertThat(authors).isEmpty()
            }
        }

        @Test
        fun `When a paper is not found, then a failed result with a NotFoundException is returned`() = runTest {
            val result = repo.getPaperById(UUID.randomUUID())

            assertResultFailure<NotFoundException>(result)
        }
    }

    @Nested
    inner class GetPaperByExternalId {
        @Test
        fun `When a paper is found, then a successful result with the correct paper is returned`() = runTest {
            val externalId = "ExternalId"
            insertPaperAndGetId(externalId = externalId)

            val paper = assertResultSuccess(repo.getPaperByExternalId(externalId))

            with(paper) {
                assertEquals("Title", title)
                assertEquals("ExternalId", externalId)
                assertEquals("Abstract", abstract)
                assertEquals(2025, year)
                assertEquals("Publisher", publisher)
                assertEquals("PublicationType", publicationType)
                assertEquals("PublicationName", publicationName)
                assertThat(fetcherMetadata).isEmpty()
            }
        }

        @Test
        fun `When a paper is not found, then a failed result with a NotFoundException is returned`() = runTest {
            val result = repo.getPaperByExternalId("NonExistentExternalId")

            assertResultFailure<NotFoundException>(result)
        }
    }

    @Nested
    inner class EnsurePaperExists {
        @Test
        fun `When a paper with the given id exists, then no exception is thrown`() = runTest {
            val paperId = insertPaperAndGetId("Test Paper")

            assertDoesNotThrow { repo.ensurePaperExists(paperId) }
        }

        @Test
        fun `When a paper with the given id does not exist, then a PaperNotFoundException is thrown`() = runTest {
            val paperId = UUID.randomUUID()

            assertThrows<PaperNotFoundException> { repo.ensurePaperExists(paperId) }
        }
    }

    @Nested
    inner class DoesPaperExistByExternalId {
        @Test
        fun `When a paper with the given external id exists, then true is returned`() = runTest {
            val externalId = "ExternalId"
            insertPaperAndGetId(externalId = externalId)

            val isPaperExistent = repo.doesPaperExistByExternalId(externalId)

            assertTrue(isPaperExistent)
        }

        @Test
        fun `When a paper with the given external id does not exist, then false returned`() = runTest {
            val externalId = "NonExistentExternalId"

            val isPaperExistent = repo.doesPaperExistByExternalId(externalId)

            assertFalse(isPaperExistent)
        }
    }

    @Nested
    inner class CreatePaper {
        fun getExamplePaperRequest(externalId: String = "ExternalId"): Paper = Paper.newBuilder()
            .setExternalId(externalId)
            .setTitle("Title")
            .setAbstrakt("Abstract")
            .setYear(2025)
            .setPublisher("Publisher")
            .setPublicationName("PublicationName")
            .setPublicationType("PublicationType")
            .addAllAuthors(
                listOf(
                    author {
                        firstName = "FirstName"
                        lastName = "LastName"
                    },
                ),
            )
            .build()

        @Test
        fun `When a paper is created, then the created paper is returned`() = runTest {
            val request = getExamplePaperRequest()

            val start = OffsetDateTime.now()
            val createdPaper = repo.createPaper(request)
            val end = OffsetDateTime.now()

            with(createdPaper) {
                assertNotNull(id)
                assertEquals("Title", title)
                assertEquals("ExternalId", externalId)
                assertEquals("Abstract", abstract)
                assertEquals(2025, year)
                assertEquals("Publisher", publisher)
                assertEquals("PublicationType", publicationType)
                assertEquals("PublicationName", publicationName)
                assertThat(fetcherMetadata).isEmpty()
                assertThat(authors).hasSize(1)
                assertThat(authors[0].firstName).isEqualTo("FirstName")
                assertThat(authors[0].lastName).isEqualTo("LastName")
                assertThat(createdAt).isBetweenWithDelta(start, end)
                assertNull(modifiedAt)
                assertNull(modifiedBy)
            }
        }

        @Test
        fun `When a paper is created with a external ID that already exists, then creating the paper fails`() =
            runTest {
                val externalId = "ExternalId"
                insertPaperAndGetId(externalId = externalId)

                val request = getExamplePaperRequest(externalId = externalId)

                assertThrows<SQLException> { repo.createPaper(request) }
            }

        @Test
        fun `When a paper is created with an empty external ID, then the paper is created with a null external ID`() =
            runTest {
                val request = getExamplePaperRequest(externalId = "")

                val createdPaper = repo.createPaper(request)

                assertNull(createdPaper.externalId)
            }
    }

    @Nested
    inner class UpdatePaper {
        @ParameterizedTest(name = "Update the fields {0}")
        @MethodSource("se.uulm.snowballr.backend.repository.PaperTableRepoTest#validFieldMasks")
        @Suppress("LongMethod")
        fun `When a paper is updated, then only the fields specified in the field mask are updated and the updated paper is returned`(
            fieldMask: List<String>,
        ) = runTest {
            val externalId = UUID.randomUUID().toString()
            val paperId = insertPaperAndGetId(externalId = externalId)
            val paper = repo.getPaperById(paperId).getOrThrow()

            val updatedPaperDetails = paper.toGrpcPaper(emptyList()).toBuilder()
                .setExternalId("updated-external-id")
                .setTitle("Updated Title")
                .setAbstrakt("Updated Abstract")
                .setYear(paper.year - 10)
                .setPublisher("Updated Publisher")
                .setPublicationName("Updated PublicationName")
                .setPublicationType("Updated PublicationType")
                .addAllAuthors(
                    listOf(
                        Author.newBuilder()
                            .setFirstName("UpdatedFirstName")
                            .setLastName("UpdatedLastName")
                            .build(),
                    ),
                )
                .build()

            val request = Paper.Update.newBuilder()
                .setPaper(updatedPaperDetails)
                .setMask(FieldMaskUtil.fromStringList(fieldMask))
                .build()

            val start = OffsetDateTime.now()
            val updatedPaper = repo.updatePaper(request)
            val end = OffsetDateTime.now()

            if ("paper.external_id" in fieldMask) {
                assertThat(updatedPaper.externalId).isEqualTo("updated-external-id")
            } else {
                assertThat(updatedPaper.externalId).isEqualTo(externalId)
            }
            if ("paper.title" in fieldMask) {
                assertThat(updatedPaper.title).isEqualTo("Updated Title")
            } else {
                assertThat(updatedPaper.title).isEqualTo("Title")
            }
            if ("paper.abstrakt" in fieldMask) {
                assertThat(updatedPaper.abstract).isEqualTo("Updated Abstract")
            } else {
                assertThat(updatedPaper.abstract).isEqualTo("Abstract")
            }
            if ("paper.year" in fieldMask) {
                assertThat(updatedPaper.year).isEqualTo(paper.year - 10)
            } else {
                assertThat(updatedPaper.year).isEqualTo(2025)
            }
            if ("paper.publisher" in fieldMask) {
                assertThat(updatedPaper.publisher).isEqualTo("Updated Publisher")
            } else {
                assertThat(updatedPaper.publisher).isEqualTo("Publisher")
            }
            if ("paper.publication_name" in fieldMask) {
                assertThat(updatedPaper.publicationName).isEqualTo("Updated PublicationName")
            } else {
                assertThat(updatedPaper.publicationName).isEqualTo("PublicationName")
            }
            if ("paper.publication_type" in fieldMask) {
                assertThat(updatedPaper.publicationType).isEqualTo("Updated PublicationType")
            } else {
                assertThat(updatedPaper.publicationType).isEqualTo("PublicationType")
            }
            if ("paper.authors" in fieldMask) {
                assertThat(updatedPaper.authors).hasSize(1)
                assertThat(updatedPaper.authors[0].firstName).isEqualTo("UpdatedFirstName")
                assertThat(updatedPaper.authors[0].lastName).isEqualTo("UpdatedLastName")
            } else {
                assertThat(updatedPaper.authors).isEmpty()
            }

            assertThat(updatedPaper.modifiedAt).isBetweenWithDelta(start, end)
        }

        @Test
        fun `When a paper is updated with an empty field mask, then nothing is updated`() = runTest {
            val paperId = insertPaperAndGetId()
            val paper = repo.getPaperById(paperId).getOrThrow()

            val request = Paper.Update.newBuilder()
                .setPaper(paper.toGrpcPaper(emptyList()))
                .setMask(FieldMaskUtil.fromStringList(emptyList()))
                .build()

            val updatedPaper = repo.updatePaper(request)

            assertThat(updatedPaper).isEqualTo(paper)
            assertThat(updatedPaper.modifiedAt).isNull()
        }
    }
}
