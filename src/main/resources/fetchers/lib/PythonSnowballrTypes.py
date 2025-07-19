from dataclasses import dataclass
from typing import Optional
import builtins

# Unsafe hash needed to make it hashable whilst maintaining mutability.
# Do not add an object of this class to a dict and then modify it!
@dataclass(unsafe_hash=True)
class Paper:
    title: str
    abstract: str
    externalId: Optional[str] = None
    publishedAt: Optional[int] = None
    publisher: Optional[str] = None
    publicationType: Optional[str] = None
    publicationName: Optional[str] = None

class Logger:
    def __init__(self, impl):
        self.__impl__ = impl
    def info(self, msg):
        self.__impl__.info(str(msg))
    def warn(self, msg):
        self.__impl__.warn(str(msg))
    def error(self, msg):
        self.__impl__.error(str(msg))
    def debug(self, msg):
        self.__impl__.debug(str(msg))
    def trace(self, msg):
        self.__impl__.trace(str(msg))

class FetcherManager:
    def __init__(self, impl):
        self.__impl__ = impl
    def getAvailableFetchers(self) -> set[str]:
        return self.__impl__.getAvailableFetchers()
    def getAvailableOptions(self, fetcher: str) -> set[str]:
        return self.__impl__.getAvailableOptions(fetcher)
    def searchPapers(self, fetcher: str, searchQuery: str, options: dict[str, str]) -> set[Paper]:
        return self.__impl__.searchPapers(fetcher, searchQuery, options)
    def fetchForwardReferences(self, fetcher: str, paper: Paper, options: dict[str, str]) -> set[Paper]:
        return self.__impl__.fetchForwardReferences(fetcher, paper, options)
    def fetchBackwardReferences(self, fetcher: str, paper: Paper, options: dict[str, str]) -> set[Paper]:
        return self.__impl__.fetchBackwardReferences(fetcher, paper, options)

# Workaround used to make ruff and lsp not complain about
# missing definitions
log = Logger(builtins.snowballr["log"])
fetchers = FetcherManager(builtins.snowballr["fetchers"])
