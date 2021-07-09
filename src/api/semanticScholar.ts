import { IApiQuery } from "./iApiQuery.ts";
import { IApiResponse } from "./iApiResponse.ts";
import { IApiFetcher } from "./iApiFetcher.ts";
import { IApiPaper, sourceApi } from './iApiPaper.ts';
import { logger } from "./logger.ts";
import { IApiAuthor } from "./iApiAuthor.ts";
import { IApiUniqueId, idType } from "./iApiUniqueId.ts";

export class SemanticScholar implements IApiFetcher {
	url: string;

	public constructor(url: string) {
		logger.info("SemanticScholar initialized");
		this.url = url;
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
		var RIds: number[];
		var references: IApiPaper[];

		let response = fetch(`${this.url}/${query.id}`)
			.then(data => {
				return data.json();
			})
			.then(data => {
				//logger.debug(data);
				paper = this._parseResponse(data);
				citations = this._getChildren(data.citations);
				references = this._getChildren(data.references);
			})
			.then(data => {
				let apiReturn: IApiResponse = {
					"paper": paper,
					"citations": citations,
					"references": references
				}
				return apiReturn;
			})
			.catch(data => {
				logger.error("Error while fetching semanticScholar: " + data);
				return {
					"paper": paper ? paper : undefined,
					"citations": citations ? citations : undefined,
					"references": references ? references : undefined
				} as IApiResponse;
			})
		return response;
	}

	/**
	 * Parse all microsoft-id references from a single paper and query for all the ids to return a paperObject for each.
	 *
	 * @param microsoftIds - list of microsoft-ids provided by the source-paper in key RId
	 * @returns list of paperObjects containing the references. Promise.
	 */
	private _getChildren(children: []): IApiPaper[] {
		let parsedChildren: IApiPaper[] = [];
		for (let child in children) {
			let parsedChild = this._parseResponse(children[child]);
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

		let parsedAuthors: IApiAuthor[] = [];
		for (let a of response.authors) {
			let parsedAuthor: IApiAuthor = {
				id: undefined,
				orcid: undefined,
				rawString: a.name,
				lastName: undefined,
				firstName: undefined,
			}
			parsedAuthors.push(parsedAuthor);
		}

		let parsedUniqueIds: IApiUniqueId[] = [];
		parsedUniqueIds.push(
			{
				id: undefined,
				type: idType.DOI,
				value: response.doi ? response.doi : undefined,
			} as IApiUniqueId
		)
		parsedUniqueIds.push(
			{
				id: undefined,
				type: idType.SemanticScholar,
				value: response.paperId ? response.paperId : undefined,
			} as IApiUniqueId
		)

		let parsedResponse: IApiPaper = {
			id: undefined,
			title: response.title ? [response.title] : [],
			author: parsedAuthors,
			abstract: response.abstract ? [response.abstract] : [],
			numberOfReferences: response.references ? [response.references.length] : [],
			numberOfCitations: response.citations ? [response.citations.length] : [],
			year: response.year ? Number(response.year) : undefined,
			publisher: undefined,
			type: undefined,
			scope: undefined,
			scopeName: response.venue ? [response.venue] : undefined,
			pdf: response.url ? [response.url] : undefined,
			uniqueId: parsedUniqueIds,
			source: [sourceApi.S2]
		};
		return parsedResponse;
	}
}