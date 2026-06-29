import json
import random
import sys
import time
from dataclasses import dataclass, field
from enum import StrEnum
from typing import Any, Callable, Iterator, Optional

import requests
from dataclass_wizard import JSONWizard, fromdict


@dataclass(unsafe_hash=True)
class Author(JSONWizard):
    class _(JSONWizard.Meta):
        key_transform_with_load = "SNAKE"
        key_transform_with_dump = "SNAKE"

    first_name: str = ""
    last_name: str = ""


@dataclass(unsafe_hash=True)
class Paper(JSONWizard):
    class _(JSONWizard.Meta):
        marshal_date_time_as = "Timestamp"
        key_transform_with_load = "SNAKE"
        key_transform_with_dump = "SNAKE"

    title: str = ""
    external_id: Optional[str] = None
    abstract: str = ""
    year: int = 0
    publisher: str = ""
    publication_type: str = ""
    publication_name: str = ""
    authors: list[Author] = field(default_factory=list)
    fetcher_metadata: dict[str, str] = field(default_factory=dict)


@dataclass(unsafe_hash=True)
class Link(JSONWizard):
    class _(JSONWizard.Meta):
        key_transform_with_load = "SNAKE"
        key_transform_with_dump = "SNAKE"

    label: str = ""
    url: str = ""


@dataclass(unsafe_hash=True)
class FetcherOptionsSchema(JSONWizard):
    class _(JSONWizard.Meta):
        key_transform_with_load = "SNAKE"
        key_transform_with_dump = "SNAKE"

    name: str = ""
    description: str = ""
    required: bool = False
    is_secret: bool = False
    default_value: Optional[str] = None


@dataclass(unsafe_hash=True)
class FetcherInformation(JSONWizard):
    class _(JSONWizard.Meta):
        key_transform_with_load = "SNAKE"
        key_transform_with_dump = "SNAKE"

    name: str = ""
    description: str = ""
    links: list[Link] = field(default_factory=list)
    options_schema: dict[str, FetcherOptionsSchema] = field(default_factory=dict)


class EventType(StrEnum):
    INFO = "info"
    QUERY = "query"
    FORWARDS = "forwards"
    BACKWARDS = "backwards"


type Options = dict[str, str]
type QueryFn = Callable[[str, Options], list[Paper]]
type ReferenceFn = Callable[[Paper, Options], list[Paper]]


def _read_stdin_payload() -> dict:
    if sys.stdin is None or sys.stdin.closed:
        return {}
    if sys.stdin.isatty():
        return {}

    try:
        payload = sys.stdin.read()
    except OSError:
        return {}

    if payload == "":
        return {}

    return json.loads(payload)


def _apply_defaults(options: Options, schema: dict[str, FetcherOptionsSchema]) -> Options:
    result = dict(options)
    for key, option_schema in schema.items():
        if (key not in result or result[key] == "") and option_schema.default_value is not None:
            result[key] = option_schema.default_value
    return result


def fetcher_plugin(
    information: FetcherInformation,
    query: QueryFn,
    forwards: ReferenceFn,
    backwards: ReferenceFn,
):
    if len(sys.argv) < 2:
        print("The fetcher was called without an action.", file=sys.stderr)
        print("python fetcher.py <ACTION> <...ARGS>", file=sys.stderr)
        exit(1)

    payload = _read_stdin_payload()

    match sys.argv[1]:
        case EventType.INFO:
            print(information.to_json())

        case EventType.QUERY:
            if payload:
                query_arg: str = payload["search_query"]
                options_arg: Options = payload.get("options", {})
            elif len(sys.argv) == 4:
                query_arg = sys.argv[2]
                options_arg = json.loads(sys.argv[3])
            else:
                msg = "The fetcher was called with an incorrect number of arguments."
                print(msg, file=sys.stderr)
                print("python fetcher.py query <SEARCH_QUERY> <OPTIONS>", file=sys.stderr)
                exit(1)

            options_arg = _apply_defaults(options_arg, information.options_schema)
            result = query(query_arg, options_arg)
            print(Paper.list_to_json(result))

        case EventType.FORWARDS:
            if payload:
                paper_arg: Paper = fromdict(Paper, payload["paper"])
                options_arg: Options = payload.get("options", {})
            elif len(sys.argv) == 4:
                paper_arg = Paper.from_json(sys.argv[2])
                options_arg = json.loads(sys.argv[3])
            else:
                msg = "The fetcher was called with an incorrect number of arguments."
                print(msg, file=sys.stderr)
                print("python fetcher.py forwards <PAPER> <OPTIONS>", file=sys.stderr)
                exit(1)

            options_arg = _apply_defaults(options_arg, information.options_schema)
            result = forwards(paper_arg, options_arg)
            print(Paper.list_to_json(result))

        case EventType.BACKWARDS:
            if payload:
                paper_arg: Paper = fromdict(Paper, payload["paper"])
                options_arg: Options = payload.get("options", {})
            elif len(sys.argv) == 4:
                paper_arg = Paper.from_json(sys.argv[2])
                options_arg = json.loads(sys.argv[3])
            else:
                msg = "The fetcher was called with an incorrect number of arguments."
                print(msg, file=sys.stderr)
                print("python fetcher.py backwards <PAPER> <OPTIONS>", file=sys.stderr)
                exit(1)

            options_arg = _apply_defaults(options_arg, information.options_schema)
            result = backwards(paper_arg, options_arg)
            print(Paper.list_to_json(result))

        case _:
            print("Unknown fetcher action.", file=sys.stderr)
            exit(1)


def safe_get(res: dict, key: str, default: Any):
    """
    Retrieves a value from the passed dictionary using the specified key.
    If the key doesn't exist or the value of the entry is None, the provided default value is
    returned.
    """
    val = res.get(key)
    if val is None:
        return default
    else:
        return val


def request_with_retry(
    url: str,
    headers: dict[str, Any] = {},
    params: dict[str, Any] = {},
    timeout_seconds: float = 0.0,
) -> dict[str, Any]:
    """
    Requests the specified url with automated retries.

    If a "Too Many Requests" status code is returned, the request is retried after a certain
    timeout. The timeout is defined in the following order:
    - by the "Retry-After" header
    - by the "timeout_seconds" parameter with exponential backoff and jitter

    If timeout_seconds is set to 0, no backoff is applied and the requests are fired constantly.
    Use this with caution.

    A request is also retried if the connection is dropped mid-transfer (ChunkedEncodingError),
    which can happen for large responses when the server closes the socket early.

    The request has a timeout of 10 seconds.

    The maximum number of attempts is 10 and the maximum timeout between requests is 60 seconds.
    """
    max_attempts = 10
    max_timeout = 60

    for n in range(max_attempts):
        try:
            response = requests.get(url, headers=headers, params=params, timeout=10)

            if response.status_code == 429:
                if n == max_attempts - 1:
                    response.raise_for_status()

                retry_after = response.headers.get("Retry-After")
                wait = int(retry_after) if retry_after is not None else None

                if wait is None:
                    wait = _exp_backoff(timeout_seconds, max_timeout, n)

                if wait > 0:
                    time.sleep(wait)
                continue

            response.raise_for_status()
            return response.json()
        except requests.exceptions.ChunkedEncodingError:
            time.sleep(_exp_backoff(timeout_seconds, max_timeout, n))
            continue

    raise RuntimeError(
        f"Failed to fetch the requested resource '{url}' after {max_attempts} attempts"
    )


def _exp_backoff(timeout_seconds: float, max_timeout: float, attempt: int) -> float:
    return random.uniform(0, min(max_timeout, timeout_seconds * 2**attempt))


def paginate_with_retry(
    first_url: str,
    next_url: Callable[[dict[str, Any]], Optional[str]],
    headers: dict[str, Any] = {},
    params: dict[str, Any] = {},
    timeout_seconds: float = 0.0,
) -> Iterator[dict[str, Any]]:
    """
    Paginates through a resource by repeatedly calling request_with_retry.

    Before each request, sleeps the remaining time since the last call to respect the
    timeout_seconds interval, avoiding 429s proactively. request_with_retry still handles any 429s
    reactively as a safety net.

    Pagination stops when next_url returns None.
    """
    url: Optional[str] = first_url
    last_call = 0.0

    while url is not None:
        elapsed = time.monotonic() - last_call
        if elapsed < timeout_seconds:
            time.sleep(timeout_seconds - elapsed)

        last_call = time.monotonic()
        response = request_with_retry(url, headers, params, timeout_seconds)
        yield response
        url = next_url(response)
