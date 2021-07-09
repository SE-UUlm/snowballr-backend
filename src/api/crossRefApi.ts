import { IApiQuery } from "./iApiQuery.ts";
import { IApiResponse } from "./iApiResponse.ts";
import { IApiFetcher } from "./iApiFetcher.ts";
import { IApiPaper, sourceApi } from './iApiPaper.ts';
import { logger } from "./logger.ts";
import { IApiAuthor } from "./iApiAuthor.ts";
import { IApiUniqueId, idType } from "./iApiUniqueId.ts";
import { sleep } from "https://deno.land/x/sleep/mod.ts";

export class CrossRefApi implements IApiFetcher {
	url: string;
	private _headers: {};
	private _rateLimit: number = 50;
	private _rateInterval: number = 1;
	private _iterations: number = 0;

	public constructor(url: string) {
		logger.info("CrossRefApi initialized");
		this.url = url;
		this._headers = { "Content-Type": "application/json" };
	}

	/**
	 * Checks for a paper at the microsoft research api via a query object.
	 * Returns the papers gathered info and all papers citated or referenced.
	 *
	 * @param query - Object defined by interface in IApiQuery to filter and query api calls.
	 * @returns Object containing the fetched paper and all paperObjects from citations and references. Promise.
	 */
	public async fetch(query: IApiQuery): Promise<IApiResponse> {
		var citations: IApiPaper[];
		var paper: IApiPaper;
		var references: IApiPaper[];
		var rawReferences: [];
		//logger.debug(query);

		let rawPaperResponse = {};

		let response = fetch(`${this.url}/${query.id}?mailto=lukas.romer@uni-ulm.de`)
			.then(data => {
				rawPaperResponse = data.statusText;
				this._rateInterval = Number(data.headers.get("x-rate-limit-interval")) ? Number(data.headers.get("x-rate-limit-interval")!.replace('s', '')) : this._rateInterval;
				this._rateLimit = Number(data.headers.get("x-rate-limit-limit")) ? Number(data.headers.get("x-rate-limit-limit")!.replace('s', '')) : this._rateInterval;
				return data.json();
			})
			.then(data => {
				paper = this._parseResponse(data);

				rawReferences = data.message.reference;
				return data.message.relation ? this._getChildObjects(data.message.relation.cites) : [];
			})
			.then(data => {
				citations = data;
				return this._getChildObjects(rawReferences);
			})
			.then(data => {
				references = data;
				let apiReturn: IApiResponse = {
					"paper": paper,
					"citations": citations,
					"references": references
				}
				return apiReturn;
			})
			.catch(data => {
				logger.error("Error while fetching crossRefApi. PaperQuery " + rawPaperResponse);
				return {
					"paper": paper ? paper : undefined,
					"citations": citations ? citations : undefined,
					"references": references ? references : undefined
				} as IApiResponse;
			})
		return response;
	}

	/**
	 * Makes a http fetch call for each entry for citations and references provided by the original paper.
	 * If a DOI is provided prefers to http fetch via the doi sind its way faster and provides a unique result.
	 * If there's no DOI http fetch is done by querying the unstructured string provided in the entry.
	 * @param rawChildren - list of citation or reference objcets return by the api
	 * @returns Object list of citations or references implemented via IApiPaper
	 */
	private async _getChildObjects(rawChildren: any): Promise<IApiPaper[]> {
		var fetchableByDoi: string[] = [];
		var fetchableByBibliographic: string[] = [];
		let children: Array<IApiPaper> = [];
		var fetches = [];

		for (let r of rawChildren) {
			if (r.DOI) {

				fetchableByDoi.push(r.DOI);
			} else {
				//console.log(r);
				fetchableByBibliographic.push(r.unstructured);
			}
		}

		let rawDoi = {};

		for (let d in fetchableByDoi) {

			await this._limitRequests();

			fetches.push(fetch(`${this.url}/${fetchableByDoi[d]}?mailto=lukas.romer@uni-ulm.de`)
				.then(data => {
					rawDoi = data.statusText;
					return data.json();
				})
				.then(data => {
					let ref = this._parseResponse(data);
					return ref;
				})
				.then(data => {
					//console.log(data);
					children.push(data);
				})
				.catch(data => {
					logger.error("ChildObject not fetchable by DOI: " + rawDoi);
				})
			)
		}

		let rawBibliographic: string | null;

		for (let b in fetchableByBibliographic) {
			let query: string = fetchableByBibliographic[b].replace(/ /g, '+');
			//console.log(query);

			await this._limitRequests();

			fetches.push(fetch(`${this.url}/?query.bibliographic=${query}&rows=1&mailto=lukas.romer@uni-ulm.de`)
				.then(data => {
					rawBibliographic = data.headers.get("x-rate-limit-limit");
					return data.json();
				})
				.then(data => {
					//console.log(data);
					let ref = this._parseResponse({ 'message': data.message.items[0] });
					return ref;
				})
				.then(data => {
					//console.log(data);
					children.push(data);
				})
				.catch(data => {
					logger.error("ChildObject not fetchable by Bibliographic: " + rawBibliographic);
				})
			)
		}
		return Promise.all(fetches).then(() => children);
	}

	private async _limitRequests() {
		this._iterations++;
		//logger.warning(this._iterations);
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
		//logger.debug(JSON.stringify(response, null, 2));

		let parsedAuthors: IApiAuthor[] = [];
		if (response.message.author) {
			for (let a of response.message.author) {
				let parsedAuthor: IApiAuthor = {
					id: [],
					orcid: [],
					rawString: a.given && a.family ? [`${a.given}, ${a.family}`] : [],
					lastName: a.family ? [a.family] : [],
					firstName: a.given ? [a.given] : [],
				}
				parsedAuthors.push(parsedAuthor);
			}
		}

		let parsedUniqueIds: IApiUniqueId[] = [];
		parsedUniqueIds.push(
			{
				id: undefined,
				type: idType.ISSN,
				value: response.message.ISSN ? response.message.ISSN : undefined,
			} as IApiUniqueId
		)
		parsedUniqueIds.push(
			{
				id: undefined,
				type: idType.ISBN,
				value: response.message.ISBN ? response.message.ISBN : undefined,
			} as IApiUniqueId
		)
		parsedUniqueIds.push(
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
			numberOfReferences: response.message['reference-count'] ? [response.message['reference-count']] : [],
			numberOfCitations: response.message.relation && response.message.relation.cites ? [response.message.relation.cites.length] : [],
			year: response.Y ? Number(response.Y) : undefined,
			publisher: response.message.publisher ? [response.message.publisher] : [],
			type: response.message.type ? response.message.type : undefined,
			scope: undefined,
			scopeName: undefined,
			pdf: response.message.link ? response.message.link.map((item: any) => item.URL) : undefined,
			uniqueId: parsedUniqueIds,
			source: [sourceApi.CR]
		};
		return parsedResponse;
	}
}