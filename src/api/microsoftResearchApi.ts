import { IApiQuery } from "./iApiQuery.ts";
import { IApiResponse } from "./iApiResponse.ts";
import { IApiFetcher } from "./iApiFetcher.ts";
import { IApiPaper, sourceApi } from './iApiPaper.ts';
import { logger } from "./logger.ts";
import { IApiAuthor } from "./iApiAuthor.ts";
import { IApiUniqueId, idType } from "./iApiUniqueId.ts";

export class MicrosoftResearchApi implements IApiFetcher {
	url: string;
	private _authToken: string;
	private _headers: {};
	private _attributes: string;
	private _paperTypeMapper: string[];
	private _queryAttributeMapper: {};

	public constructor(url: string, authToken: string) {
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
		var paper: IApiPaper = {};
		var citations: Promise<IApiPaper[]> | undefined;
		let references: Promise<IApiPaper[]> | undefined;
		try {
			let response = await fetch(this.url, {
				method: 'POST',
				headers: this._headers,
				body: JSON.stringify({
					expr: this._parseQuery(query),
					attributes: this._attributes
				})
			})
			let json = await response.json();
			paper = this._parseResponse(json.entities[0]);
			citations = json.entities[0] && this._getCitations(json.entities[0].Id);
			//logger.debug(json.entities[0].RId)
			// references = (json.entities[0] && json.entities[0].RId > 0) && this._getReferences(json.entities[0].RId);
			references = json.entities[0] && this._getReferences(json.entities[0].RId);
			//logger.debug(await references)
			var apiReturn: IApiResponse = {
				"paper": paper,
				"citations": await citations,
				"references": await references
			}
		}
		catch (e) {
			logger.critical(`MicrosoftResearchApi: Failed to fetch Query: ${e}`);
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
		let baseExpr: string = `Or(DOI='${query.id}', And(Composite(AA.AuN='${query.rawName.toLowerCase()}'), Ti='${query.title.toLowerCase()}'))`

		return baseExpr;
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
		let refLength: number = (response.RId === undefined ? 0 : response.RId.length);

		let parsedAuthors: IApiAuthor[] = [];
		for (let a of response.AA) {
			let parsedAuthor: IApiAuthor = {
				id: [],
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
			year: response.Y ? Number(response.Y) : undefined,
			publisher: response.PB ? [response.PB] : [],
			type: response.Pt ? this._paperTypeMapper[response.Pt] : undefined,
			scope: undefined,
			scopeName: undefined,
			pdf: response.S ? response.S.map((item: any) => item.U) : undefined,
			uniqueId: parsedUniqueIds,
			source: [sourceApi.MA]
		};
		return parsedResponse;
	}
}