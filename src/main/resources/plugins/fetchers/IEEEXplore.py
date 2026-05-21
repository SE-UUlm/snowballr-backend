# THIS FILE IS AUTO-GENERATED. DO NOT MODIFY.

from snowballr import fetcher_plugin, Paper, Author
from xploreapi import XPLORE
from datetime import datetime

metadata_key: str = "IEEEXploreId"

options: dict[str, str] = {
    "API_KEY": "IEEEXplore API key"
}

def search_papers(search_query: str, options: dict[str, str]) -> list[Paper]:
    query = XPLORE(options["API_KEY"])
    query.queryText(search_query)
    query.dataType("json")
    query.dataFormat("object")
    results = query.callAPI()
    articles = results.get("articles", [])
    return list(map(paper_from_response, articles))

def backward_references(paper: Paper, options: dict[str, str]) -> list[Paper]:
    return []

def forward_references(paper: Paper, options: dict[str, str]) -> list[Paper]:
    article_number = paper.metadata.get(metadata_key)
    if article_number is None:
        return []

    query = XPLORE(options["API_KEY"])
    query.citations(article_number, "ieee")
    query.dataType("json")
    query.dataFormat("object")
    results = query.callAPI()

    references = []

    for result in results.get("ieee_citation", []):
        query = XPLORE(options["API_KEY"])

        if result.get("links", {}).get("articleNumber") is None:
            continue

        query.articleNumber(result["links"]["articleNumber"])
        query.dataType("json")
        query.dataFormat("object")
        results = query.callAPI()
        references.append(paper_from_response(results["articles"][0]))

    return references

def paper_from_response(res) -> Paper:
    authors = [author_from_response(author) for author in res.get("authors", {}).get("authors", []) if "full_name" in author]
    # Prefer publication year over insert year
    date = res.get("publication_year", int(res.get("insert_date", "1970")[:4]))
    return Paper(
        res.get("title", ""),
        res.get("doi", None),
        res.get("abstract", ""),
        date,
        res.get("publisher", ""),
        res.get("content_type", ""),
        res.get("publication_title", ""),
        authors,
        { metadata_key: res["article_number"] },
    )

def author_from_response(res) -> Author:
    first_name, sep, last_name = res.get("full_name", "").rpartition(' ')
    return Author(
        first_name,
        last_name,
    )

fetcher_plugin(
    options,
    search_papers,
    forward_references,
    backward_references,
)
