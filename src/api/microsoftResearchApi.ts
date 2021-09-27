import { IApiQuery } from "./iApiQuery.ts";
import { IApiResponse } from "./iApiResponse.ts";
import { IApiFetcher } from "./iApiFetcher.ts";
import { IApiPaper, SourceApi } from './iApiPaper.ts';
import { logger } from "./logger.ts";
import { IApiAuthor } from "./iApiAuthor.ts";
import { IApiUniqueId, idType } from "./iApiUniqueId.ts";
import { Cache } from "./cache.ts";
import { assign } from "../helper/assign.ts";
import { createHash } from "https://deno.land/std/hash/mod.ts";

export class MicrosoftResearchApi implements IApiFetcher {
	url: string;
	cache: Cache<IApiResponse> | undefined;
	private _authToken: string;
	private _headers: {};
	private _attributes: string;
	private _paperTypeMapper: string[];
	private _queryAttributeMapper: {};

	public constructor(url: string, authToken: string, cache?: Cache<IApiResponse>) {
		logger.info("MicrosoftResearchApi initialized");
		this.url = url;
		this._authToken = authToken;
		this._headers = { "Ocp-Apim-Subscription-Key": this._authToken, "Content-Type": "application/json" };
		this._attributes = "Id,IA,RId,CC,Y,PB,Pt,S,Ti,AA.AuN,DOI";
		this._paperTypeMapper = ["Unknown", "Journal article", "Patent", "Conference paper", "Book chapter", "Book", "Book reference entry", "Dataset", "Repository"];
		this._queryAttributeMapper = {
			"author": "AA.AuN",
			"title": "Ti",
			"id": "DOI",
			"year": "Y",
			"publisher": "PB",
			"type": "Pt"
		};
		this.cache = cache;
	}

	/**
	 * Abstract is provided via dictionary. This function casts an IA-object to a single string.
	 *
	 * @param iA - invertedAbstract object provided by microsoft api
	 * @returns abstract of a paper in a single string.
	 */
	private static _convertInvertedAbstract(iA: any): string[] {
		let abstr: string = "";
		try {
			for (let key in iA.InvertedIndex) {
				abstr += " " + key;
			}
			return [abstr.trim()];
		} catch (error) {
			logger.error("Couldn't convert InvertedAbstract from Microsoft API to a single string: " + error);
			//logger.critical(iA);
			return [];
		}
	}

	/**
	 * Checks for a paper at the microsoft research api via a query object.
	 * Returns the papers gathered info and all papers citated or referenced.
	 *
	 * @param query - Object defined by interface in IApiQuery to filter and query api calls.
	 * @returns Object containing the fetched paper and all paperObjects from citations and references. Promise.
	 */
	public async fetch(query: IApiQuery): Promise<IApiResponse> {
		var paper: IApiPaper = {} as IApiPaper;
		var citations: Promise<IApiPaper[]> | undefined;
		let references: Promise<IApiPaper[]> | undefined;
		let queryIdentifier = createHash("sha3-256");
		queryIdentifier.update(JSON.stringify(query));
		let queryString = queryIdentifier.toString();
		try {
			let get = this.cache!.get(queryString);
			if (this.cache && get) {
				logger.info(`MA: Loaded fetch from cache.`)
				//console.log(get)
				return get;
			}
			let response = await fetch(this.url, {
				method: 'POST',
				headers: this._headers,
				body: JSON.stringify({
					expr: this._parseQuery(query),
					attributes: this._attributes
				})
			})
			let json = await response.json();

			// --> DB
			paper = this._parseResponse(json.entities[0]);
			citations = json.entities[0] && json.entities[0].Id ? this._getCitations(json.entities[0].Id) : new Promise((resolve) => { resolve([]); });
			references = json.entities[0] && json.entities[0].RId ? this._getReferences(json.entities[0].RId) : new Promise((resolve) => { resolve([]); });

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
			logger.warning(`MicrosoftResearchApi: Failed to fetch Query: ${e}`);
			var apiReturn: IApiResponse = {
				"paper": paper,
				"citations": citations ? await citations : [],
				"references": references ? await references : []
			}
		}
		return apiReturn;
	}


	/**
	 * Parse all microsoft-id references from a single paper and query for all the ids to return a paperObject for each.
	 *
	 * @param microsoftIds - list of microsoft-ids provided by the source-paper in key RId
	 * @returns list of paperObjects containing the references. Promise.
	 */
	private async _getReferences(microsoftIds: number[]): Promise<IApiPaper[]> {
		let convertedIds: string[] = microsoftIds.map(String);
		let references: Array<IApiPaper> = [];
		//logger.debug(microsoftIds);
		try {
			convertedIds = convertedIds.map(i => 'Id=' + i);
			let queryPattern = convertedIds.join(',');

			let response = await fetch(this.url, {
				method: 'POST',
				headers: this._headers,
				body: JSON.stringify({
					expr: `Or(${queryPattern})`,
					count: convertedIds.length,
					attributes: this._attributes
				})
			})
			let json = await response.json();
			//logger.debug(json);
			for (let value in json.entities) {
				let cit = this._parseResponse(json.entities[value]);
				references.push(cit);
			}
		}
		catch (e) {
			logger.error(`MicrosoftResearchApi: Failed to fetch References: ${e}`);
		}

		return references;
	}

	/**
	 * Query for all papers containing the original-papers microsoft id in their references
	 *
	 * @param microsoftId - original-paper microsoft id. Returned by another fetch call.
	 * @returns list of paperObjects containing the citations. Promise.
	 */
	private async _getCitations(microsoftId: string): Promise<IApiPaper[]> {
		var citations: Array<IApiPaper> = [];
		try {
			let response = await fetch(this.url, {
				method: 'POST',
				headers: this._headers,
				body: JSON.stringify({
					expr: `RId=${microsoftId}`,
					count: 1000,
					attributes: this._attributes
				})
			})
			let json = await response.json();
			for (let value in json.entities) {
				let cit = this._parseResponse(json.entities[value]);
				citations.push(cit);
			}
		}
		catch (e) {
			logger.error(`MicrosoftResearchApi: Failed to fetch Citations: ${e}`);
		}

		return citations;
	}

	/**
	 * Parse a queryObject to a string readable by the microsoftApi
	 *
	 * @param query - queryObject for the originalPaper.
	 * @returns string appendable to the api call via body-key "expr".
	 */
	private _parseQuery(query: IApiQuery): string {
		if (!((query.title && query.title.trim().length > 0) || query.doi)) {
			throw new Error("Neither a doi nor a title is given. We cannot search for anything.");
		}
		let urlQuery: string = `Or(DOI='${query.doi ? query.doi.toUpperCase() : ""}', Ti='${query.title ? query.title!.toLowerCase() : ""}')`

		if (query.rawName && query.rawName.trim().length > 0 && query.title) {
			urlQuery = `Or(DOI='${query.doi ? query.doi.toUpperCase() : ""}', And(Composite(AA.AuN='${query.rawName!.toLowerCase()}'), Ti='${query.title!.toLowerCase()}'))`
		}
		return urlQuery;
	}

	/**
	 * Cast the response of a single paper return by the microsoftApi to a ApiPaper object
	 * Used to get normalized result of all apis.
	 *
	 * @param response - single object return for a single paper by the microsoftApi
	 * @returns normalized ApiPaper object.
	 */
	private _parseResponse(response: any): IApiPaper {
		//logger.debug(response);
		let refLength: number = (response.RId ? response.RId.length : 0);

		let parsedAuthors: IApiAuthor[] = [];
		for (let a of response.AA) {
			let parsedAuthor: IApiAuthor = {
				id: undefined,
				orcid: [],
				rawString: [a.AuN],
				lastName: [],
				firstName: [],
			}
			parsedAuthors.push(parsedAuthor);
		}

		let parsedUniqueIds: IApiUniqueId[] = [];
		response.DOI && parsedUniqueIds.push(
			{
				id: undefined,
				type: idType.DOI,
				value: response.DOI ? response.DOI : undefined,
			} as IApiUniqueId
		)
		response.Id && parsedUniqueIds.push(
			{
				id: undefined,
				type: idType.MicrosoftAcademic,
				value: response.Id ? response.Id : undefined,
			} as IApiUniqueId
		)

		let parsedResponse: IApiPaper = {
			id: undefined,
			title: response.Ti ? [response.Ti] : [],
			author: parsedAuthors,
			abstract: response.IA ? MicrosoftResearchApi._convertInvertedAbstract(response.IA) : [],
			numberOfReferences: refLength ? [refLength] : [],
			numberOfCitations: response.CC ? [response.CC] : [],
			year: response.Y ? [Number(response.Y)] : [],
			publisher: response.PB ? [response.PB] : [],
			type: response.Pt ? [this._paperTypeMapper[response.Pt]] : [],
			scope: [],
			scopeName: [],
			pdf: response.S ? response.S.map((item: any) => item.U) : [],
			uniqueId: parsedUniqueIds,
			source: [SourceApi.MA],
			raw: []
		};
		return parsedResponse;
	}

	public async getDoi(query: IApiQuery): Promise<string | undefined> {
		try {
			if (!(query.title && query.title.trim().length > 0)) {
				throw new Error("If no DOI is given, we need atleast a title for the search.");
			}
			let urlQuery = `Composite(AA.AuN='${query.rawName!.toLowerCase()}')`;
			if (query.rawName) {
				urlQuery = `And(Composite(AA.AuN='${query.rawName!.toLowerCase()}'), Ti='${query.title!.toLowerCase()}')`;
			}
			let response = await fetch(this.url, {
				method: 'POST',
				headers: this._headers,
				body: JSON.stringify({
					expr: urlQuery,
					attributes: this._attributes
				})
			})
			let json = await response.json();
			//logger.debug(json);
			//logger.debug(json.entities[0].DOI)
			if (json.entities[0].DOI) {
				return json.entities[0].DOI;
			}
			else {
				throw new Error("Fetched object has no DOI");
			}
		}
		catch (e) {
			logger.warning(`MA: Couldnt fetch DOI for the following query: ${query}`);
			logger.warning(e);
		}
		return undefined;
	}

}