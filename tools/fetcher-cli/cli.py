#!/usr/bin/env python3

import argparse
import json
import os
import re
import subprocess
import sys
import time
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path
from typing import Any

SCRIPT_DIR = Path(__file__).parent.resolve()
FETCHERS_DIR = (SCRIPT_DIR / "../../src/main/resources/plugins/fetchers").resolve()
OUTPUT_DIR = SCRIPT_DIR / "output"
CONFIG_FILE = SCRIPT_DIR / "config.json"

PYTHON_EXECUTABLE = sys.executable

DEFAULT_TIMEOUT_SECONDS = 30.0
DEFAULT_RETRIES = 0
DEFAULT_RETRY_DELAY_MILLIS = 1000


def load_config() -> dict:
    if not CONFIG_FILE.exists():
        return {}
    try:
        with open(CONFIG_FILE) as f:
            return json.load(f)
    except json.JSONDecodeError:
        print(f"Warning: {CONFIG_FILE} is not valid JSON — ignoring it.", file=sys.stderr)
        return {}


def get_options_for(fetcher: str) -> dict:
    return load_config().get(fetcher, {})


def available_fetchers() -> list[str]:
    return sorted(p.stem for p in FETCHERS_DIR.iterdir() if p.suffix == ".py" and p.is_file())


@dataclass(frozen=True)
class RunSettings:
    """How a plugin process is run: timeout per attempt and retries after the first attempt."""

    timeout_seconds: float
    retries: int
    retry_delay_millis: int


class FetcherRunError(Exception):
    """A failed plugin run that may succeed when it is retried."""


def run_fetcher_once(fetcher_path: Path, action: str, payload: str, timeout: float) -> object:
    env = os.environ.copy()
    env["PYTHONPATH"] = str(FETCHERS_DIR / "lib")

    try:
        result = subprocess.run(
            [PYTHON_EXECUTABLE, str(fetcher_path), action],
            input=payload,
            capture_output=True,
            text=True,
            env=env,
            timeout=timeout,
        )
    except subprocess.TimeoutExpired as ex:
        print_fetcher_stderr(ex.stderr)
        raise FetcherRunError(
            f"fetcher '{fetcher_path.stem}' timed out after {timeout:g}s."
        ) from None

    print_fetcher_stderr(result.stderr)

    if result.returncode != 0:
        raise FetcherRunError(
            f"fetcher '{fetcher_path.stem}' exited with code {result.returncode}."
        )

    output = result.stdout.strip()
    if not output:
        raise FetcherRunError(f"fetcher '{fetcher_path.stem}' returned no output.")

    try:
        return json.loads(output)
    except json.JSONDecodeError:
        raise FetcherRunError(f"fetcher '{fetcher_path.stem}' returned invalid JSON.") from None


def print_fetcher_stderr(stderr: str | bytes | None) -> None:
    if isinstance(stderr, bytes):
        stderr = stderr.decode(errors="replace")
    if stderr and stderr.strip():
        print(f"[fetcher stderr] {stderr.strip()}", file=sys.stderr)


def run_fetcher(fetcher: str, action: str, settings: RunSettings, payload: str = "") -> Any:
    """
    Runs the plugin and returns its parsed JSON output. A run that times out, exits with a non-zero
    code or returns no or invalid JSON is retried up to `settings.retries` times. The delay between
    two attempts starts at `settings.retry_delay_millis` and doubles after each retry, like
    FETCHER_RETRY_DELAY_IN_MILLIS in the backend. Exits with code 1 if the last attempt fails.
    """
    fetcher_path = FETCHERS_DIR / f"{fetcher}.py"
    if not fetcher_path.exists():
        print(f"Error: fetcher '{fetcher}' not found in {FETCHERS_DIR}", file=sys.stderr)
        sys.exit(1)

    attempts = settings.retries + 1
    retry_delay_millis = settings.retry_delay_millis
    for attempt in range(1, attempts):
        try:
            return run_fetcher_once(fetcher_path, action, payload, settings.timeout_seconds)
        except FetcherRunError as ex:
            print(
                f"Attempt {attempt}/{attempts} failed: {ex} Retrying in {retry_delay_millis} ms.",
                file=sys.stderr,
            )
        time.sleep(retry_delay_millis / 1000)
        retry_delay_millis *= 2

    try:
        return run_fetcher_once(fetcher_path, action, payload, settings.timeout_seconds)
    except FetcherRunError as ex:
        suffix = f" (attempt {attempts}/{attempts})" if attempts > 1 else ""
        print(f"Error: {ex}{suffix}", file=sys.stderr)
        sys.exit(1)


def save_output(action: str, fetcher: str, data: object) -> Path:
    OUTPUT_DIR.mkdir(exist_ok=True)
    timestamp = datetime.now().strftime("%Y-%m-%dT%H-%M-%S")
    path = OUTPUT_DIR / f"{action}_{fetcher}_{timestamp}.json"
    with open(path, "w") as f:
        json.dump(data, f, indent=2)
    return path


def parse_hstore(value: str) -> dict[str, str]:
    """Parse a PostgreSQL HSTORE string into a Python dict."""
    return {
        m.group(1).replace('\\"', '"'): m.group(2).replace('\\"', '"')
        for m in re.finditer(r'"((?:[^"\\]|\\.)*)"\s*=>\s*"((?:[^"\\]|\\.)*)"', value)
    }


def normalize_paper(paper: dict) -> dict:
    """Convert HSTORE-serialized fetcher_metadata to a dict if needed."""
    metadata = paper.get("fetcher_metadata")
    if isinstance(metadata, str):
        paper = {**paper, "fetcher_metadata": parse_hstore(metadata)}
    return paper


def load_paper(paper_arg: str) -> dict:
    path = Path(paper_arg)
    if path.exists():
        with open(path) as f:
            return normalize_paper(json.load(f))
    return normalize_paper(json.loads(paper_arg))


def truncate(text: str, width: int) -> str:
    return text if len(text) <= width else text[: width - 1] + "…"


def format_authors(authors: list) -> str:
    if not authors:
        return "—"
    names = []
    for a in authors[:3]:
        first = a.get("first_name", "")
        last = a.get("last_name", "")
        names.append(f"{first} {last}".strip() or last)
    result = ", ".join(names)
    if len(authors) > 3:
        result += f" +{len(authors) - 3}"
    return result


def format_external_ids(external_ids: list) -> str:
    if not external_ids:
        return "—"
    first = external_ids[0]
    result = f"{first.get('type', '')}:{first.get('value', '')}"
    if len(external_ids) > 1:
        result += f" +{len(external_ids) - 1}"
    return result


def print_papers_table(papers: list[dict]) -> None:
    if not papers:
        print("No papers found.")
        return

    col_title = 52
    col_year = 6
    col_authors = 32
    col_ids = 40

    header = (
        f"  {'#':<4} {'Title':<{col_title}} {'Year':<{col_year}}"
        f" {'Authors':<{col_authors}} {'External IDs':<{col_ids}}"
    )
    print(header)
    print("  " + "-" * (len(header) - 2))
    for i, paper in enumerate(papers):
        title = truncate(paper.get("title", ""), col_title)
        year = str(paper.get("year", ""))
        authors = truncate(format_authors(paper.get("authors", [])), col_authors)
        external_ids = truncate(format_external_ids(paper.get("external_ids", [])), col_ids)
        print(
            f"  {i:<4} {title:<{col_title}} {year:<{col_year}}"
            f" {authors:<{col_authors}} {external_ids:<{col_ids}}"
        )


# --- Subcommands ---


def cmd_list(_args):
    fetchers = available_fetchers()
    if not fetchers:
        print("No fetchers found.")
        return
    print(f"Available fetchers ({len(fetchers)}):")
    for name in fetchers:
        print(f"  {name}")


def cmd_info(args):
    fetcher = args.fetcher
    info: dict = run_fetcher(fetcher, "info", run_settings(args))
    fetcher_config = get_options_for(fetcher)

    print(f"Name:        {info.get('name', fetcher)}")
    print(f"Description: {info.get('description', '')}")
    links = info.get("links", [])
    if links:
        print("Links:")
        for link in links:
            print(f"  {link.get('label', '')}: {link.get('url', '')}")

    options_schema: dict[str, dict] = info.get("options_schema", {})
    if not options_schema:
        print("\nNo configurable options.")
        return

    if not CONFIG_FILE.exists():
        print("\nHint: no config.json found. Run 'init-config' to create one.")

    print("\nOptions:")
    print(f"  {'Key':<28} {'Description':<36} {'Req':<5} {'Secret':<7} {'Configured':<12} Value")
    print(f"  {'-' * 28} {'-' * 36} {'-' * 5} {'-' * 7} {'-' * 12} {'-' * 20}")
    for key, schema in options_schema.items():
        description = truncate(schema.get("description", ""), 36)
        is_required = schema.get("required", False)
        is_secret = schema.get("is_secret", False)
        required = "yes" if is_required else "no"
        secret = "yes" if is_secret else "no"
        value = fetcher_config.get(key)
        default_value = schema.get("default_value")
        if value:
            configured = "yes"
        elif key not in fetcher_config:
            configured = "no"
        else:
            configured = "<empty>" if is_required else "no"
        if is_secret and value:
            display_value = "***"
        elif value:
            display_value = value
        elif default_value is not None:
            display_value = f"{default_value} (default)"
        else:
            display_value = ""
        print(
            f"  {key:<28} {description:<36} {required:<5} {secret:<7} {configured:<12} "
            f"{display_value}"
        )


def cmd_init_config(args):
    if CONFIG_FILE.exists() and not args.overwrite:
        print(
            f"Error: {CONFIG_FILE} already exists. Use --overwrite to replace it.",
            file=sys.stderr,
        )
        sys.exit(1)

    fetchers = available_fetchers()
    if not fetchers:
        print("No fetchers found — nothing to configure.")
        return

    config = {}
    for fetcher in fetchers:
        info: dict = run_fetcher(fetcher, "info", run_settings(args))
        options_schema = info.get("options_schema", {})
        config[fetcher] = dict.fromkeys(options_schema, "")

    with open(CONFIG_FILE, "w") as f:
        json.dump(config, f, indent=2)

    print(f"Created {CONFIG_FILE} with {len(fetchers)} fetcher(s).")
    print("Fill in the option values before running search or reference commands.")


def cmd_search(args):
    fetcher = args.fetcher
    payload = json.dumps({"search_query": args.query, "options": get_options_for(fetcher)})
    start = time.monotonic()
    papers: list[dict] = run_fetcher(fetcher, "query", run_settings(args), payload)
    elapsed = time.monotonic() - start

    print_papers_table(papers)
    saved = save_output("search", fetcher, papers)
    print(f"\n{len(papers)} paper(s) found in {elapsed:.2f}s. Full results saved to: {saved}")


def cmd_references(args, action: str):
    fetcher = args.fetcher
    paper = load_paper(args.paper)
    payload = json.dumps({"paper": paper, "options": get_options_for(fetcher)})
    start = time.monotonic()
    papers: list[dict] = run_fetcher(fetcher, action, run_settings(args), payload)
    elapsed = time.monotonic() - start

    print_papers_table(papers)
    saved = save_output(action, fetcher, papers)
    print(f"\n{len(papers)} paper(s) found in {elapsed:.2f}s. Full results saved to: {saved}")


# --- Entry point ---


def positive_float(value: str) -> float:
    number = float(value)
    if number <= 0:
        raise argparse.ArgumentTypeError(f"must be greater than 0, got {value}")
    return number


def non_negative_int(value: str) -> int:
    number = int(value)
    if number < 0:
        raise argparse.ArgumentTypeError(f"must not be negative, got {value}")
    return number


def plugin_run_options_parser() -> argparse.ArgumentParser:
    """Options shared by all subcommands that run a plugin."""
    parser = argparse.ArgumentParser(add_help=False)
    group = parser.add_argument_group("plugin run options")
    group.add_argument(
        "--timeout",
        type=positive_float,
        default=DEFAULT_TIMEOUT_SECONDS,
        metavar="SECONDS",
        help=f"Timeout of one plugin run in seconds (default: {DEFAULT_TIMEOUT_SECONDS:g}).",
    )
    group.add_argument(
        "--retries",
        type=non_negative_int,
        default=DEFAULT_RETRIES,
        metavar="N",
        help=f"Retries after the first failed attempt (default: {DEFAULT_RETRIES}).",
    )
    group.add_argument(
        "--retry-delay",
        type=non_negative_int,
        default=DEFAULT_RETRY_DELAY_MILLIS,
        metavar="MILLISECONDS",
        help="Delay before the first retry; doubles after each retry "
        f"(default: {DEFAULT_RETRY_DELAY_MILLIS}).",
    )
    return parser


def run_settings(args: argparse.Namespace) -> RunSettings:
    return RunSettings(
        timeout_seconds=args.timeout,
        retries=args.retries,
        retry_delay_millis=args.retry_delay,
    )


def main():
    parser = argparse.ArgumentParser(
        prog="fetcher-cli",
        description="CLI for interacting with SnowballR fetcher plugins.",
    )
    sub = parser.add_subparsers(dest="command", required=True)
    plugin_run_options = [plugin_run_options_parser()]

    sub.add_parser("list", help="List all available fetchers.")

    p_opts = sub.add_parser(
        "info", parents=plugin_run_options, help="Show a fetcher's information and options."
    )
    p_opts.add_argument("fetcher", help="Fetcher name.")

    p_search = sub.add_parser("search", parents=plugin_run_options, help="Search for papers.")
    p_search.add_argument("fetcher", help="Fetcher name.")
    p_search.add_argument("query", help="Search query string.")

    p_fwd = sub.add_parser(
        "forwards", parents=plugin_run_options, help="Fetch papers that cite the given paper."
    )
    p_fwd.add_argument("fetcher", help="Fetcher name.")
    p_fwd.add_argument("paper", help="Paper as a JSON string or path to a JSON file.")

    p_bwd = sub.add_parser(
        "backwards", parents=plugin_run_options, help="Fetch papers cited by the given paper."
    )
    p_bwd.add_argument("fetcher", help="Fetcher name.")
    p_bwd.add_argument("paper", help="Paper as a JSON string or path to a JSON file.")

    p_init = sub.add_parser(
        "init-config",
        parents=plugin_run_options,
        help="Create config.json pre-populated with all fetcher option keys.",
    )
    p_init.add_argument("--overwrite", action="store_true", help="Overwrite existing config.json.")

    args = parser.parse_args()
    {
        "list": cmd_list,
        "info": cmd_info,
        "search": cmd_search,
        "forwards": lambda a: cmd_references(a, "forwards"),
        "backwards": lambda a: cmd_references(a, "backwards"),
        "init-config": cmd_init_config,
    }[args.command](args)


if __name__ == "__main__":
    main()
