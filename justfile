set positional-arguments

default:
    @just --list

# Run fetcher CLI
fetcher-cli *args='':
    uv run --no-project tools/fetcher-cli/cli.py "$@"
