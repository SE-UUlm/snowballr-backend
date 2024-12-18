import { IApiQuery } from "./iApiQuery.ts";
import { IApiResponse } from "./iApiResponse.ts";
import { IApiFetcher } from "./iApiFetcher.ts";
import { IApiPaper, SourceApi } from "./iApiPaper.ts";
import { logger } from "./logger.ts";
import { IApiAuthor } from "./iApiAuthor.ts";
import { IApiUniqueId, idType } from "./iApiUniqueId.ts";
import { Cache } from "./cache.ts";
import { hashQuery } from "../helper/queryHasher.ts";
import { CONFIG } from "../helper/config.ts";
import { warnApiDisabledByConfig } from "../helper/error.ts";

export class SemanticScholar implements IApiFetcher {
  url: string;
  cache: Cache<IApiResponse> | undefined;
  public constructor(url: string, token: string, cache?: Cache<IApiResponse>) {
    logger.info("SemanticScholar initialized");
    this.url = url;
    this.cache = cache;
  }

  /**
   * Checks for a paper at the microsoft research api via a query object.
   * Returns the papers gathered info and all papers citated or referenced.
   *
   * @param query - Object defined by interface in IApiQuery to filter and query api calls.
   * @returns Object containing the fetched paper and all paperObjects from citations and references. Promise.
   */
  public async fetch(query: IApiQuery): Promise<IApiResponse> {
    if (!CONFIG.semanticScholar.enabled) {
      return warnApiDisabledByConfig("SemanticScholar");
    }
    let paper: IApiPaper = {} as IApiPaper;
    let citations: Promise<IApiPaper[]> | undefined;
    let references: Promise<IApiPaper[]> | undefined;
    const queryString = hashQuery(query);
    let apiReturn: IApiResponse;

    try {
      const get = this.cache!.get(queryString);
      if (CONFIG.semanticScholar.useCache && this.cache && get) {
        logger.info(`S2: Loaded fetch from cache.`);
        return get;
      }
      //logger.debug("here")
      const response = await fetch(`${this.url}/${query.doi}`);
      const json = await response.json();
      paper = this._parseResponse(json);
      //logger.critical(json.citations)
      citations = json.citations && this._getChildren(json.citations);
      references = json.references && this._getChildren(json.references);
      //logger.debug(await citations)
      apiReturn = {
        "paper": paper,
        "citations": await citations,
        "references": await references,
      };
      if (this.cache) {
        this.cache.add(queryString, apiReturn);
      }
    } catch (e) {
      logger.critical(`SemanticScholarApi: Failed to fetch Query: ${e}`);
      apiReturn = {
        "paper": paper,
        "citations": citations ? await citations : [],
        "references": references ? await references : [],
      };
    }
    return apiReturn;
  }

  /**
   * Parse all microsoft-id references from a single paper and query for all the ids to return a paperObject for each.
   *
   * @param microsoftIds - list of microsoft-ids provided by the source-paper in key RId
   * @returns list of paperObjects containing the references. Promise.
   */
  private async _getChildren(children: []): Promise<IApiPaper[]> {
    //logger.debug(children)
    const parsedChildren: IApiPaper[] = [];
    for (const child in children) {
      const parsedChild = this._parseResponse(children[child]);
      parsedChildren.push(parsedChild);
    }
    return parsedChildren;
  }

  /**
   * Cast the response of a single paper return by the semanticScholar to a ApiPaper object
   * Used to get normalized result of all apis.
   *
   * @param response - single object return for a single paper by the semanticScholar
   * @returns normalized ApiPaper object.
   */
  private _parseResponse(response: any): IApiPaper {
    //logger.debug(response);

    const parsedAuthors: IApiAuthor[] = [];
    for (const a of response.authors) {
      const parsedAuthor: IApiAuthor = {
        id: undefined,
        orcid: [],
        rawString: [a.name],
        lastName: [],
        firstName: [],
      };
      parsedAuthors.push(parsedAuthor);
    }

    const parsedUniqueIds: IApiUniqueId[] = [];
    response.doi && parsedUniqueIds.push(
      {
        id: undefined,
        type: idType.DOI,
        value: response.doi ? response.doi : undefined,
      } as IApiUniqueId,
    );
    response.paperId && parsedUniqueIds.push(
      {
        id: undefined,
        type: idType.SemanticScholar,
        value: response.paperId ? response.paperId : undefined,
      } as IApiUniqueId,
    );

    const parsedResponse: IApiPaper = {
      id: undefined,
      title: response.title ? [response.title] : [],
      author: parsedAuthors,
      abstract: response.abstract ? [response.abstract] : [],
      numberOfReferences: response.references
        ? [response.references.length]
        : [],
      numberOfCitations: response.citations ? [response.citations.length] : [],
      year: response.year ? [Number(response.year)] : [],
      publisher: [],
      type: [],
      scope: [],
      scopeName: response.venue ? [response.venue] : [],
      pdf: response.url ? [response.url] : [],
      uniqueId: parsedUniqueIds,
      source: [SourceApi.S2],
      raw: [],
    };
    return parsedResponse;
  }

  // deno-lint-ignore require-await
  public async getDoi(query: IApiQuery): Promise<string | undefined> {
    logger.warning(`S2: Not able to fetch without DOI`);
    return undefined;
  }
}
