import urllib.parse
from typing import Any, Optional

from snowballr import (
    Author,
    Paper,
    fetcher_plugin,
    paginate_with_retry,
    request_with_retry,
    safe_get,
)

id_metadata_key: str = "SemanticScholarId"
corpus_id_metadata_key: str = "SemanticScholarCorpusId"

options = {"API_KEY": "SemanticScholar API key"}

base_url = "https://api.semanticscholar.org/graph/v1"
fields = "corpusId,title,externalIds,abstract,publicationDate,year,venue,publicationTypes,authors"


def search_papers(searchQuery: str, options: dict[str, str]) -> list[Paper]:
    """
    API reference:
    https://api.semanticscholar.org/api-docs/graph#tag/Paper-Data/operation/get_graph_paper_relevance_search

    This returns at most 25 papers.
    """
    url = f"{base_url}/paper/search"
    params = {
        "query": urllib.parse.quote_plus(searchQuery),
        "fields": fields,
        "limit": 25,
    }
    headers, timeout_seconds = _s2_params(options)

    data = request_with_retry(url, headers, params, timeout_seconds)
    papers = safe_get(data, "data", [])

    return list(map(paper_from_response, papers))


def forward_references(paper: Paper, options: dict[str, str]) -> list[Paper]:
    """
    API reference:
    https://api.semanticscholar.org/api-docs/graph#tag/Paper-Data/operation/get_graph_get_paper_citations
    """
    return get_references(paper, options, "citations", "citingPaper")


def backward_references(paper: Paper, options: dict[str, str]) -> list[Paper]:
    """
    API reference:
    https://api.semanticscholar.org/api-docs/graph#tag/Paper-Data/operation/get_graph_get_paper_references
    """
    return get_references(paper, options, "references", "citedPaper")


def get_references(
    paper: Paper, options: dict[str, str], url_suffix: str, obj_key: str
) -> list[Paper]:
    metadata = paper.fetcher_metadata
    paper_id = safe_get(metadata, id_metadata_key, metadata[corpus_id_metadata_key])
    # TODO: try other IDs (external IDs)
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

    return list(map(lambda obj: paper_from_response(safe_get(obj, obj_key, {})), paper_objects))


def _s2_params(options: dict[str, str]) -> tuple[dict[str, str], float]:
    headers = {}
    timeout_seconds = 0.0

    if "API_KEY" in options:
        headers["x-api-key"] = options["API_KEY"]
        timeout_seconds = 1.0  # 1 RPS for calls with API key

    return headers, timeout_seconds


def paper_from_response(res) -> Paper:
    authors = [
        author_from_response(author) for author in safe_get(res, "authors", []) if "name" in author
    ]
    external_id = external_id_from_response(safe_get(res, "externalIds", {}))

    date_str = safe_get(res, "publicationDate", "") or str(safe_get(res, "year", "0"))
    year = int(str(date_str)[:4] or "0")

    publication_type = next(iter(safe_get(res, "publicationTypes", [])), "")

    metadata = {}
    paper_id = res["paperId"]
    if paper_id is not None:
        metadata[id_metadata_key] = paper_id
    corpus_id = res["corpusId"]
    if corpus_id is not None:
        metadata[corpus_id_metadata_key] = str(corpus_id)

    return Paper(
        title=safe_get(res, "title", ""),
        external_id=external_id,
        abstract=safe_get(res, "abstract", ""),
        year=year,
        publisher="",
        publication_type=publication_type,
        publication_name=safe_get(res, "venue", ""),
        authors=authors,
        fetcher_metadata=metadata,
    )


def author_from_response(res) -> Author:
    first_name, _, last_name = safe_get(res, "name", "").rpartition(" ")
    return Author(
        first_name,
        last_name,
    )


def external_id_from_response(res) -> Optional[str]:
    # Order of external IDs to retrieve (first match is returned)
    order = ["DOI", "DBLP", "PubMed", "PubMedCentral", "Medline", "MAG", "ArXiv"]
    for key in order:
        if key in res:
            return res[key]
    return None


fetcher_plugin(
    options,
    search_papers,
    forward_references,
    backward_references,
)
