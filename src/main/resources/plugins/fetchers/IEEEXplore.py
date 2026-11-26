from typing import Any, Optional

from requests import HTTPError
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
    name="IEEE Xplore",
    description=(
        "IEEE Xplore is IEEE's digital library, covering over 6 million documents including "
        "journals, conference proceedings, standards, and books across electrical engineering, "
        "computer science, and related fields. An API key is required.\n\n"
        "Supports paper search and forward references (papers that cite a paper). "
        "Backward references are not available through the IEEE Xplore API."
    ),
    links=[
        Link("Homepage", "https://ieeexplore.ieee.org/"),
        Link("Register for an account", "https://developer.ieee.org/member/register"),
    ],
    options_schema={
        "API_KEY": FetcherOptionsSchema(
            name="API Key",
            description="IEEE Xplore API key",
            required=True,
            is_secret=True,
        ),
    },
)

base_url = "https://ieeexploreapi.ieee.org/api/v1"

# IEEE Xplore allows 10 calls per second by default.
request_interval_seconds = 0.1

# Maximum page size the API accepts.
max_records = 200

# Number of articles looked up with a single boolean search.
lookup_batch_size = 100


def search_papers(search_query: str, options: dict[str, str]) -> list[Paper]:
    """
    API reference:
    https://developer.ieee.org/docs/read/Metadata_API_details

    This returns at most 25 papers, sorted by relevance.
    """
    params = {"querytext": search_query, "max_records": 25} | _ieee_params(options)
    data = _request(f"{base_url}/search/articles", params)

    return list(map(_paper_from_response, safe_get(data, "articles", [])))


def backward_references(paper: Paper, options: dict[str, str]) -> list[Paper]:
    return []


def forward_references(paper: Paper, options: dict[str, str]) -> list[Paper]:
    article_number = _article_number(paper)
    if article_number is None:
        return []

    citing_numbers = _citing_article_numbers(article_number, options)

    papers = []
    for start in range(0, len(citing_numbers), lookup_batch_size):
        papers += _articles_by_numbers(citing_numbers[start : start + lookup_batch_size], options)

    return papers


def _article_number(paper: Paper) -> Optional[str]:
    for external_id in paper.external_ids:
        if external_id.type == "IEEE_XPLORE":
            return external_id.value

    return None


def _citing_article_numbers(article_number: str, options: dict[str, str]) -> list[str]:
    """
    Collects the article numbers of all IEEE papers citing the given article, following the
    pagination of the citation endpoint. The citations only contain a formatted citation string,
    so the papers have to be looked up separately.
    """
    url = f"{base_url}/search/document/{article_number}/citation"
    params = {"type": "ieee", "max_records": max_records} | _ieee_params(options)
    received = 0

    def next_url(data: dict[str, Any]) -> Optional[str]:
        # The response does not echo start_record, so track the position here.
        nonlocal received
        citations = safe_get(data, "ieee_citation", [])
        received += len(citations)

        # Requesting a start_record past the last citation returns a 500, so stop at the total
        # count instead of relying on a short last page.
        total = int(safe_get(data, "ieee_citation_count", 0))
        if len(citations) < max_records or received >= total:
            return None

        return f"{url}?start_record={received + 1}"

    def fetch_pages() -> list[dict[str, Any]]:
        pages = []
        try:
            for page in paginate_with_retry(
                url, next_url, {}, params, request_interval_seconds, options["API_KEY"]
            ):
                pages.append(page)
        except HTTPError as e:
            # The API answers an article without IEEE citations with a 500 and an empty JSON
            # object instead of an empty list. Only accept this for the first page, so a failure
            # in the middle of the pagination is still reported.
            if not pages and _is_no_citations_response(e):
                return []
            raise

        return pages

    numbers = []
    for page in fetch_pages():
        for citation in safe_get(page, "ieee_citation", []):
            number = safe_get(safe_get(citation, "links", {}), "articleNumber", None)
            if number is not None:
                numbers.append(str(number))

    return numbers


def _articles_by_numbers(article_numbers: list[str], options: dict[str, str]) -> list[Paper]:
    """
    Looks up several articles with a single boolean search instead of one request per article.
    """
    query = " OR ".join(f'"Article Number":{number}' for number in article_numbers)
    params = {"querytext": query, "max_records": len(article_numbers)} | _ieee_params(options)
    data = _request(f"{base_url}/search/articles", params)

    return list(map(_paper_from_response, safe_get(data, "articles", [])))


def _paper_from_response(res: dict[str, Any]) -> Paper:
    authors = [
        _author_from_response(author)
        for author in safe_get(safe_get(res, "authors", {}), "authors", [])
        if safe_get(author, "full_name", None) is not None
    ]

    article_number = str(res["article_number"])
    external_ids = [ExternalId("IEEE_XPLORE", article_number)]
    if (doi := res.get("doi")) is not None:
        external_ids.append(ExternalId("DOI", doi))

    return Paper(
        title=safe_get(res, "title", ""),
        external_ids=external_ids,
        abstract=safe_get(res, "abstract", ""),
        year=safe_get(res, "publication_year", 0),
        publisher=safe_get(res, "publisher", ""),
        publication_type=safe_get(res, "content_type", ""),
        publication_name=safe_get(res, "publication_title", ""),
        authors=authors,
    )


def _author_from_response(res: dict[str, Any]) -> Author:
    first_name, _, last_name = safe_get(res, "full_name", "").rpartition(" ")
    return Author(first_name, last_name)


def _request(url: str, params: dict[str, Any]) -> dict[str, Any]:
    # IEEE Xplore only accepts the API key as a query parameter, so keep it out of error messages.
    return request_with_retry(url, {}, params, request_interval_seconds, params["apikey"])


def _is_no_citations_response(error: HTTPError) -> bool:
    response = error.response
    return response is not None and response.status_code == 500 and response.text.strip() == "{}"


def _ieee_params(options: dict[str, str]) -> dict[str, str]:
    return {"apikey": options["API_KEY"], "format": "json"}


fetcher_plugin(
    information=fetcher_information,
    query=search_papers,
    forwards=forward_references,
    backwards=backward_references,
)
