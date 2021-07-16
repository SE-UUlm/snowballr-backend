import { IApiQuery } from "./iApiQuery.ts";
import { IApiResponse } from "./iApiResponse.ts";
import { IApiFetcher } from "./iApiFetcher.ts";
import { IApiPaper, SourceApi } from './iApiPaper.ts';
import { logger } from "./logger.ts";
import { IApiAuthor } from "./iApiAuthor.ts";
import { IApiUniqueId, idType } from "./iApiUniqueId.ts";
import axiod from "https://deno.land/x/axiod/mod.ts";

export class IeeeApi implements IApiFetcher {
	url: string;
	private _token: string;
	private _config: {} = {};
	private _paperReferences: number = 0;

	public constructor(url: string, token: string) {
		logger.info("IEEE initialized");
		this.url = url;
		this._token = token;
	}

	/**
	 * Checks for a paper at the ieee research api via a query object.
	 * Since IEEE is daily rate limited and we dont get any information or refs and cites we only use it to fetch metadata of the original paper searched.
	 *
	 * @param query - Object defined by interface in IApiQuery to filter and query api calls.
	 * @returns Object containing the fetched paper and all paperObjects from citations and references. Promise.
	 */
	public async fetch(query: IApiQuery): Promise<IApiResponse> {
		var paper: IApiPaper = {};
		let citations: Promise<IApiPaper[]> | undefined;
		let references: Promise<IApiPaper[]> | undefined;
		try {
			//logger.debug("here")
			if (query.doi) {
				logger.debug(`Fetching IEEE by DOI: ${query.doi}`);
				var response = await fetch(`${this.url}/?apikey=${this._token}&format=json&max_records=25&start_record=1&sort_order=asc&sort_field=article_title&doi=${query.doi}`);
			}
			else if (query.rawName && query.title) {
				logger.debug(`Fetching IEEE by title and author: ${query.title} | ${query.rawName}`);
				var response = await fetch(`${this.url}/?apikey=${this._token}&format=json&max_records=25&start_record=1&sort_order=asc&sort_field=article_title&author=${query.rawName}&title=${query.title}`);
			}
			else {
				logger.critical("Query not fetchable by IEEE. Need either the DOI or (title and author).");
				return {} as IApiResponse;
			}
			let json = await response.json();
			if (!json.articles) {
				throw new Error("Paper not found by IEEE");
			}
			//logger.debug(json);
			this._config = {
				params: {},
				headers: {
					'Accept': 'application/json',
					'User-Agent': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/13.0.3 Safari/605.1.15',
					'Referer': json.articles[0].html_url
				},
				//validateStatus: status) => true
			}

			paper = this._parseResponse(json.articles[0]);

			var apiReturn: IApiResponse = {
				"paper": paper,
				"citations": [], //await this._getCitationsFromHtml(json.articles[0].html_url),
				"references": await this._getReferencesFromHtml(json.articles[0].html_url)
			}
		}
		catch (e) {
			logger.critical(`IEEE - Failed to fetch Query | ${e}`);
			var apiReturn: IApiResponse = {
				"paper": paper,
				"citations": citations ? await citations : [],
				"references": references ? await references : []
			}
		}
		return apiReturn;
	}

	private async _getCitationsFromHtml(url: string): Promise<IApiPaper[]> {
		logger.debug(url);
		let citations: IApiPaper[] = [];
		const response = await axiod.get(`${url.replace('document', 'rest/document')}citations`, this._config);
		logger.debug(response.data);
		let citationIDs = response.data.paperCitations.ieee.map((item: any) => item.links.documentLink.replace("/document/", ''));
		//citationIDs.concat(response.data.paperCitations.nonIeee.map((item: any) => item.publicationNumber));
		logger.debug(citationIDs);
		for (let c in response.data.paperCitations.ieee) {
			let rawMetadata = {};
			citations.push(this._parseResponse(rawMetadata));
		}
		return citations;
	}

	private async _getReferencesFromHtml(url: string): Promise<IApiPaper[]> {
		logger.debug(url);
		let references: IApiPaper[] = [];

		const response = await axiod.get(`${url.replace('document', 'rest/document')}references`, this._config);
		this._paperReferences = response.data.references ? response.data.references.length : 0;

		for (let r in response.data.references) {
			logger.debug(r);
			logger.debug(response.data.references[r]);
			let data = response.data.references[r];
			let regexAuthors = new RegExp(/([A-ZÀ-Ú]\. )+(([A-ZÀ-Ú][a-zà-ú]*)-*)+/g);
			let regexYear = new RegExp(/(?<!pp\. |-)[\d]{4}/g);
			//let publisherRegex = new RegExp();
			let text = String(data.text).split(data.title);
			let replacement = ",";
			//logger.debug(data.text.match(regexAuthors).map((item: any) => { return { 'fullname': item } }));
			let rawMetadata = {};
			if (data.title) {
				let text = String(data.text).split(data.title);
				let authors = text[0].match(regexAuthors);
				let year = text[1].match(regexYear);
				rawMetadata = {
					'title': data.title,
					'authors': {
						//'authors': data.title ? text[0].replace(/,([^,]*)$/, '$1').replace(/,* *(and)/, ',').split(',').map((item: any) => { return { 'fullname': item.trim() } }) : []
						'authors': authors ? authors.map((item: any) => { return { 'full_name': item } }) : []
					},
					'year': year ? year.slice(-1)[0] : undefined,
					'publisher': data.text[1].split(',') ? text[1].split(',')[0].trim() : undefined,
					'pdf_url': data.googleScholarLink ? data.googleScholarLink : undefined,
				}
			}
			else {
				rawMetadata = {
					'raw': data.text
				};
			}
			logger.debug(rawMetadata);
			references.push(this._parseResponse(rawMetadata));
		}
		//logger.debug(references);
		return references;

		// let citationIDs = response.data.references.ieee.map((item: any) => item.links.documentLink.replace("/document/", ''));
		//citationIDs.concat(response.data.paperCitations.nonIeee.map((item: any) => item.publicationNumber));
		//logger.debug(response);
		// for (let cite in response.data.paperCitations.ieee) {

		// }
	}


	/**
	 * Cast the response of a single paper return by the IEEE to a ApiPaper object
	 * Used to get normalized result of all apis.
	 *
	 * @param response - single object return for a single paper by the IEEE
	 * @returns normalized ApiPaper object.
	 */
	private _parseResponse(response: any): IApiPaper {
		//logger.debug(response);

		let parsedAuthors: IApiAuthor[] = [];
		//logger.info(response.authors.authors);
		//logger.info(response.year)
		//logger.info(response.title)
		if (response.authors && response.authors.authors) {
			for (let a of response.authors.authors) {
				console.error("hier" + JSON.stringify(a, null, 2))
				let parsedAuthor: IApiAuthor = {
					id: [],
					orcid: [],
					rawString: [a.full_name],
					lastName: [],
					firstName: [],
				}
				parsedAuthors.push(parsedAuthor);
			}
		}

		let parsedUniqueIds: IApiUniqueId[] = [];
		response.doi && parsedUniqueIds.push(
			{
				id: undefined,
				type: idType.DOI,
				value: response.doi ? response.doi : undefined,
			} as IApiUniqueId
		)
		response.partnum && parsedUniqueIds.push(
			{
				id: undefined,
				type: idType.IEEE,
				value: response.partnum ? response.partnum : undefined,
			} as IApiUniqueId
		)

		response.isbn && parsedUniqueIds.push(
			{
				id: undefined,
				type: idType.ISBN,
				value: response.isbn ? response.isbn : undefined,
			} as IApiUniqueId
		)

		response.issn && parsedUniqueIds.push(
			{
				id: undefined,
				type: idType.ISSN,
				value: response.issn ? response.issn : undefined,
			} as IApiUniqueId
		)

		let parsedResponse: IApiPaper = {
			id: undefined,
			title: response.title ? [response.title] : [],
			author: parsedAuthors,
			abstract: response.abstract ? [response.abstract] : [],
			numberOfReferences: response.references ? [response.references.length] : [],
			numberOfCitations: response.citing_paper_count ? [response.citing_paper_count] : [],
			year: response.publication_year ? [Number(response.publication_year)] : [],
			publisher: response.publisher ? [response.publisher] : [],
			type: undefined,
			scope: response.content_type ? [response.content_type] : [],
			scopeName: response.publication_title ? [response.publication_title] : [],
			pdf: response.pdf_url ? [response.pdf_url] : undefined,
			uniqueId: parsedUniqueIds,
			source: [SourceApi.IE],
			raw: response.raw ? [response.raw] : []
		};
		return parsedResponse;
	}
}