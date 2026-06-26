This page lists tools that assist the development process of this repository.

<!-- markdownlint-disable MD007 -->
<!-- @formatter:off -->
<!-- TOC -->
  * [Fetcher CLI](#fetcher-cli)
    * [Configuration](#configuration)
    * [Subcommands](#subcommands)
      * [list](#list)
      * [info](#info)
      * [search](#search)
      * [forwards](#forwards)
      * [backwards](#backwards)
      * [init-config](#init-config)
<!-- TOC -->
<!-- @formatter:on -->
<!-- markdownlint-enable MD007 -->

## Fetcher CLI

The fetcher plugin system is a core part of the SnowballR application. Ensuring
correct functionality is a high priority. This is currently not easy because the
plugins are not easy to access. Especially searching for forward and backward
references are not called by any API methods, which make them hard to test
manually. This is where the Fetcher CLI should help out. It offers direct access
by providing the same implementation as the
[PythonPluginFetcherManager](https://github.com/SE-UUlm/snowballr-backend/blob/develop/src/main/kotlin/se/uulm/snowballr/backend/fetcher/PythonPluginFetcherManager.kt).
The CLI is located at
[`tools/fetcher-cli`](https://github.com/SE-UUlm/snowballr-backend/tree/develop/tools/fetcher-cli).
It enables faster development of new fetcher plugins and maintenance of existing
ones.

Make sure to call it from an active Python environment (>= 3.13, see
[Getting Started](https://github.com/SE-UUlm/snowballr-backend/wiki/Getting-Started)).

```bash
uv run tools/fetcher-cli/cli.py <subcommand> <args>

# when using just, prefer
just fetcher-cli <subcommand> <args>
```

### Configuration

Some fetcher plugins may require configuration to work correctly, e.g. an API
token. To provide this information, create a `config.json` at
[`tools/fetcher-cli`](https://github.com/SE-UUlm/snowballr-backend/tree/develop/tools/fetcher-cli).
It should contain a configuration object for each fetcher. This may look like
this:

```json
{
    "ExampleFetcher": {
        "API_TOKEN": "<your_api_token>"
    }
}
```

If not already existing, a `config.json` can be created using the following
command:

```bash
just fetcher-cli init-config
```

This will create a configuration object for each existing fetcher.

### Subcommands

If call to a fetcher returns data, it will be stored in
[`tools/fetcher-cli/output`](https://github.com/SE-UUlm/snowballr-backend/tree/develop/tools/fetcher-cli/output).
The subcommands will also print a short summary of the received data.

#### list

Represents `getAvailableFetchers`.

This will list all existing fetchers. There are no additional arguments.

Example:

```bash
just fetcher-cli list
```

#### info

Shows a fetcher's information: its name, description and links, followed by its
options schema. For each option the table lists whether it is required or secret,
whether it is configured in `config.json` and its (effective) value.

Example:

```bash
just fetcher-cli info ExampleFetcher
```

#### search

Represents `searchPapers`.

This will use the specified fetcher to search papers using the specified query.

Example:

```bash
just fetcher-cli search ExampleFetcher Snowballing
```

#### forwards

Represents `fetchForwardReferences`.

This will use the specified fetcher to retrieve all forward references for the
specified paper.

The paper can be either provided as JSON string or as path to JSON file.

Example:

```bash
just fetcher-cli forwards ExampleFetcher tools/fetcher-cli/paper.json
```

#### backwards

Represents `fetchBackwardReferences`.

This will use the specified fetcher to retrieve all backward references for the
specified paper.

The paper can be either provided as JSON string or as path to JSON file.

Example:

```bash
just fetcher-cli backwards ExampleFetcher tools/fetcher-cli/paper.json
```

#### init-config

See [Configuration](#configuration) for usage examples. To overwrite an existing
`config.json`, use `--overwrite`. Note that this may delete existing tokens.
