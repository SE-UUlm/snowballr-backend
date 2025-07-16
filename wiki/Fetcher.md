> [!WARNING]
> Python fetchers execute on the same system as the backend instance and thus
> have direct access to the same system. They can access the filesystem, create
> network connections and much more. ONLY USE TRUSTED FETCHERS!

There are a lot of online databases providing access to a vast collection of
scientific publications. SnowballR aims to unite these online databases and
integrate them into a coherent and low-friction user experience. Instead of
time-consumingly needing to search these databases by hand, SnowballR takes
care of it behind the scenes so you can focus on the important part: reviewing
papers.

To access these services, the SnowballR backend needs a way to talk to them.
This could for example be an http client sending requests to a REST API. In the
following, an object allowing SnowballR to interface with a database will be
referred to as a _fetcher_.

As the landscape of services is ever-changing, hard-coding a fetcher for each
and every one of these inside of SnowballR's backend would be very costly and
fragile. Yet, SnowballR should not be your limiting factor. It should adapt and
evolve; be customizable to suit your needs. That is why we decided to implement
a plugin system, allowing you to create your very own fetchers with ease.

At the heart of this plugin system is the [Python](https://www.python.org/)
programming language. It has a low barrier of entry and easy syntax whilst also
being powerful and developer-friendly. Furthermore, it provides a broad set of
builtin functionality and is very widespread, even in non-computer-science
degrees. This makes it a very natural and favourable choice.

> [!IMPORTANT]
> Installing fetchers requires direct access to a running instance of the
> SnowballR backend.

The following guide assumes you have basic python knowledge and direct access
to a running SnowballR backend instance.

### Prerequisites

- A working Python installation.
- A working PIP installation.
- The Python development headers.
- The OpenJDK development headers.
- Your favourite code editor.

These commands should get you started:
- Fedora: `dnf install -y python3 python3-devel python3-pip  java-21-openjdk-devel gcc`
- Ubuntu: `apt install --update -y python3 python3-dev python3-pip python3-venv openjdk-21-jdk gcc`
- Other: The packages should have similar names.

The plugin system makes use of the system `python` and `pip` installations,
assuming that they are already installed. If you are using the Docker container,
these dependencies are already included.

To improve the developer experience, some kind of lsp-assisted editor is
recommended. If you are using [VSCode](https://code.visualstudio.com/), check
out the [Python Extension](https://marketplace.visualstudio.com/items?itemName=ms-python.python).
There are similar extension for other Editors like [Zed](https://zed.dev/) or,
if you live in the terminal (you know who you are), there are
[ruff](https://github.com/astral-sh/ruff) and
[python-lsp](https://github.com/python-lsp/python-lsp-server).

### Dependencies

The python plugin system is implemented using the
[Jep](https://github.com/ninia/jep) library. The native library it relies on
has to be installed first. Do this using `pip`:

```bash
pip install jep
```

In case pip does not allow you to install `jep` this way because it is managed
by the system package manager, use a
[virtual environment](https://docs.python.org/3/library/venv.html) instead:

```bash
python3 -m venv ./venv
source ./venv/bin/activate
pip install jep
```

Remember, however, that the backend has to be also executed inside this venv
for the plugins to work.

### Plugin Directory

Fetcher plugins are loaded during runtime from a specific directory. This
directory can be configured using the `FETCHER_PLUGIN_DIRECTORY` environment
variable and defaults to `./plugins/fetchers/`. This directory is relative to
the working directory of the backend and should already be present if you have
previously started the backend. Every `*.py` file directly contained within the
plugin directory will be treated as a fetcher. If you would like to create a
reusable module/library, put it inside a subdirectory like the `lib` folder or
create a new one.

### Fetcher Contract

Every fetcher is required to follow an implicit contract to be compatible with
the backend. The following definitions with their respective types need to be
present:

```py
availableOptions: set[str] = ...
def searchPapers(searchQuery: str, options: dict[str, str]) -> set[Paper]: ...
def fetchForwardReferences(paper: Paper, options: dict[str, str]) -> set[Paper]: ...
def fetchBackwardReferences(paper: Paper, options: dict[str, str]) -> set[Paper]: ...
```

To accommodate for API secrets and other variables, each fetcher is configurable
using a string-to-string dictionary. The `availableOptions` set serves as a hint
to the frontend, which options this fetcher accepts. There could, however, at
any time be missing or additional entries. Project admins can select a fetcher
and provide key-value pairs as options in the project settings. Remember during
the implementation: A fetcher can be used accross multiple projects, each with
their own set of options.

**searchPapers**:

### Writing a Fetcher

Equipped with this knowledge, let's get started and write our own example
fetcher. SnowballR provides a predefined[^1] `Paper` dataclass, which is
expected as a result. The source is located in
`./plugins/fetchers/lib/snowballr.py` and can be imported like this:
`from lib.snowballr import Paper`. It is mutable and can be constructed like
this:

```py
paper = Paper(
    title,            # str (required)
    abstract,         # str (required)
    externalId,       # str
    publishedAt,      # int: seconds since epoch
    publisher,        # str
    publicationType,  # str
    publicationName   # str
)
```

Let's put all the pieces together:

#### Basic Fetcher

```py
# ./plugins/fetchers/basic.py

from lib.snowballr import Paper

availableOptions = {}

def searchPapers(searchQuery: str, options: dict[str, str]) -> set[Paper]:
    return { Paper("title", "abstract") }

def fetchForwardReferences(paper: Paper, options: dict[str, str]) -> set[Paper]:
    return {}

def fetchBackwardReferences(paper: Paper, options: dict[str, str]) -> set[Paper]:
    return {}
```

Of course, this is not very useful. A common way of acquiring data is using some
sort of REST API. Let's take a look at how one would do that.

#### HTTP Requests

First, install the [requests](https://pypi.org/project/requests/) package:

```bash
pip install requests
```

Now it can be used like this to make requests:

```py
# ./plugins/fetchers/http.py

from lib.snowballr import Paper
import requests

availableOptions = {
    "API_KEY"
}

def searchPapers(searchQuery: str, options: dict[str, str]) -> set[Paper]:
    content = requests.get(
        "https://example.com",
        auth=("user", options["API_KEY"])
    ).text

    return {
        Paper("title", content)
    }

def fetchForwardReferences(paper: Paper, options: dict[str, str]) -> set[Paper]:
    return {}

def fetchBackwardReferences(paper: Paper, options: dict[str, str]) -> set[Paper]:
    return {}
```

But the possibilities do not end here. You could access a filesystem database,
resort to many `pip` libraries or even access other fetchers using the global
`fetchers` instance[^1]:

```py
class FetcherManager:
    def getAvailableFetchers(self) -> set[str]: ...
    def getAvailableOptions(self, fetcher: str) -> set[str]: ...
    def searchPapers(self, fetcher: str, searchQuery: str, options: dict[str, str]) -> set[Paper]: ...
    def fetchForwardReferences(self, fetcher: str, paper: Paper, options: dict[str, str]) -> set[Paper]: ...
    def fetchBackwardReferences(self, fetcher: str, paper: Paper, options: dict[str, str]) -> set[Paper]: ...
```

Using it like this:

```py
from lib.snowballr import fetchers
fetchers.searchPapers("other", "query", {})
```

#### Debugging

You can use the global `log` instance to print messages to the backend's log.
The interface looks like this:

```py
class Logger:
    def info(self, msg: str): ...
    def warn(self, msg: str): ...
    def error(self, msg: str): ...
    def debug(self, msg: str): ...
    def trace(self, msg: str): ...
```

Just use it anywhere in your code: `log.info("hi from python")`.

[^1]: There are more definitions provided by SnowballR. Check them out in their
    [definition file](https://github.com/SE-UUlm/snowballr-backend/blob/develop/src/main/resources/PythonSnowballrTypes.py).
