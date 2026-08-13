package se.uulm.snowballr.backend.model.dto.criterion

enum class CriterionField(val grpcPath: String) {
    TAG("criterion.tag"),
    NAME("criterion.name"),
    DESCRIPTION("criterion.description"),
    CATEGORY("criterion.category"),
}
