from snowballr import Author, FetcherInformation, FetcherOptionsSchema, Link, Paper, fetcher_plugin
from xploreapi import XPLORE

fetcher_information = FetcherInformation(
    name="IEEE Xplore",
    description="Fetcher for retrieving papers from IEEEXplore",
    links=[
        Link("Homepage", "https://ieeexplore.ieee.org/"),
        Link("Register for an account", "https://developer.ieee.org/member/register"),
    ],
    options_schema={
        "API_KEY": FetcherOptionsSchema(
            description="IEEEXplore API key",
            required=True,
            is_secret=True,
        ),
    },
)

metadata_key: str = "IEEEXploreId"


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
    article_number = paper.fetcher_metadata.get(metadata_key)
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
    authors = [
        author_from_response(author)
        for author in res.get("authors", {}).get("authors", [])
        if "full_name" in author
    ]
    # Prefer publication year over insert year
    date = res.get("publication_year", int(res.get("insert_date", "0000")[:4]))
    return Paper(
        title=res.get("title", ""),
        external_id=res.get("doi", None),
        abstract=res.get("abstract", ""),
        year=date,
        publisher=res.get("publisher", ""),
        publication_type=res.get("content_type", ""),
        publication_name=res.get("publication_title", ""),
        authors=authors,
        fetcher_metadata={metadata_key: res["article_number"]},
    )


def author_from_response(res) -> Author:
    first_name, _, last_name = res.get("full_name", "").rpartition(" ")
    return Author(
        first_name,
        last_name,
    )


fetcher_plugin(
    information=fetcher_information,
    query=search_papers,
    forwards=forward_references,
    backwards=backward_references,
)
