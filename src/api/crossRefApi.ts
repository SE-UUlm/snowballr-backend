import { IApiQuery } from "./iApiQuery.ts";
import { IApiResponse } from "./iApiResponse.ts";
import { IApiFetcher } from "./iApiFetcher.ts";
import { IApiPaper, SourceApi } from './iApiPaper.ts';
import { logger } from "./logger.ts";
import { IApiAuthor } from "./iApiAuthor.ts";
import { IApiUniqueId, idType } from "./iApiUniqueId.ts";
import { sleep } from "https://deno.land/x/sleep/mod.ts";
import { Cache } from "./cache.ts";
import { hashQuery } from "../helper/queryHasher.ts";
import { CONFIG } from "../helper/config.ts";

export class CrossRefApi implements IApiFetcher {
	url: string;
	cache: Cache<IApiResponse> | undefined;
	private _headers: {};
	private _rateLimit: number = 50;
	private _rateInterval: number = 1;
	private _iterations: number = 0;
	private _mail: string = "";

	public constructor(url: string, mail?: string, cache?: Cache<IApiResponse>) {
		logger.info("CrossRefApi initialized");
		this.url = url;
		//this._headers = { 'User-Agent': `GroovyBib/1.1 (https://example.org/GroovyBib/; mailto:GroovyBib@example.org) BasedOnFunkyLib/1.4` };
		this._headers = { "Content-Type": "application/json" };
		mail ? this._mail = `?mailto=${mail}` : "";
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
		var citations: Promise<IApiPaper[]> | undefined;
		var paper: IApiPaper = {} as IApiPaper;
		var references: Promise<IApiPaper[]> | undefined;
		let queryString = hashQuery(query);

		try {
			let get = this.cache!.get(queryString)
			if (CONFIG.crossRef.useCache && this.cache && get) {
				logger.info(`CR: Loaded fetch from cache.`);
				return get;
			}

			let response = await fetch(this._parseQuery(query), {
				headers: this._headers,
			})

			/** Get rate limit from api to apply it dynamically since it can change from time to time */
			this._rateInterval = Number(response.headers.get("x-rate-limit-interval")) ? Number(response.headers.get("x-rate-limit-interval")!.replace('s', '')) : this._rateInterval;
			this._rateLimit = Number(response.headers.get("x-rate-limit-limit")) ? Number(response.headers.get("x-rate-limit-limit")!.replace('s', '')) : this._rateInterval;
			let json = await response.json();
			paper = this._parseResponse(json);
			references = json.message.reference ? this.getChildObjects(json.message.reference) : new Promise((resolve) => { resolve([]); });
			citations = json.message.relation && json.message.relation.cites ? this.getChildObjects(json.message.relation.cites) : new Promise((resolve) => { resolve([]); });
			var apiReturn: IApiResponse = {
				"paper": paper,
				"citations": await citations,
				"references": await references
			}
			if (this.cache) {
				this.cache.add(queryString, apiReturn);
			};

		}
		catch (e) {
			logger.warning(`CrossRef: Failed to fetch Query: ${e}`);
			var apiReturn: IApiResponse = {
				"paper": paper,
				"citations": citations ? await citations : [],
				"references": references ? await references : []
			}
		}

		return apiReturn;
	}

	private _parseQuery(query: IApiQuery): string {
		if (query.doi) { return `${this.url}/${query.doi}` };
		if (query.title && query.rawName) { return `${this.url}?query.bibliographic=${query.title.replace(/\s/g, '+')}&query.author=${query.rawName.replace(/\s/g, '+')}&rows=1` };
		if (query.title) { return `${this.url}?query.bibliographic=${query.title.replace(/\s/g, '+')}&rows=1` };
		throw new Error('Cannot fetch for this query. We either need a doi or a title.')
	}

	/**
	 * Makes a http fetch call for each entry for citations and references provided by the original paper.
	 * If a DOI is provided prefers to http fetch via the doi sind its way faster and provides a unique result.
	 * If there's no DOI http fetch is done by querying the unstructured string provided in the entry.
	 * @param rawChildren - list of citation or reference objcets return by the api
	 * @returns Object list of citations or references implemented via IApiPaper
	 */
	public async getChildObjects(rawChildren: any): Promise<IApiPaper[]> {
		var fetchableByDoi: string[] = [];
		var fetchableByBibliographic: string[] = [];
		var children: Array<IApiPaper> = [];
		var fetches = [];

		for (let r of rawChildren) {
			if (r.DOI) {
				fetchableByDoi.push(r.DOI);
			} else {

				fetchableByBibliographic.push(r.unstructured);
			}
		}

		for (let d in fetchableByDoi) {
			////logger.debug("fetchableByDoi")
			await this._limitRequests();
			fetches.push(this._fetchDoiFromApi(fetchableByDoi[d]));
		}

		for (let b in fetchableByBibliographic) {
			if (fetchableByBibliographic[b] === undefined) { continue; }
			let query: string = fetchableByBibliographic[b].replace(/ /g, '+');
			await this._limitRequests();
			fetches.push(this._fetchBibFromApi(query));
		}
		return await Promise.all(fetches);
	}

	private async _fetchDoiFromApi(item: string): Promise<IApiPaper> {
		try {
			let response = await fetch(`${this.url}/${item}`, {
				headers: this._headers,
			});
			let json = await response.json();

			let child = this._parseResponse(json);
			return child;
		}
		catch (e) {
			logger.warning(`CrossRef: Failed to fetch child by doi: ${e}`);
			return {} as IApiPaper;
		}
	}

	private async _fetchBibFromApi(item: string): Promise<IApiPaper> {
		try {
			let response = await fetch(`${this.url}?query.bibliographic=${item.replace(/\s/g, '+')}&rows=1`, {
				headers: this._headers,
			});
			let json = await response.json();
			let child = this._parseResponse({ 'message': json.message.items[0] });
			return child;
		}
		catch (e) {
			logger.warning(`CrossRef: Failed to fetch child by doi: ${e}`);
			return {} as IApiPaper;
		}
	}

	private async _limitRequests() {
		this._iterations++;
		////logger.warning(this._iterations);
		if (this._iterations === this._rateLimit) {
			await sleep(this._rateInterval * 2);
			this._iterations = 0;
			logger.warning(`Limiting Crossref Api due to following Restricitons: rateLimit=${this._rateLimit}; rateInterval=${this._rateInterval}`);
		}
	}

	/**
	 * Cast returns from rest api to a single IApiPaper Object
	 * Tries to except missing parameters, keys and values filling them with undefined
	 * @param response - api metadata for a single paper
	 * @returns Object Single IApiPaper object which represents metadata of such
	 */
	private _parseResponse(response: any): IApiPaper {
		////logger.debug(JSON.stringify(response, null, 2));

		let parsedAuthors: IApiAuthor[] = [];
		if (response.message.author) {
			for (let a of response.message.author) {
				let parsedAuthor: IApiAuthor = {
					id: undefined,
					orcid: [],
					rawString: a.given && a.family ? [`${a.given}, ${a.family}`] : [],
					lastName: a.family ? [a.family] : [],
					firstName: a.given ? [a.given] : [],
				}
				parsedAuthors.push(parsedAuthor);
			}
		}

		let parsedUniqueIds: IApiUniqueId[] = [];
		response.message.ISSN && parsedUniqueIds.push(
			{
				id: undefined,
				type: idType.ISSN,
				value: response.message.ISSN ? response.message.ISSN : undefined,
			} as IApiUniqueId
		)
		response.message.ISBN && parsedUniqueIds.push(
			{
				id: undefined,
				type: idType.ISBN,
				value: response.message.ISBN ? response.message.ISBN : undefined,
			} as IApiUniqueId
		)
		response.message.DOI && parsedUniqueIds.push(
			{
				id: undefined,
				type: idType.DOI,
				value: response.message.DOI ? response.message.DOI : undefined,
			} as IApiUniqueId
		)

		let parsedResponse: IApiPaper = {
			id: undefined,
			title: response.message.title[0] ? [response.message.title[0]] : [],
			author: parsedAuthors,
			abstract: [],
			numberOfReferences: response.message['references-count'] ? [response.message['references-count']] : [],
			numberOfCitations: response.message['is-referenced-by-count'] ? [response.message['is-referenced-by-count']] : [],
			year: response.Y ? [Number(response.Y)] : [],
			publisher: response.message.publisher ? [response.message.publisher] : [],
			type: response.message.type ? [response.message.type] : [],
			scope: [],
			scopeName: [],
			pdf: response.message.link ? response.message.link.map((item: any) => item.URL) : [],
			uniqueId: parsedUniqueIds,
			source: [SourceApi.CR],
			raw: []
		};
		return parsedResponse;
	}

	public async getDoi(query: IApiQuery): Promise<string | undefined> {
		try {
			let url = `${this.url}?query.bibliographic=${query.title!.replace(/\s/g, '+')}&rows=1`;
			let response = await fetch(url, {
				headers: this._headers,
			});
			let json = await response.json();
			return json.message.items[0].DOI;
		}
		catch (e) {
			//logger.warning(`CR: Couldnt fetch DOI for the following query: ${JSON.stringify(query, null, 2)}`);
			//logger.warning(e);
		}
		return undefined;
	}
}