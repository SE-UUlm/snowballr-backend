import json
import random
import sys
import time
from dataclasses import dataclass, field
from datetime import datetime, timezone
from email.utils import parsedate_to_datetime
from enum import StrEnum
from typing import Any, Callable, Iterator, Optional
from urllib.parse import quote, quote_plus

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
class ExternalId(JSONWizard):
    class _(JSONWizard.Meta):
        key_transform_with_load = "SNAKE"
        key_transform_with_dump = "SNAKE"

    type: str = ""
    value: str = ""


@dataclass(unsafe_hash=True)
class Paper(JSONWizard):
    class _(JSONWizard.Meta):
        marshal_date_time_as = "Timestamp"
        key_transform_with_load = "SNAKE"
        key_transform_with_dump = "SNAKE"

    title: str = ""
    external_ids: list[ExternalId] = field(default_factory=list)
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


def _resolve_single_paper(paper_arg: Paper | list[Paper]) -> Paper:
    if isinstance(paper_arg, list):
        if len(paper_arg) != 1:
            msg = f"Expected exactly one paper, got {len(paper_arg)}."
            print(msg, file=sys.stderr)
            exit(1)
        return paper_arg[0]
    return paper_arg


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
                paper_arg = _resolve_single_paper(Paper.from_json(sys.argv[2]))
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
                paper_arg = _resolve_single_paper(Paper.from_json(sys.argv[2]))
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
    secret: str = "",
) -> dict[str, Any]:
    """
    Requests the specified url with automated retries.

    If a secret is passed, it is removed from the message of any request error. Some APIs only
    accept an API key as a query parameter, and the exceptions raised by requests contain the full
    URL, e.g. "401 Client Error: Unauthorized for url: ...?api_key=SECRET". Their tracebacks end up
    in stderr and the backend logs. See _redacted_error for details.
    """
    try:
        return _request_with_retry(url, headers, params, timeout_seconds)
    except requests.exceptions.RequestException as e:
        if not secret:
            raise

        # Suppress the exception chain, as the original exception would print the URL again.
        raise _redacted_error(e, secret) from None


def _request_with_retry(
    url: str,
    headers: dict[str, Any],
    params: dict[str, Any],
    timeout_seconds: float,
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

            if _should_retry_request(response):
                if n == max_attempts - 1:
                    response.raise_for_status()

                wait = _retry_after_seconds(response.headers.get("Retry-After"))

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


def _should_retry_request(response: requests.Response) -> bool:
    """
    Determines whether a request should be retried based on the status code of its response.
    Retries are performed for 429 (Too Many Requests) and 504 (Gateway Timeout) status codes.
    """
    return response.status_code in [429, 504]


def _retry_after_seconds(retry_after: Optional[str]) -> Optional[float]:
    """
    Parses a "Retry-After" header, which is either a number of seconds or an HTTP date.
    Returns None if the header is missing or malformed, so the caller falls back to backoff.
    """
    if retry_after is None:
        return None

    try:
        return max(0, int(retry_after))
    except ValueError:
        pass

    try:
        retry_at = parsedate_to_datetime(retry_after)
    except (TypeError, ValueError):
        return None

    if retry_at.tzinfo is None:
        retry_at = retry_at.replace(tzinfo=timezone.utc)

    return max(0.0, (retry_at - datetime.now(timezone.utc)).total_seconds())


def _exp_backoff(timeout_seconds: float, max_timeout: float, attempt: int) -> float:
    return random.uniform(0, min(max_timeout, timeout_seconds * 2**attempt))


def paginate_with_retry(
    first_url: str,
    next_url: Callable[[dict[str, Any]], Optional[str]],
    headers: dict[str, Any] = {},
    params: dict[str, Any] = {},
    timeout_seconds: float = 0.0,
    secret: str = "",
) -> Iterator[dict[str, Any]]:
    """
    Paginates through a resource by repeatedly calling request_with_retry. The secret is passed on
    to request_with_retry.

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
        response = request_with_retry(url, headers, params, timeout_seconds, secret)
        yield response
        url = next_url(response)


def _redacted_error(
    error: requests.exceptions.RequestException, secret: str
) -> requests.exceptions.RequestException:
    """
    Returns an error of the same type with any occurrence of the secret, plain or URL-encoded,
    removed from its message. The response and request stay attached, so callers can still check
    the status code. Note that they still contain the URL with the secret, so never log them.

    For an HTTPError, the start of the response body is added to the message, because it tells
    apart errors with the same status code, e.g. an exceeded quota and an invalid key both return
    403.
    """
    message = _redact(str(error), secret)

    response = error.response
    if isinstance(error, requests.exceptions.HTTPError) and response is not None:
        body = " ".join(response.text.split())[:_max_error_body_length]
        message = f"{message}: {_redact(body, secret)}"

    try:
        return type(error)(message, response=response, request=error.request)
    except TypeError:
        # Some subclasses, e.g. JSONDecodeError, have a different constructor.
        return requests.exceptions.RequestException(
            message, response=response, request=error.request
        )


# Number of characters of an error response body included in a redacted error message.
_max_error_body_length = 300


def _redact(text: str, secret: str) -> str:
    if not secret:
        return text

    for variant in {secret, quote(secret, safe=""), quote_plus(secret)}:
        text = text.replace(variant, "<redacted>")

    return text
