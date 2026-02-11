# THIS FILE IS AUTO-GENERATED. DO NOT MODIFY.

import sys
from enum import StrEnum
from dataclasses import dataclass, field
import json
from dataclass_wizard import asdict, fromdict, JSONWizard
from typing import Callable, Optional
from datetime import datetime

@dataclass(unsafe_hash=True)
class Author(JSONWizard):
    class _(JSONWizard.Meta):
        key_transform_with_load = 'SNAKE'
        key_transform_with_dump = 'SNAKE'

    first_name: str = ""
    last_name: str = ""

@dataclass(unsafe_hash=True)
class Paper(JSONWizard):
    class _(JSONWizard.Meta):
        marshal_date_time_as = 'Timestamp'
        key_transform_with_load = 'SNAKE'
        key_transform_with_dump = 'SNAKE'

    title: str = ""
    external_id: Optional[str] = None
    abstract: str = ""
    year: int = 1970
    publisher: str = ""
    publication_type: str = ""
    publication_name: str = ""
    authors: list[Author] = field(default_factory=list)
    metadata: dict[str, str] = field(default_factory=dict)

class EventType(StrEnum):
    OPTIONS = "options"
    QUERY = "query"
    FORWARDS = "forwards"
    BACKWARDS = "backwards"

type Options = dict[str, str]
type QueryFn = Callable[[str, Options], list[Paper]]
type ReferenceFn = Callable[[Paper, Options], list[Paper]]

def fetcher_plugin(
    options: dict[str, str],
    query: QueryFn,
    forwards: ReferenceFn,
    backwards: ReferenceFn,
):
    if len(sys.argv) < 2:
        print("TOO FEW ARGS")
        exit(1)

    match sys.argv[1]:
        case EventType.OPTIONS:
            print(json.dumps(options))

        case EventType.QUERY:
            if len(sys.argv) < 4:
                print("TOO FEW ARGS")
                exit(1)

            options: Options = json.loads(sys.argv[3])
            result = query(sys.argv[2], options)
            print(Paper.list_to_json(result))

        case EventType.FORWARDS:
            if len(sys.argv) < 4:
                print("TOO FEW ARGS")
                exit(1)

            paper: Paper = Paper.from_json(sys.argv[2])
            options: Options = json.loads(sys.argv[3])
            result = forwards(paper, options)
            print(Paper.list_to_json(result))

        case EventType.BACKWARDS:
            if len(sys.argv) < 4:
                print("TOO FEW ARGS")
                exit(1)

            paper: Paper = Paper.from_json(sys.argv[2])
            options: Options = json.loads(sys.argv[3])
            result = backwards(paper, options)
            print(Paper.list_to_json(result))

        case _:
            print("ERROR")
