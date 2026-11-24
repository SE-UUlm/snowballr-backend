from typing import Any, Callable, Optional
from urllib.parse import quote

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
    name="OpenAlex",
    description=(
        "OpenAlex is a free and open catalog of the global research system, indexing scholarly "
        "works, authors, and sources across all research fields. Supports paper search, forward "
        "references, and backward references.\n\n"
        "Without an API key, requests share a small free daily budget per IP address, and "
        "references are limited to the first 10,000 results. With a free API key, each user gets "
        "their own daily budget and all references are retrieved."
    ),
    links=[
        Link("Homepage", "https://openalex.org/"),
        Link("Request API Key", "https://openalex.org/settings/api"),
    ],
    options_schema={
        "API_KEY": FetcherOptionsSchema(
            name="API Key",
            description="OpenAlex API key",
            required=False,
            is_secret=True,
        )
    },
)

id_metadata_key: str = "OpenAlexId"

base_url = "https://api.openalex.org"
fields = "id,ids,title,publication_year,authorships,primary_location,abstract_inverted_index"

# Requests per second OpenAlex allows without and with a (free) API key.
requests_per_second_without_key = 10
requests_per_second_with_key = 30


def search_papers(search_query: str, options: dict[str, str]) -> list[Paper]:
    """
    API reference:
    https://help.openalex.org/api/searching/

    This returns at most 25 papers.
    """
    url = f"{base_url}/works"
    params = {
        "search": search_query,
        "select": fields,
        "per_page": 25,
    }
    headers = _oa_headers(options)

    data = request_with_retry(url, headers, params, _request_interval(options), _api_key(options))
    papers = safe_get(data, "results", [])

    return list(map(_paper_from_response, papers))


def forward_references(paper: Paper, options: dict[str, str]) -> list[Paper]:
    paper_id = _construct_paper_id(paper, options)
    if paper_id is None:
        return []

    ref_filter = f"filter=cites:{paper_id}"

    return _fetch_all_pages(ref_filter, options)


def backward_references(paper: Paper, options: dict[str, str]) -> list[Paper]:
    paper_id = _construct_paper_id(paper, options)
    if paper_id is None:
        return []

    ref_filter = f"filter=cited_by:{paper_id}"

    return _fetch_all_pages(ref_filter, options)


def _construct_paper_id(paper: Paper, options: dict[str, str]) -> Optional[str]:
    paper_id = paper.fetcher_metadata.get(id_metadata_key)
    if paper_id is not None:
        return _short_openalex_id(paper_id)

    # Retrieve paper by other external IDs to get the OpenAlex ID
    prefixes = {
        "DOI": "doi",
        "PUB_MED": "pmid",
        "PUB_MED_CENTRAL": "pmcid",
        "MAG": "mag",
    }
    for external_id in paper.external_ids:
        prefix = prefixes.get(external_id.type)
        if prefix is None:
            continue

        url = f"{base_url}/works/{prefix}:{quote(external_id.value, safe='/')}"
        params = {"select": "id"}
        headers = _oa_headers(options)

        try:
            data = request_with_retry(
                url, headers, params, _request_interval(options), _api_key(options)
            )
        except HTTPError as e:
            if e.response is not None and e.response.status_code == 404:
                continue
            raise

        paper_id = data.get("id")
        if paper_id is not None:
            return _short_openalex_id(paper_id)

    return None


def _fetch_all_pages(specific_param: str, options: dict[str, str]) -> list[Paper]:
    """
    Fetch all pages of results from the OpenAlex API, handling pagination based on whether an API
    key is provided. If an API key is provided, use cursor-based pagination (paid API). Otherwise,
    use page number-based pagination (free API).
    API reference: https://help.openalex.org/api/paging/
    """
    headers = _oa_headers(options)
    per_page = 100
    ref_url = f"{base_url}/works?{specific_param}&select={fields}&per_page={per_page}"

    if _has_api_key(options):
        next_url = _fetch_all_pages_paid(ref_url)
        ref_url = f"{ref_url}&cursor=*"
    else:
        next_url = _fetch_all_pages_free(ref_url, per_page)

    interval = _request_interval(options)
    papers = []
    for page in paginate_with_retry(ref_url, next_url, headers, {}, interval, _api_key(options)):
        papers += safe_get(page, "results", [])

    return list(map(_paper_from_response, papers))


def _fetch_all_pages_free(ref_url: str, per_page: int) -> Callable[[dict[str, Any]], Optional[str]]:
    """
    Pagination logic for free API, which has a limit of 10k results and uses page numbers instead of
    cursors.
    API reference: https://help.openalex.org/api/paging/
    """

    def next_url(data: dict[str, Any]) -> Optional[str]:
        meta = safe_get(data, "meta", {})
        page_number = meta.get("page")

        if page_number is None:
            return None

        count = safe_get(meta, "count", 0)
        reached_limit = (page_number + 1) * per_page > 10_000
        reached_pages_end = page_number * per_page >= count

        # Stop at 10k limit or at end of pagination
        if reached_limit or reached_pages_end:
            return None

        page_number += 1

        return f"{ref_url}&page={page_number}"

    return next_url


def _fetch_all_pages_paid(ref_url: str) -> Callable[[dict[str, Any]], Optional[str]]:
    """
    Pagination logic for paid API, which uses cursors instead of page numbers and has no limit on
    results.
    API reference: https://help.openalex.org/api/paging/
    """

    def next_url(data: dict[str, Any]) -> Optional[str]:
        meta = safe_get(data, "meta", {})
        cursor = meta.get("next_cursor")

        if not cursor:
            return None

        # The cursor is base64 and may contain "+", which a query string would read as a space.
        return f"{ref_url}&cursor={quote(cursor, safe='')}"

    return next_url


def _short_openalex_id(openalex_id: str) -> str:
    return openalex_id.removeprefix("https://openalex.org/")


def _paper_from_response(res) -> Paper:
    external_ids = _external_ids_from_response(res)

    metadata = {}
    if (paper_id := res.get("id")) is not None:
        metadata[id_metadata_key] = _short_openalex_id(paper_id)

    authors = [
        _author_from_response(authorship["author"])
        for authorship in safe_get(res, "authorships", [])
        if safe_get(safe_get(authorship, "author", {}), "display_name", None) is not None
    ]

    source = safe_get(safe_get(res, "primary_location", {}), "source", {})
    inverted_index = safe_get(res, "abstract_inverted_index", {})

    return Paper(
        title=safe_get(res, "title", ""),
        external_ids=external_ids,
        abstract=_abstract_from_inverted_index(inverted_index),
        year=safe_get(res, "publication_year", 0),
        publication_type=safe_get(source, "type", ""),
        publication_name=safe_get(source, "display_name", ""),
        publisher=safe_get(source, "host_organization_name", ""),
        authors=authors,
        fetcher_metadata=metadata,
    )


def _abstract_from_inverted_index(index: dict) -> str:
    pairs = [(pos, word) for word, positions in index.items() for pos in positions]
    return " ".join(word for _, word in sorted(pairs))


def _author_from_response(res) -> Author:
    first_name, _, last_name = safe_get(res, "display_name", "").rpartition(" ")
    return Author(first_name, last_name)


def _external_ids_from_response(res) -> list[ExternalId]:
    """
    API reference:
    https://help.openalex.org/data/works/attributes/#ids
    """
    ids = safe_get(res, "ids", {})

    # Map the response key to the target ExternalId type string, the prefix to remove,
    # and the prefix to add, matching our validation.
    key_mapping = {
        "doi": ("DOI", "https://doi.org/", ""),
        "mag": ("MAG", "", ""),
        "pmid": ("PUB_MED", "https://pubmed.ncbi.nlm.nih.gov/", ""),
        "pmcid": (
            # PubMedCentral requires 'PMC' prefix
            "PUB_MED_CENTRAL",
            "https://www.ncbi.nlm.nih.gov/pmc/articles/",
            "PMC",
        ),
    }

    return [
        ExternalId(id_type, f"{prefix_add}{ids[key].removeprefix(prefix_remove)}")
        for key, (id_type, prefix_remove, prefix_add) in key_mapping.items()
        if key in ids and ids[key] is not None
    ]


def _oa_headers(options: dict[str, str]) -> dict[str, str]:
    headers = {}

    if _has_api_key(options):
        headers["Authorization"] = f"Bearer {options['API_KEY']}"

    return headers


def _request_interval(options: dict[str, str]) -> float:
    """
    Minimum number of seconds between two requests to stay within the rate limit. Also used as the
    base of the exponential backoff when a request is rate limited.
    """
    if _has_api_key(options):
        return 1 / requests_per_second_with_key

    return 1 / requests_per_second_without_key


def _api_key(options: dict[str, str]) -> str:
    # The API key is sent as a query parameter, so it is passed on to be kept out of error messages.
    return options.get("API_KEY", "")


def _has_api_key(options: dict[str, str]) -> bool:
    return _api_key(options) != ""


fetcher_plugin(
    information=fetcher_information,
    query=search_papers,
    forwards=forward_references,
    backwards=backward_references,
)
