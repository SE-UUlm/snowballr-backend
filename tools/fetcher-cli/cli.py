#!/usr/bin/env python3

import argparse
import json
import os
import re
import subprocess
import sys
from datetime import datetime
from pathlib import Path

SCRIPT_DIR = Path(__file__).parent.resolve()
FETCHERS_DIR = (SCRIPT_DIR / "../../src/main/resources/plugins/fetchers").resolve()
OUTPUT_DIR = SCRIPT_DIR / "output"
CONFIG_FILE = SCRIPT_DIR / "config.json"

PYTHON_EXECUTABLE = sys.executable


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


def run_fetcher(fetcher: str, action: str, payload: str = "") -> str:
    fetcher_path = FETCHERS_DIR / f"{fetcher}.py"
    if not fetcher_path.exists():
        print(f"Error: fetcher '{fetcher}' not found in {FETCHERS_DIR}", file=sys.stderr)
        sys.exit(1)

    env = os.environ.copy()
    env["PYTHONPATH"] = str(FETCHERS_DIR / "lib")

    result = subprocess.run(
        [PYTHON_EXECUTABLE, str(fetcher_path), action],
        input=payload,
        capture_output=True,
        text=True,
        env=env,
        timeout=30,
    )

    if result.stderr.strip():
        print(f"[fetcher stderr] {result.stderr.strip()}", file=sys.stderr)

    if result.returncode != 0:
        print(f"Error: fetcher '{fetcher}' exited with code {result.returncode}.", file=sys.stderr)
        sys.exit(1)

    output = result.stdout.strip()
    if not output:
        print(f"Error: fetcher '{fetcher}' returned no output.", file=sys.stderr)
        sys.exit(1)

    return output


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


def print_papers_table(papers: list[dict]) -> None:
    if not papers:
        print("No papers found.")
        return

    col_title = 52
    col_year = 6
    col_authors = 32
    col_id = 22

    header = f"  {'#':<4} {'Title':<{col_title}} {'Year':<{col_year}} {'Authors':<{col_authors}} {'ExternalId':<{col_id}}"
    print(header)
    print("  " + "-" * (len(header) - 2))
    for i, paper in enumerate(papers):
        title = truncate(paper.get("title", ""), col_title)
        year = str(paper.get("year", ""))
        authors = truncate(format_authors(paper.get("authors", [])), col_authors)
        external_id = truncate(paper.get("external_id") or "—", col_id)
        print(f"  {i:<4} {title:<{col_title}} {year:<{col_year}} {authors:<{col_authors}} {external_id:<{col_id}}")


# --- Subcommands ---

def cmd_list(_args):
    fetchers = available_fetchers()
    if not fetchers:
        print("No fetchers found.")
        return
    print(f"Available fetchers ({len(fetchers)}):")
    for name in fetchers:
        print(f"  {name}")


def cmd_options(args):
    fetcher = args.fetcher
    schema: dict[str, str] = json.loads(run_fetcher(fetcher, "options"))
    fetcher_config = get_options_for(fetcher)

    if not CONFIG_FILE.exists():
        print("Hint: no config.json found. Run 'init-config' to create one.\n")

    print(f"Options for '{fetcher}':")
    print(f"  {'Key':<28} {'Description':<42} Configured")
    print(f"  {'-'*28} {'-'*42} {'-'*10}")
    for key, description in schema.items():
        configured = "yes" if key in fetcher_config else "no"
        print(f"  {key:<28} {description:<42} {configured}")


def cmd_init_config(args):
    if CONFIG_FILE.exists() and not args.overwrite:
        print(f"Error: {CONFIG_FILE} already exists. Use --overwrite to replace it.", file=sys.stderr)
        sys.exit(1)

    fetchers = available_fetchers()
    if not fetchers:
        print("No fetchers found — nothing to configure.")
        return

    config = {}
    for fetcher in fetchers:
        schema: dict[str, str] = json.loads(run_fetcher(fetcher, "options"))
        config[fetcher] = {key: "" for key in schema}

    with open(CONFIG_FILE, "w") as f:
        json.dump(config, f, indent=2)

    print(f"Created {CONFIG_FILE} with {len(fetchers)} fetcher(s).")
    print("Fill in the option values before running search or reference commands.")


def cmd_search(args):
    fetcher = args.fetcher
    payload = json.dumps({"search_query": args.query, "options": get_options_for(fetcher)})
    papers: list[dict] = json.loads(run_fetcher(fetcher, "query", payload))

    print_papers_table(papers)
    saved = save_output("search", fetcher, papers)
    print(f"\n{len(papers)} paper(s) found. Full results saved to: {saved}")


def cmd_references(args, action: str):
    fetcher = args.fetcher
    paper = load_paper(args.paper)
    payload = json.dumps({"paper": paper, "options": get_options_for(fetcher)})
    papers: list[dict] = json.loads(run_fetcher(fetcher, action, payload))

    print_papers_table(papers)
    saved = save_output(action, fetcher, papers)
    print(f"\n{len(papers)} paper(s) found. Full results saved to: {saved}")


# --- Entry point ---

def main():
    parser = argparse.ArgumentParser(
        prog="fetcher-cli",
        description="CLI for interacting with SnowballR fetcher plugins.",
    )
    sub = parser.add_subparsers(dest="command", required=True)

    sub.add_parser("list", help="List all available fetchers.")

    p_opts = sub.add_parser("options", help="Show a fetcher's options and their config status.")
    p_opts.add_argument("fetcher", help="Fetcher name.")

    p_search = sub.add_parser("search", help="Search for papers.")
    p_search.add_argument("fetcher", help="Fetcher name.")
    p_search.add_argument("query", help="Search query string.")

    p_fwd = sub.add_parser("forwards", help="Fetch papers that cite the given paper.")
    p_fwd.add_argument("fetcher", help="Fetcher name.")
    p_fwd.add_argument("paper", help="Paper as a JSON string or path to a JSON file.")

    p_bwd = sub.add_parser("backwards", help="Fetch papers cited by the given paper.")
    p_bwd.add_argument("fetcher", help="Fetcher name.")
    p_bwd.add_argument("paper", help="Paper as a JSON string or path to a JSON file.")

    p_init = sub.add_parser("init-config", help="Create config.json pre-populated with all fetcher option keys.")
    p_init.add_argument("--overwrite", action="store_true", help="Overwrite existing config.json.")

    args = parser.parse_args()
    {
        "list": cmd_list,
        "options": cmd_options,
        "search": cmd_search,
        "forwards": lambda a: cmd_references(a, "forwards"),
        "backwards": lambda a: cmd_references(a, "backwards"),
        "init-config": cmd_init_config,
    }[args.command](args)


if __name__ == "__main__":
    main()
