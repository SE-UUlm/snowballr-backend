package se.uulm.snowballr.backend.model.dto.paper

enum class PaperField(val grpcPath: String) {
    TITLE("paper.title"),
    ABSTRACT("paper.abstrakt"),
    YEAR("paper.year"),
    PUBLISHER("paper.publisher"),
    PUBLICATION_NAME("paper.publication_name"),
    PUBLICATION_TYPE("paper.publication_type"),
    AUTHORS("paper.authors"),
    EXTERNAL_IDS("paper.external_ids"),
}
