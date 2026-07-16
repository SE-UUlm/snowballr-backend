import urllib.parse
from typing import Any, Optional

from snowballr import (
    Author,
    ExternalId,
    FetcherInformation,
    FetcherOptionsSchema,
    Link,
    Paper,
    fetcher_plugin,
    paginate_with_retry,
    request_with_retry,
    safe_get,
)

fetcher_information = FetcherInformation(
    name="Semantic Scholar",
    description=(
        "Semantic Scholar is a free, AI-powered academic search engine by the Allen Institute for "
        "AI, indexing over 214 million papers and 2.49 billion citations across all research "
        "fields. Supports paper search, forward references (papers that cite a paper), "
        "and backward references (papers cited by a paper).\n\n"
        "Without an API key, requests share a public pool (1,000 req/s total). "
        "With an API key, each user gets 1 dedicated request per second.\n\n"
        "Note: Some information that is displayed on the Semantic Scholar Website, such as the "
        "abstract or the references may be elided in the information provided by the fetcher."
    ),
    links=[
        Link("Homepage", "https://www.semanticscholar.org/"),
        Link("Request API Key", "https://www.semanticscholar.org/product/api#api-key-form"),
    ],
    options_schema={
        "API_KEY": FetcherOptionsSchema(
            name="API Key",
            description="Semantic Scholar API key",
            required=False,
            is_secret=True,
        )
    },
)

id_metadata_key: str = "SemanticScholarId"
corpus_id_metadata_key: str = "SemanticScholarCorpusId"

base_url = "https://api.semanticscholar.org/graph/v1"
fields = "corpusId,title,externalIds,abstract,publicationDate,year,venue,publicationTypes,authors"


def search_papers(search_query: str, options: dict[str, str]) -> list[Paper]:
    """
    API reference:
    https://api.semanticscholar.org/api-docs/graph#tag/Paper-Data/operation/get_graph_paper_relevance_search

    This returns at most 25 papers.
    """
    url = f"{base_url}/paper/search"
    params = {
        "query": urllib.parse.quote_plus(search_query),
        "fields": fields,
        "limit": 25,
    }
    headers, timeout_seconds = _s2_params(options)

    data = request_with_retry(url, headers, params, timeout_seconds)
    papers = safe_get(data, "data", [])

    return list(map(_paper_from_response, papers))


def forward_references(paper: Paper, options: dict[str, str]) -> list[Paper]:
    """
    API reference:
    https://api.semanticscholar.org/api-docs/graph#tag/Paper-Data/operation/get_graph_get_paper_citations
    """
    return _get_references(paper, options, "citations", "citingPaper")


def backward_references(paper: Paper, options: dict[str, str]) -> list[Paper]:
    """
    API reference:
    https://api.semanticscholar.org/api-docs/graph#tag/Paper-Data/operation/get_graph_get_paper_references
    """
    return _get_references(paper, options, "references", "citedPaper")


def _get_references(
    paper: Paper, options: dict[str, str], url_suffix: str, obj_key: str
) -> list[Paper]:
    paper_id = _construct_paper_id(paper)
    if paper_id is None:
        return []

    url = f"{base_url}/paper/{paper_id}/{url_suffix}"
    params = {
        "fields": fields,
        "limit": 1000,
    }
    headers, timeout_seconds = _s2_params(options)

    def next_url(data: dict[str, Any]) -> Optional[str]:
        next_offset = data.get("next")
        return f"{url}?offset={next_offset}" if next_offset is not None else None

    paper_objects = []
    for page in paginate_with_retry(url, next_url, headers, params, timeout_seconds):
        paper_objects += safe_get(page, "data", [])

    return list(map(lambda obj: _paper_from_response(safe_get(obj, obj_key, {})), paper_objects))


def _construct_paper_id(paper: Paper) -> Optional[str]:
    metadata = paper.fetcher_metadata

    if (s2_id := metadata.get(id_metadata_key)) is not None:
        return s2_id

    if (s2_corpus_id := metadata.get(corpus_id_metadata_key)) is not None:
        return f"CorpusId:{s2_corpus_id}"

    ext_id_map = {ext.type: ext.value for ext in paper.external_ids}
    key_mapping = {
        "SEMANTIC_SCHOLAR": "",
        "DOI": "DOI:",
        "ARXIV": "ARXIV:",
        "MAG": "MAG:",
        "ACL": "ACL:",
        "PUB_MED": "PMID:",
        "MEDLINE": "PMID:",
        "PUB_MED_CENTRAL": "PMCID:",
        "URL": "URL:",
        # DBLP is not available as paper ID
    }

    for ex_type, prefix in key_mapping.items():
        if ex_type in ext_id_map:
            return f"{prefix}{ext_id_map[ex_type]}"

    return None


def _s2_params(options: dict[str, str]) -> tuple[dict[str, str], float]:
    headers = {}
    timeout_seconds = 0.0

    if "API_KEY" in options:
        headers["x-api-key"] = options["API_KEY"]
        timeout_seconds = 1.0  # 1 RPS for calls with API key

    return headers, timeout_seconds


def _paper_from_response(res) -> Paper:
    authors = [
        _author_from_response(author) for author in safe_get(res, "authors", []) if "name" in author
    ]
    external_ids = _external_ids_from_response(safe_get(res, "externalIds", {}))

    date_str = safe_get(res, "publicationDate", "") or str(safe_get(res, "year", "0"))
    year = int(str(date_str)[:4] or "0")

    publication_type = next(iter(safe_get(res, "publicationTypes", [])), "")

    metadata = {}
    paper_id = res["paperId"]
    if paper_id is not None:
        metadata[id_metadata_key] = paper_id
        external_ids.append(ExternalId("SEMANTIC_SCHOLAR", paper_id))
    corpus_id = res["corpusId"]
    if corpus_id is not None:
        metadata[corpus_id_metadata_key] = str(corpus_id)

    return Paper(
        title=safe_get(res, "title", ""),
        external_ids=external_ids,
        abstract=safe_get(res, "abstract", ""),
        year=year,
        publisher="",
        publication_type=publication_type,
        publication_name=safe_get(res, "venue", ""),
        authors=authors,
        fetcher_metadata=metadata,
    )


def _author_from_response(res) -> Author:
    first_name, _, last_name = safe_get(res, "name", "").rpartition(" ")
    return Author(
        first_name,
        last_name,
    )


def _external_ids_from_response(res) -> list[ExternalId]:
    # Map the response key to the target ExternalId type string
    key_mapping = {
        "DOI": "DOI",
        "ArXiv": "ARXIV",
        "MAG": "MAG",
        "ACL": "ACL",
        "PubMed": "PUB_MED",
        "Medline": "MEDLINE",
        "PubMedCentral": "PUB_MED_CENTRAL",
        "DBLP": "DBLP",
    }

    return [
        ExternalId(id_type, res[key])
        for key, id_type in key_mapping.items()
        if key in res and res[key] is not None
    ]


fetcher_plugin(
    information=fetcher_information,
    query=search_papers,
    forwards=forward_references,
    backwards=backward_references,
)
