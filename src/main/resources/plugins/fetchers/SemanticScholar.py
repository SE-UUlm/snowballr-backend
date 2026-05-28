from snowballr import fetcher_plugin, Paper, Author, safe_get
import requests
import urllib.parse
import sys
from typing import Optional, Any

id_metadata_key: str = "SemanticScholarId"
corpus_id_metadata_key: str = "SemanticScholarCorpusId"

options = {
    "API_KEY": "SemanticScholar API key"
}

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

def get_references(paper: Paper, options: dict[str, str], url_suffix: str, obj_key: str) -> list[Paper]:
    paper_id = paper.fetcher_metadata.get(id_metadata_key)
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
    A request is retried if a "Too Many Requests" status code is returned.

    This method expects that the caller terminates the call if a time limit is reached.
    """
    headers = {}

    if "API_KEY" in options:
        headers["x-api-key"] = options["API_KEY"]

    while True:
        response = requests.get(
            url,
            headers=headers,
        )
        if response.status_code == 429:
            continue
        response.raise_for_status()
        return response.json()

def paper_from_response(res) -> Paper:
    authors = [author_from_response(author) for author in safe_get(res, "authors", []) if "name" in author]
    external_id = external_id_from_response(safe_get(res, "externalIds", {}))
    year = int(safe_get(res, "publicationDate", str(safe_get(res, "year", "0000")))[:4])
    publication_type=next(iter(safe_get(res, "publicationTypes", [])), "")
    return Paper(
        title=safe_get(res, "title", ""),
        external_id=external_id,
        abstract=safe_get(res, "abstract", ""),
        year=year,
        publisher="",
        publication_type=publication_type,
        publication_name=safe_get(res, "venue", ""),
        authors=authors,
        fetcher_metadata={
            id_metadata_key: res["paperId"],
            corpus_id_metadata_key: str(res["corpusId"]),
        },
    )

def author_from_response(res) -> Author:
    first_name, sep, last_name = safe_get(res, "name", "").rpartition(' ')
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
