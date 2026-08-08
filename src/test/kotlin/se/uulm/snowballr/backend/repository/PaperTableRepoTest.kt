package se.uulm.snowballr.backend.repository

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
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.isBetweenWithDelta
import se.uulm.snowballr.backend.model.dto.paper.Author
import se.uulm.snowballr.backend.model.dto.paper.ExternalId
import se.uulm.snowballr.backend.model.dto.paper.ExternalIdType
import se.uulm.snowballr.backend.model.exception.NotFoundException
import se.uulm.snowballr.backend.model.exception.notfound.entity.PaperNotFoundException
import se.uulm.snowballr.backend.model.incoming.paper.CreatePaperRequest
import se.uulm.snowballr.backend.model.incoming.paper.UpdatePaperRequest
import se.uulm.snowballr.backend.repository.RepositoryHelper.insertExternalId
import se.uulm.snowballr.backend.repository.RepositoryHelper.insertPaperAndGetId
import se.uulm.snowballr.backend.table.PaperTable
import se.uulm.snowballr.backend.table.association.PaperHasExternalIdTable
import se.uulm.snowballr.backend.utils.assertResultFailure
import se.uulm.snowballr.backend.utils.assertResultSuccess
import java.sql.SQLException
import java.time.OffsetDateTime
import java.util.UUID

class PaperTableRepoTest : RepositoryTest(arrayOf(PaperTable, PaperHasExternalIdTable), false) {
    private val repo = PaperTableRepo(db)

    companion object {
        @JvmStatic
        fun validFieldMasks(): List<Arguments> = listOf(
            Arguments.of(listOf("paper.external_ids")),
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
            val paperId = insertPaperAndGetId()
            val externalId = insertExternalId(paperId)
            val result = repo.getPaperById(paperId)

            val paper = assertResultSuccess(result)
            with(paper) {
                assertEquals("Title", title)
                assertEquals(listOf(externalId), externalIds)
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
    inner class EnsurePaperExists {
        @Test
        fun `When a paper with the given ID exists, then no exception is thrown`() = runTest {
            val paperId = insertPaperAndGetId("Test Paper")

            assertDoesNotThrow { repo.ensurePaperExists(paperId) }
        }

        @Test
        fun `When a paper with the given ID does not exist, then a PaperNotFoundException is thrown`() = runTest {
            val paperId = UUID.randomUUID()

            assertThrows<PaperNotFoundException> { repo.ensurePaperExists(paperId) }
        }
    }

    @Nested
    inner class DoesPaperExistByExternalId {
        @Test
        fun `When a paper with the given external ID exists, then true is returned`() = runTest {
            val paperId = insertPaperAndGetId()
            val externalId = insertExternalId(paperId)

            val isPaperExistent = repo.doesPaperExistByExternalIds(listOf(externalId))

            assertTrue(isPaperExistent)
        }

        @Test
        fun `When a paper with the given external ID does not exist, then false returned`() = runTest {
            val externalId = DataBuilder.createExampleExternalId()

            val isPaperExistent = repo.doesPaperExistByExternalIds(listOf(externalId))

            assertFalse(isPaperExistent)
        }
    }

    @Nested
    inner class CreatePaper {
        fun getExamplePaperRequest(externalIds: List<ExternalId> = emptyList()) = CreatePaperRequest(
            title = "Title",
            externalIds = externalIds,
            abstract = "Abstract",
            year = 2025,
            publisher = "Publisher",
            publicationName = "PublicationName",
            publicationType = "PublicationType",
            authors = listOf(Author("FirstName", "LastName")),
            fetcherMetadata = emptyMap(),
        )

        @Test
        fun `When a paper is created, then the created paper is returned`() = runTest {
            val externalId = DataBuilder.createExampleExternalId()
            val request = getExamplePaperRequest(listOf(externalId))

            val start = OffsetDateTime.now()
            val createdPaper = repo.createPaper(request)
            val end = OffsetDateTime.now()

            with(createdPaper) {
                assertNotNull(id)
                assertEquals("Title", title)
                assertEquals(listOf(externalId), externalIds)
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
                val paperId = insertPaperAndGetId()
                val externalId = insertExternalId(paperId)

                val request = getExamplePaperRequest(externalIds = listOf(externalId))

                assertThrows<SQLException> { repo.createPaper(request) }
            }

        @Test
        fun `When a paper is created without external IDs, then the paper is created without external IDs`() = runTest {
            val request = getExamplePaperRequest(externalIds = emptyList())

            val createdPaper = repo.createPaper(request)

            assertEquals(0, createdPaper.externalIds.size)
        }

        @Test
        fun `When a paper is created with fetcher metadata, then the metadata is persisted`() = runTest {
            val metadata = mapOf(
                "id" to UUID.randomUUID().toString(),
                "foo" to "bar",
            )
            val request = getExamplePaperRequest().copy(fetcherMetadata = metadata)

            val createdPaper = repo.createPaper(request)

            assertEquals(metadata.size, createdPaper.fetcherMetadata.size)
            for ((key, value) in metadata) {
                val actual = createdPaper.fetcherMetadata[key]
                assertEquals(value, actual)
            }
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
            val paperId = insertPaperAndGetId()
            val externalId = insertExternalId(paperId, ExternalIdType.DOI, "10.1234/5678")
            val paper = repo.getPaperById(paperId).getOrThrow()

            val updatedPaperDetails = paper.copy(
                title = "Updated Title",
                externalIds = listOf(ExternalId(ExternalIdType.URL, "https://updated-ex-id.com")),
                abstract = "Updated Abstract",
                year = paper.year - 10,
                publisher = "Updated Publisher",
                publicationName = "Updated PublicationName",
                publicationType = "Updated PublicationType",
                authors = listOf(Author("UpdatedFirstName", "UpdatedLastName")),
            )
            val request = UpdatePaperRequest.fromPaper(updatedPaperDetails)

            val start = OffsetDateTime.now()
            val updatedPaper = repo.updatePaper(request, fieldMask)
            val end = OffsetDateTime.now()

            if ("paper.external_ids" in fieldMask) {
                val updatedExternalId = ExternalId(ExternalIdType.URL, "https://updated-ex-id.com")
                assertThat(updatedPaper.externalIds).isEqualTo(listOf(updatedExternalId))
            } else {
                assertThat(updatedPaper.externalIds).isEqualTo(listOf(externalId))
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
            val request = UpdatePaperRequest.fromPaper(paper)

            val updatedPaper = repo.updatePaper(request, emptyList())

            assertThat(updatedPaper).isEqualTo(paper)
            assertThat(updatedPaper.modifiedAt).isNull()
        }
    }

    @Nested
    inner class GetPapersBySearchQuery {
        @Test
        fun `When a paper is matching the search query, then the paper is returned`() = runTest {
            val paper1 = insertPaperAndGetId(title = "Something about IT")
            val externalId = insertExternalId(paper1)
            val paper2 = insertPaperAndGetId(title = "Something about AI")
            val paper3 = insertPaperAndGetId(title = "Something about Cats")

            val matchingPapers = repo.getPapersBySearchQuery("Something about")

            assertEquals(3, matchingPapers.size)
            assertThat(matchingPapers.map { it.id }).containsExactlyInAnyOrder(paper1, paper2, paper3)
            assertEquals(listOf(externalId), matchingPapers.find { it.externalIds.isNotEmpty() }?.externalIds)
        }

        @Test
        fun `When no paper is matching the search query, then an empty list is returned`() = runTest {
            insertPaperAndGetId(title = "Cats are beautiful")

            val matchingPapers = repo.getPapersBySearchQuery("Dogs not that great")

            assertEquals(0, matchingPapers.size)
        }

        @Test
        fun `When more than 20 papers match the search query, then only the first 20 matching papers are returned`() =
            runTest {
                for (i in 1..25) {
                    insertPaperAndGetId(title = "Frontend Framework Number $i")
                }

                val matchingPapers = repo.getPapersBySearchQuery("Frontend Framework")

                assertEquals(20, matchingPapers.size)
            }
    }

    @Nested
    inner class GetPapersByExternalIds {
        @Test
        fun `When papers are found by their external IDs, then the papers are returned`() = runTest {
            val paper1 = insertPaperAndGetId()
            val externalId1 = insertExternalId(paper1, value = "doi123")
            val paper2 = insertPaperAndGetId()
            val externalId2 = insertExternalId(paper2, value = "doi456")
            val paper3 = insertPaperAndGetId()
            val externalId3 = insertExternalId(paper3, value = "doi789")

            val papers = repo.getPapersByExternalIds(listOf(externalId1, externalId2, externalId3))

            assertEquals(3, papers.size)
            assertThat(papers.map { it.id }).containsExactlyInAnyOrder(paper1, paper2, paper3)
        }

        @Test
        fun `When no papers match the external IDs, then no papers are returned`() = runTest {
            val paper1 = insertPaperAndGetId()
            insertExternalId(paper1, value = "doi123")
            val paper2 = insertPaperAndGetId()
            insertExternalId(paper2, value = "doi456")
            val paper3 = insertPaperAndGetId()
            insertExternalId(paper3, value = "doi789")

            val papers = repo.getPapersByExternalIds(
                listOf(
                    DataBuilder.createExampleExternalId(value = "foo"),
                    DataBuilder.createExampleExternalId(value = "bar"),
                    DataBuilder.createExampleExternalId(value = "cat"),
                ),
            )

            assertEquals(0, papers.size)
        }

        @Test
        fun `When an empty list is passed, then an empty list is returned`() = runTest {
            val papers = repo.getPapersByExternalIds(emptyList())

            assertEquals(0, papers.size)
        }

        @Test
        fun `When a paper is found by one of its external IDs, then all of its external IDs are returned`() = runTest {
            val paperId = insertPaperAndGetId()
            val doi = insertExternalId(paperId, type = ExternalIdType.DOI, value = "10.1234/5678")
            val url = insertExternalId(paperId, type = ExternalIdType.URL, value = "https://example.com")

            val papers = repo.getPapersByExternalIds(listOf(doi))

            assertEquals(1, papers.size)
            assertThat(papers.first().externalIds).containsExactlyInAnyOrder(doi, url)
        }
    }

    @Nested
    inner class GetPapersByYear {
        @Test
        fun `When a paper's year exactly matches, then the paper is returned`() = runTest {
            val paperId = insertPaperAndGetId(year = 2020)

            val papers = repo.getPapersByYear(2020, tolerance = 0)

            assertThat(papers.map { it.id }).containsExactly(paperId)
        }

        @Test
        fun `When a paper's year is within the tolerance, then the paper is returned`() = runTest {
            val paperId = insertPaperAndGetId(year = 2018)

            val papers = repo.getPapersByYear(2020, tolerance = 2)

            assertThat(papers.map { it.id }).containsExactly(paperId)
        }

        @Test
        fun `When a paper's year is outside the tolerance, then the paper is not returned`() = runTest {
            insertPaperAndGetId(year = 2015)

            val papers = repo.getPapersByYear(2020, tolerance = 2)

            assertEquals(0, papers.size)
        }

        @Test
        fun `When multiple papers are within the tolerance, then all of them are returned`() = runTest {
            val paper1 = insertPaperAndGetId(year = 2019)
            val paper2 = insertPaperAndGetId(year = 2020)
            val paper3 = insertPaperAndGetId(year = 2021)
            insertPaperAndGetId(year = 1990)

            val papers = repo.getPapersByYear(2020, tolerance = 1)

            assertThat(papers.map { it.id }).containsExactlyInAnyOrder(paper1, paper2, paper3)
        }

        @Test
        fun `When no paper matches the year, then an empty list is returned`() = runTest {
            insertPaperAndGetId(year = 2020)

            val papers = repo.getPapersByYear(1990, tolerance = 0)

            assertEquals(0, papers.size)
        }

        @Test
        fun `When a paper is found by year, then all of its external IDs are returned`() = runTest {
            val paperId = insertPaperAndGetId(year = 2020)
            val doi = insertExternalId(paperId, type = ExternalIdType.DOI, value = "10.1234/5678")
            val url = insertExternalId(paperId, type = ExternalIdType.URL, value = "https://example.com")

            val papers = repo.getPapersByYear(2020, tolerance = 0)

            assertEquals(1, papers.size)
            assertThat(papers.first().externalIds).containsExactlyInAnyOrder(doi, url)
        }
    }

    @Nested
    inner class MergeFetcherMetadata {
        @Test
        fun `When the merged metadata has new keys, then they are added to the stored metadata`() = runTest {
            val paperId = insertPaperAndGetId(fetcherMetadata = mapOf("old" to "value"))

            repo.mergeFetcherMetadata(paperId, mapOf("id" to "123", "foo" to "bar"))

            val paper = repo.getPaperById(paperId).getOrThrow()
            assertEquals(mapOf("old" to "value", "id" to "123", "foo" to "bar"), paper.fetcherMetadata)
        }

        @Test
        fun `When the merged metadata has a stored key, then the stored value wins`() = runTest {
            val paperId = insertPaperAndGetId(fetcherMetadata = mapOf("id" to "stored"))

            repo.mergeFetcherMetadata(paperId, mapOf("id" to "fetched"))

            val paper = repo.getPaperById(paperId).getOrThrow()
            assertEquals(mapOf("id" to "stored"), paper.fetcherMetadata)
        }

        @Test
        fun `When fetcher metadata is merged, then modifiedAt is not changed`() = runTest {
            val paperId = insertPaperAndGetId()

            repo.mergeFetcherMetadata(paperId, mapOf("id" to "123"))

            val paper = repo.getPaperById(paperId).getOrThrow()
            assertNull(paper.modifiedAt)
        }

        @Test
        fun `When fetcher metadata is merged with an empty map, then the stored metadata is unchanged`() = runTest {
            val paperId = insertPaperAndGetId(fetcherMetadata = mapOf("old" to "value"))

            repo.mergeFetcherMetadata(paperId, emptyMap())

            val paper = repo.getPaperById(paperId).getOrThrow()
            assertEquals(mapOf("old" to "value"), paper.fetcherMetadata)
        }

        @Test
        fun `When two merges interleave, then no keys are lost`() = runTest {
            val paperId = insertPaperAndGetId()

            repo.mergeFetcherMetadata(paperId, mapOf("a" to "1"))
            repo.mergeFetcherMetadata(paperId, mapOf("b" to "2"))

            val paper = repo.getPaperById(paperId).getOrThrow()
            assertEquals(mapOf("a" to "1", "b" to "2"), paper.fetcherMetadata)
        }
    }
}
