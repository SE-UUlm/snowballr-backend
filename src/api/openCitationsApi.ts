import { IApiQuery } from "./iApiQuery.ts";
import { IApiResponse } from "./iApiResponse.ts";
import { IApiFetcher } from "./iApiFetcher.ts";
import { IApiPaper, SourceApi } from './iApiPaper.ts';
import { logger } from "./logger.ts";
import { IApiAuthor } from "./iApiAuthor.ts";
import { IApiUniqueId, idType } from "./iApiUniqueId.ts";
import { Cache } from "./cache.ts";
import { hashQuery } from "../helper/queryHasher.ts";
import { CONFIG } from "../helper/config.ts";
import { warnApiDisabledByConfig } from "../helper/error.ts";
import { addCache, getCache } from "../helper/workerHelper.ts";
export class OpenCitationsApi implements IApiFetcher {
	url: string;
	cache: Cache<IApiResponse> | undefined;
	private _headers: {};

	public constructor(url: string, token: string, cache?: Cache<IApiResponse>) {
		logger.info("OpenCitationsApi initialized");
		this.url = url;
		this._headers = { "Content-Type": "application/json" };
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
		if (!CONFIG.openCitations.enabled) { return warnApiDisabledByConfig("OpenCitations"); }
		var paper: IApiPaper = {} as IApiPaper;
		var citations: Promise<IApiPaper[]> | undefined;
		let references: Promise<IApiPaper[]> | undefined;
		let queryString = hashQuery(query);
		try {
			console.log("here")
			if (CONFIG.openCitations.useCache) {
				let get = await getCache(queryString, SourceApi.OC);
				if (get) {
					logger.info(`OC: Loaded fetch from cache.`)
					//console.log(get)
					return get;
				}
			}
			console.log("there")
			let response = await fetch(`${this.url}/index/api/v1/metadata/${query.doi}`);
			let json = await response.json();
			paper = this._parseResponse(json[0]);
			citations = this._getLinkedDOIs(json[0].citation);
			references = this._getLinkedDOIs(json[0].reference);
			var apiReturn: IApiResponse = {
				"paper": paper,
				"citations": await citations,
				"references": await references
			}
			if (this.cache || CONFIG.openCitations.useCache) {
				console.log("OC TRY CACHE ADD")
				addCache(queryString, SourceApi.OC, apiReturn);
			}
		}
		catch (e) {
			logger.critical(`OpenCitationsApi: Failed to fetch Query: ${e}`);
			var apiReturn: IApiResponse = {
				"paper": paper,
				"citations": citations ? await citations : [],
				"references": references ? await references : []
			}
		}
		logger.info("OC: DONE");
		return apiReturn;
	}

	/**
	 * Make a single ORed http call to fetch all entries for a list of DOIs.
	 * Used to get citations and references.
	 * ORed http calls with multiple DOIs can be done by seperating dois with "__"
	 * @param dois - string of DOIs return by the original api called. Each doi separated by ";"
	 * @returns Object List of IApiPaper with all metadata for the references or citations. Promise.
	 */
	private async _getLinkedDOIs(dois: string): Promise<IApiPaper[]> {
		//let urlQuery: string = dois.replace(/; /g, '__');
		let doilist = dois.split('; ')
		//console.log(doilist.length)
		let children: Array<IApiPaper> = [];
		//logger.debug(urlQuery)
		let fetches: any = [];
		try {
			while (doilist.length > 0) {
				let currentDois = doilist.splice(0, CONFIG.openCitations.linkedFetchSize);
				let urlQuery = currentDois.join('__');
				fetches.push(await this._splittedRequest(urlQuery));
			}

			//TODO HERE
			//let promises = await Promise.all(fetches);
			let promises = fetches

			promises.forEach((json: any) => {
				//console.log(json)
				json.forEach((value: any) => {
					//console.log(value)
					let child = this._parseResponse(value);
					children.push(child);
				});
			});
		}
		catch (e) {
			logger.error(`OpenCitationsApi: Failed to fetch ChildObjects: ${e}`);
		}
		return children;
	}

	private async _splittedRequest(urlQuery: string) {
		const maxRetries = 5;
		let count = 0;
		while (true) {
			try {
				console.log(urlQuery)
				var response = await fetch(`${this.url}/index/api/v1/metadata/${urlQuery}`);
				if (response.status !== 200) {
					throw new Error("OC: Failed to fetch batch of linkedDois");
				}
				break;
			}
			catch (e) {
				if (++count === maxRetries) throw e;
			}
		}
		return response.json();
	}

	/**
	 * Cast returns from rest api to a single IApiPaper Object
	 * Tries to except missing parameters, keys and values filling them with undefined
	 * @param rescccponse - api metadata for a single paper
	 * @returns Object Single IApiPaper object which represents metadata of such
	 */
	private _parseResponse(response: any): IApiPaper {
		var refCount: any;

		//logger.debug(response);
		try {
			if (response.reference && !(response.reference in ["", undefined])) {
				refCount = response.reference.split(";").length;
			} else if (response.doi_reference && !(response.doi_reference in ["", undefined])) {
				refCount = response.doi_reference.split[";"].length;
			} else {
				refCount = 0;
			}
		} catch (e) {
			refCount = 0;
		}

		let parsedAuthors: IApiAuthor[] = [];
		for (let a of response.author.split(';')) {
			let parsedAuthor: IApiAuthor = {
				id: undefined,
				orcid: a.split(',').length > 2 ? [a.split(',')[2].trim()] : [],
				rawString: a.split(',').length > 1 ? [`${a.split(',')[0]},${a.split(',')[1]}`.trim()] : [a.split(',')[0].trim()],
				lastName: a.split(',').length > 1 ? [a.split(',')[0].trim()] : [],
				firstName: a.split(',').length > 1 ? [a.split(',')[1].trim()] : [a.split(',')[0].trim()],
			}
			parsedAuthors.push(parsedAuthor);
		}

		let parsedUniqueIds: IApiUniqueId[] = [];
		response.doi && parsedUniqueIds.push(
			{
				id: undefined,
				type: idType.DOI,
				value: response.doi
			} as IApiUniqueId
		)

		let parsedResponse: IApiPaper = {
			id: undefined,
			title: response.title ? [response.title] : [],
			author: parsedAuthors,
			abstract: [],
			numberOfReferences: refCount ? [refCount] : [],
			numberOfCitations: response.citation_count ? [parseInt(response.citation_count)] : [],
			year: response.year ? [Number(response.year)] : [],
			publisher: [],
			type: [],
			scope: [],
			scopeName: response.source_title ? [response.source_title] : [],
			pdf: response.oa_link ? response.oa_link.split(",") : [],
			uniqueId: parsedUniqueIds,
			source: [SourceApi.OC],
			raw: []
		};
		return parsedResponse;
	}

	public async getDoi(query: IApiQuery): Promise<string | undefined> {
		logger.warning(`OC: Not able to fetch without DOI`);
		return undefined;
	}
}