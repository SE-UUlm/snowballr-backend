import urllib.parse
from typing import Any, Optional

import requests
from snowballr import Author, Paper, fetcher_plugin, safe_get

id_metadata_key: str = "SemanticScholarId"
corpus_id_metadata_key: str = "SemanticScholarCorpusId"

options = {"API_KEY": "SemanticScholar API key"}

base_url = "https://api.semanticscholar.org/graph/v1/"
fields = "corpusId,title,externalIds,abstract,publicationDate,year,venue,publicationTypes,authors"


def search_papers(searchQuery: str, options: dict[str, str]) -> list[Paper]:
    """
    API reference:
    https://api.semanticscholar.org/api-docs/graph#tag/Paper-Data/operation/get_graph_paper_relevance_search
    """
    query = urllib.parse.quote_plus(searchQuery)
    url = f"{base_url}paper/search?query={query}&fields={fields}&limit=25"

    data = request_with_retry(url, options)
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
    paper_id = safe_get(metadata, id_metadata_key, metadata.get(corpus_id_metadata_key, None))
    # TODO: try other IDs (external IDs)
    if paper_id is None:
        return []

    url = f"{base_url}paper/{paper_id}/{url_suffix}?fields={fields}&limit=1000"
    paper_objects = []
    offset = 0

    # Use pagination to request all references
    while True:
        offset_url = f"{url}&offset={offset}"
        data = request_with_retry(offset_url, options)
        paper_objects += safe_get(data, "data", [])

        # 'next' is None if last page is reached
        if data.get("next") is None:
            break

        offset += 1000

    return list(map(lambda obj: paper_from_response(safe_get(obj, obj_key, {})), paper_objects))


def request_with_retry(url: str, options: dict[str, str]) -> dict[str, Any]:
    """
    Requests without API Key might be blocked because of the public rate limiting.
    A request is retried if a "Too Many Requests" status code is returned or if the
    connection is dropped mid-transfer (IncompleteRead / ChunkedEncodingError), which
    can happen for large citation responses when the server closes the socket early.

    This method expects that the caller terminates the call if a time limit is reached.
    """
    headers = {}

    if "API_KEY" in options:
        headers["x-api-key"] = options["API_KEY"]

    while True:
        try:
            response = requests.get(url, headers=headers, timeout=10)
            if response.status_code == 429:
                continue
            response.raise_for_status()
            return response.json()
        except requests.exceptions.ChunkedEncodingError:
            continue


def paper_from_response(res) -> Paper:
    authors = [
        author_from_response(author) for author in safe_get(res, "authors", []) if "name" in author
    ]
    external_id = external_id_from_response(safe_get(res, "externalIds", {}))
    year = int(safe_get(res, "publicationDate", str(safe_get(res, "year", "0000")))[:4])
    publication_type = next(iter(safe_get(res, "publicationTypes", [])), "")

    metadata = {}
    paper_id = res.get("paperId", None)
    if paper_id is not None:
        metadata[id_metadata_key] = paper_id
    corpus_id = res.get("corpusId", None)
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
    first_name, sep, last_name = safe_get(res, "name", "").rpartition(" ")
    return Author(
        first_name,
        last_name,
    )


def external_id_from_response(res) -> Optional[str]:
    # Order of external IDs to retrieve (first match is returned)
    order = ["DOI", "DBLP", "PubMed", "PubMedCentral", "Medline", "MAG", "ArXiv"]
    for key in order:
        if key in res:
            return res.get(key, None)
    return None


fetcher_plugin(
    options,
    search_papers,
    forward_references,
    backward_references,
)
