> [!WARNING]
> Python fetchers execute on the same system as the backend instance and thus
> have direct access to the same system. They can access the filesystem, create
> network connections and much more. ONLY USE TRUSTED FETCHERS!

There are a lot of online databases providing access to a vast collection of
scientific publications. SnowballR aims to unite these online databases and
integrate them into a coherent and low-friction user experience. Instead of
time-consumingly needing to search these databases by hand, SnowballR takes
care of it behind the scenes and provides the user with a collection of relevant
papers that they can review directly.

To access these services, the SnowballR backend needs a way to talk to them.
This could for example be an http client sending requests to a REST API. An
object allowing SnowballR to interface with a database will be referred to as
a _fetcher_.

As the landscape of services is ever-changing, hard-coding a fetcher for every
one of these inside the SnowballR backend would be very costly and fragile. Yet,
SnowballR should not be your limiting factor. It should adapt and evolve; be
customizable to suit your needs. That is why we decided to implement a plugin
system, allowing you to create your very own fetchers with ease.

At the heart of this plugin system is the [Python](https://www.python.org/)
programming language. It has a low barrier of entry and easy syntax whilst also
being powerful and developer-friendly. Furthermore, it provides a broad set of
builtin functionality and is very widespread, even in non-computer-science
degrees. This makes it a very natural and favourable choice.

> [!INFO]
> Installing fetchers requires direct access to a running instance of the
> SnowballR backend. Contact your instance administrator if required.

The following guide assumes you have basic python knowledge and direct access
to a running SnowballR backend instance.

### Prerequisites

- A working Python installation (Version >= 3.12).
- A properly set-up and activated [Python virtual environment](https://docs.python.org/3/library/venv.html).
- Your favourite code editor.

The plugin system requires a base set of python dependencies to work. Install
them using the following command (again, make sure you're in the same `venv` the
SnowballR backend will later be executed in):

```bash
pip install -r requirements.txt
```

The list of required packages can be also found in the [requirements.txt](https://github.com/SE-UUlm/snowballr-backend/blob/develop/requirements.txt).

> [!IMPORTANT]
> The plugin system makes use of the environments `python` installation. Again,
> make sure to execute the backend in the correct `venv`, where the required
> packages were also installed.

To get proper autocomplete and type checking for the SnowballR types, add the
`./plugins/fetchers/lib/` directory to your python path. This can be done by
adding it to the `PYTHONPATH` environment variable. If the directory does not
exist yet, start the backend as it should be created automatically.

### Plugin Directory

Fetcher plugins are loaded during runtime from the `fetchers` subdirectory
of the plugin directory. This directory can be configured using the
`PLUGIN_DIRECTORY` environment variable and defaults to `./plugins/`. This
directory is relative to the working directory of the backend and should
already be present if you have previously started the backend. Every `.py`
file directly contained within the `fetchers` directory will be treated as a
fetcher. If you would like to create a reusable module/library, put it inside a
subdirectory like the `lib` folder or create a new one.

### Fetcher Contract

Every fetcher is required to follow an implicit contract to be compatible with
the backend. This contract is automatically adhered to if you use the bundled
SnowballR library:

```py

from snowballr import fetcher_plugin, Paper

options: dict[str, str] = ...
def search_papers(search_query: str, options: dict[str, str]) -> list[Paper]: ...
def forward_references(paper: Paper, options: dict[str, str]) -> list[Paper]: ...
def backward_references(paper: Paper, options: dict[str, str]) -> list[Paper]: ...

fetcher_plugin(
    options,
    search_papers,
    forward_references,
    backward_references,
)
```

Everything regarding the protocol will be taken care of by calling `fetcher_plugin`.
Just provide implementations for the functions and you're off to go.

To accommodate for API secrets and other variables, each fetcher is configurable
using a string-to-string dictionary. The `options` set serves as a hint
to the frontend, which options this fetcher accepts. There could, however, at
any time be missing or additional entries. Project admins can select a fetcher
and provide key-value pairs as options in the project settings. Remember during
the implementation: A fetcher can be used across multiple projects, each with
their own set of options.

### Writing a Fetcher

Equipped with this knowledge, let's get started and write our very
own fetcher. SnowballR provides a predefined[^1] `Paper`
dataclass, which is expected as a result. The source is located in
`./plugins/fetchers/lib/snowballr.py` and can be imported with
`from snowballr import Paper`. It is mutable and can be constructed like this:

```py
paper = Paper(
    title,             # str
    external_id,       # Optional[str]
    abstract,          # str
    year,              # int
    publisher,         # str
    publication_type,  # str
    publication_name,  # str
    authors,           # list[Author]
    metadata,          # dict[str, str]
)
```

Similarly, the `Author` can be constructed like this:

```py
author = Author(
    first_name, # str
    last_name,  # str
)
```

Let's put all the pieces together:

#### Basic Fetcher

```py
# ./plugins/fetchers/basic.py

from snowballr import fetcher_plugin, Paper, Author

options = {}

def search_papers(searchQuery: str, options: dict[str, str]) -> list[Paper]:
    return [ Paper(
        "title",
        None,
        "abstract",
        2026,
        "publisher",
        "publication_type",
        "publication_name",
        [Author("first_name", "last_name")],
        {}
    ) ]

def forward_references(paper: Paper, options: dict[str, str]) -> list[Paper]:
    return []

def backward_references(paper: Paper, options: dict[str, str]) -> list[Paper]:
    return []

fetcher_plugin(
    options,
    search_papers,
    forward_references,
    backward_references,
)
```

This fetcher has no options, never returns any references and if a paper is
searched for, then it always returns the same one, regardless of the search query.

Of course, this is not very useful. A common way of acquiring data is using some
sort of REST API. Let's take a look at how one would do that.

#### HTTP Requests

We'll make use of the installed `requests` package (which is already a
dependency) to acquire external resources.

```py
# ./plugins/fetchers/http.py

from snowballr import fetcher_plugin, Paper, Author
import requests

options = {
    "API_KEY": "The API key"
}

def search_papers(searchQuery: str, options: dict[str, str]) -> list[Paper]:
    content = requests.get(
        "https://example.com",
        auth=("user", options["API_KEY"])
    ).text

    return [ Paper(
        "title",
        None,
        content,
        2026,
        "publisher",
        "publication_type",
        "publication_name",
        [Author("first_name", "last_name")],
        {}
    ) ]

def forward_references(paper: Paper, options: dict[str, str]) -> list[Paper]:
    return []

def backward_references(paper: Paper, options: dict[str, str]) -> list[Paper]:
    return []

fetcher_plugin(
    options,
    search_papers,
    forward_references,
    backward_references,
)
```

This fetcher accepts an `API_KEY` option, displaying the hint `The API key`
in the frontend during configuration. When a paper is searched, it sends a GET
request and includes the resulting body as the abstract of a paper with the
title "title". It, again, has no reference fetching capabilities.

The possibilities are endless: Access filesystem or SQL databases, make use
of the extensive Python ecosystem or even incorporate machine learning into
a plugin.

#### Debugging

If the fetcher encounters an error (exit-code != 0), the `stderr` output is
printed as an error log entry. If the fetcher exits successfully (exit-code ==
0) and the `stderr` log is not blank, then it is printed as an info log entry.

This can be useful for determining the source of unexpected errors or during
implementation and debugging.

### Developers

If you want to contribute a fetcher to SnowballR, add it to the resources in
`src/main/resources/plugins/fetchers/` and adjust the resource builder in
`src/main/kotlin/se/uulm/snowballr/backend/fetcher/PythonPluginFetcherManager.kt`.

[^1]: There are more definitions provided by SnowballR. Check them out in their
    [definition file](https://github.com/SE-UUlm/snowballr-backend/blob/develop/src/main/resources/plugins/fetchers/lib/snowballr.py).
