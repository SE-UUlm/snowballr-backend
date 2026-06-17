import urllib.parse
from typing import Any

import requests
from snowballr import Author, Paper, fetcher_plugin, safe_get

id_metadata_key: str = "OpenAlexId"

options = {"API_KEY": "OpenAlex API key"}

base_url = "https://api.openalex.org"
fields = "id,doi,title,publication_year,type,authorships,primary_location,abstract_inverted_index"


def search_papers(search_query: str, options: dict[str, str]) -> list[Paper]:
    """
    API reference:
    https://developers.openalex.org/api-reference/works/get-a-single-work

    This returns at most 25 papers.
    """
    url = f"{base_url}/works"
    params = {
        "search": urllib.parse.quote_plus(search_query),
        "select": fields,
        "per_page": 25,
    } | _oa_params(options)

    data = request(url, options)

    with open("foo1.json", "w") as file:
        print(data, file=file)

    return list(map(paper_from_response, safe_get(data, "results", [])))


# def forward_references(paper: Paper, options: dict[str, str]) -> list[Paper]:
#     paper_id = paper.fetcher_metadata.get(id_metadata_key)
#     if paper_id is None:
#         return []

#     short_id = short_openalex_id(paper_id)
#     url = f"{base_url}/works?filter=cites:{short_id}&select={fields}"
#     return fetch_all_pages(url, options)


# def backward_references(paper: Paper, options: dict[str, str]) -> list[Paper]:
#     paper_id = paper.fetcher_metadata.get(id_metadata_key)
#     if paper_id is None:
#         return []

#     short_id = short_openalex_id(paper_id)
#     data = request(f"{base_url}/works/{short_id}?select=referenced_works", options)
#     ref_ids = safe_get(data, "referenced_works", [])

#     if not ref_ids:
#         return []

#     return fetch_works_by_ids(ref_ids, options)


# def fetch_works_by_ids(ids: list[str], options: dict[str, str]) -> list[Paper]:
#     papers = []
#     batch_size = 50
#     for i in range(0, len(ids), batch_size):
#         batch = [short_openalex_id(id) for id in ids[i : i + batch_size]]
#         id_filter = "|".join(batch)
#         url = f"{base_url}/works?filter=ids.openalex:{id_filter}&select={fields}&per-page={batch_size}"
#         data = request(url, options)
#         papers += list(map(paper_from_response, safe_get(data, "results", [])))
#     return papers


# def fetch_all_pages(url: str, options: dict[str, str]) -> list[Paper]:
#     papers = []
#     cursor = "*"
#     while cursor:
#         data = request(f"{url}&per-page=200&cursor={cursor}", options)
#         papers += list(map(paper_from_response, safe_get(data, "results", [])))
#         cursor = safe_get(data, "meta", {}).get("next_cursor")
#     return papers


# def request(url: str, options: dict[str, str]) -> dict[str, Any]:
#     params = {}
#     if "API_KEY" in options:
#         params["api_key"] = options["API_KEY"]
#     response = requests.get(url, params=params, timeout=10)
#     response.raise_for_status()
#     return response.json()


# def short_openalex_id(openalex_id: str) -> str:
#     return openalex_id.rsplit("/", 1)[-1]


# def paper_from_response(res) -> Paper:
#     doi = safe_get(res, "doi", None)
#     if doi is not None:
#         doi = doi.removeprefix("https://doi.org/")

#     year = safe_get(res, "publication_year", 0)

#     metadata = {}
#     paper_id = res.get("id")
#     if paper_id is not None:
#         paper_id = paper_id.removeprefix("https://openalex.org/")
#         metadata[id_metadata_key] = paper_id

#     authors = [
#         author_from_response(authorship["author"])
#         for authorship in safe_get(res, "authorships", [])
#         if "author" in authorship and "display_name" in authorship["author"]
#     ]

#     source = safe_get(safe_get(res, "primary_location", {}), "source", {})

#     inverted_index = safe_get(res, "abstract_inverted_index", {})
#     abstract = abstract_from_inverted_index(inverted_index)

#     return Paper(
#         title=safe_get(res, "title", ""),
#         external_id=doi,
#         abstract=abstract,
#         year=year if year is not None else 0,
#         publication_type=safe_get(source, "type", safe_get(res, "type", "")),
#         publication_name=safe_get(source, "display_name", ""),
#         publisher=safe_get(source, "host_organization_name", ""),
#         authors=authors,
#         fetcher_metadata=metadata,
#     )


# def abstract_from_inverted_index(index: dict) -> str:
#     pairs = [(pos, word) for word, positions in index.items() for pos in positions]
#     return " ".join(word for _, word in sorted(pairs))


# def author_from_response(res) -> Author:
#     first_name, sep, last_name = safe_get(res, "display_name", "").rpartition(" ")
#     return Author(first_name, last_name)

def _oa_params(options: dict[str, str]) -> tuple[dict[str, str]]:
    params = {}

    if "API_KEY" in options:
        params["api_key"] = options["API_KEY"]

    return params

fetcher_plugin(
    options,
    search_papers,
    forward_references,
    backward_references,
)
