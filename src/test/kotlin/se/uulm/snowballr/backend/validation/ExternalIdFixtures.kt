package se.uulm.snowballr.backend.validation

import se.uulm.snowballr.backend.model.dto.paper.ExternalIdType

/**
 * One realistic, well-formed example value per [ExternalIdType], shared across validator tests that need a
 * generally valid external ID for a given type.
 */
val VALID_EXTERNAL_ID_VALUES: Map<ExternalIdType, String> = mapOf(
    ExternalIdType.DOI to "10.1000/xyz123",
    ExternalIdType.ARXIV to "2101.00001",
    ExternalIdType.MAG to "1234567890",
    ExternalIdType.ACL to "P19-1001",
    ExternalIdType.PUB_MED to "12345678",
    ExternalIdType.MEDLINE to "12345678",
    ExternalIdType.PUB_MED_CENTRAL to "PMC1234567",
    ExternalIdType.DBLP to "journals/tods/Bernstein83",
    ExternalIdType.SEMANTIC_SCHOLAR to "204e3073870fae3d05bcbc2f6a8e263d9b72e776",
)
