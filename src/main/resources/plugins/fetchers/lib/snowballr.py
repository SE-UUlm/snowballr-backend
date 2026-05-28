import json
import sys
from dataclasses import dataclass, field
from enum import StrEnum
from typing import Callable, Optional

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
    year: int = 1970
    publisher: str = ""
    publication_type: str = ""
    publication_name: str = ""
    authors: list[Author] = field(default_factory=list)
    fetcher_metadata: dict[str, str] = field(default_factory=dict)


class EventType(StrEnum):
    OPTIONS = "options"
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


def fetcher_plugin(
    options: dict[str, str],
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
        case EventType.OPTIONS:
            print(json.dumps(options))

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

            result = backwards(paper_arg, options_arg)
            print(Paper.list_to_json(result))

        case _:
            print("Unknown fetcher action.", file=sys.stderr)
            exit(1)
