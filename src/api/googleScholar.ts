// deno-lint-ignore-file
import { IApiQuery } from "./iApiQuery.ts";
import { IApiResponse } from "./iApiResponse.ts";
import { IApiFetcher } from "./iApiFetcher.ts";
import { IApiPaper, SourceApi } from './iApiPaper.ts';
import { logger } from "./logger.ts";
import { IApiAuthor } from "./iApiAuthor.ts";
import { IApiUniqueId, idType } from "./iApiUniqueId.ts";
import axiod from "https://deno.land/x/axiod/mod.ts";
import { Cache } from "./cache.ts";
import { createHash } from "https://deno.land/std/hash/mod.ts";
import { DOMParser } from 'https://deno.land/x/deno_dom/deno-dom-wasm.ts';
import { HttpUserAgents } from './httpUserAgent.ts';
import { sleep } from "https://deno.land/x/sleep/mod.ts";
import { getRandomFromRange } from "../helper/random.ts";

var activeFetches = 0;

export class GoogleScholar implements IApiFetcher {
	url: string;
	cache: Cache<IApiResponse> | undefined;
	//private _rateInterval:  = new Range();
	private _paperReferences: number = 0;

	public constructor(url: string, token: string, cache?: Cache<IApiResponse>) {
		logger.info("GoogleScholar initialized");
		this.url = url;
		this.cache = cache;
	}

	/**
	 * Checks for a paper at the ieee research api via a query object.
	 * Since IEEE is daily rate limited and we dont get any information or refs and cites we only use it to fetch metadata of the original paper searched.
	 *
	 * @param query - Object defined by interface in IApiQuery to filter and query api calls.
	 * @returns Object containing the fetched paper and all paperObjects from citations and references. Promise.
	 */
	public async fetch(query: IApiQuery): Promise<IApiResponse> {
		var paper: IApiPaper = {} as IApiPaper;
		let citations: Promise<IApiPaper[]> | undefined;
		let references: Promise<IApiPaper[]> | undefined;
		let queryIdentifier = createHash("sha3-256");
		queryIdentifier.update(JSON.stringify(query));
		let queryString = queryIdentifier.toString();
		try {
			// let get = this.cache!.get(queryString);
			// if (this.cache && get) {
			// 	logger.info(`GoogleScholar: Loaded fetch from cache.`)
			// 	return get;
			// }

			// use a random user-agent to make it harder to get rate limited.
			let axiodConfig = {
				params: {},
				headers: {
					'Accept': 'application/json',
					'User-Agent': HttpUserAgents[Math.floor(Math.random() * HttpUserAgents.length)],
					//'Referer': json.articles[0].html_url
				},
				//validateStatus: status) => true
			}
			//console.log(this.url)
			//console.log(query.doi)
			if (activeFetches > 0) {
				let timeout = getRandomFromRange(30, 45);
				logger.info(`GS: Ratelimiting Requests. Waiting ${timeout} seconds...`)
				await sleep(timeout * activeFetches);
			}
			activeFetches++;
			let rawHtml = await axiod.get(`${this.url}/scholar?as_sdt=0,5&q=${query.doi}&hl=en`, axiodConfig);
			let html: any = new DOMParser().parseFromString(rawHtml.data, 'text/html');

			// console.log(title.innerHTML);
			// console.log(info.innerHTML);
			// console.log(citedLink.innerHTML);
			// console.log(citedLink.attributes.href);

			paper = this._parseResponse(this._transformScrapedData(html));

			var apiReturn: IApiResponse = {
				"paper": paper,
				"citations": [],
				"references": []
			}
			if (this.cache) {
				this.cache.add(queryString, apiReturn);
			};
		}
		catch (e) {
			logger.critical(`IEEE - Failed to fetch Query | ${e}`);
			var apiReturn: IApiResponse = {
				"paper": paper,
				"citations": citations ? await citations : [],
				"references": references ? await references : []
			}
		}
		finally {
			activeFetches--;
		}
		return apiReturn;
	}

	private _transformScrapedData(dom: any) {
		let content = dom.querySelector('#gs_res_ccl_mid > div');
		let title = content.querySelector('.gs_ri > h3 > a');
		let info = content.querySelector('.gs_a');
		let citedLink = content.querySelector('.gs_fl > a:nth-child(3)');
		let splittedInfo = info.innerHTML.replace(/&nbsp;/g, ' ').split('-');

		return {
			'title': title.innerHTML,
			'author': splittedInfo[0],
			'publisher': splittedInfo[1].split(',')[0].trim(),
			'year': splittedInfo[1].split(',')[1],
			'citationCount': citedLink.innerHTML.split('Cited by ')[1]
		}
	}

	// private async _getCitationsFromHtml(url: string): Promise<IApiPaper[]> {
	// 	//logger.debug(url);
	// 	let citations: IApiPaper[] = [];
	// 	const response = await axiod.get(`${url.replace('document', 'rest/document')}citations`, this._config);
	// 	//logger.debug(response.data);

	// 	let citationIDs = response.data.paperCitations.ieee.map((item: any) => item.links.documentLink.replace("/document/", ''));
	// 	//logger.debug(citationIDs);
	// 	citations.push.apply(citations, this._getCitationsTypeIeee(response.data.paperCitations.ieee));
	// 	citations.push.apply(citations, this._getCitationsTypeNonIeee(response.data.paperCitations.nonIeee));

	// 	return citations;
	// }

	// private _getCitationsTypeIeee(ieeeData: any): IApiPaper[] {
	// 	let citations: IApiPaper[] = [];
	// 	for (let c in ieeeData) {
	// 		let year = ieeeData[c].displayText.match(this._citeRegexYear);
	// 		let authors = ieeeData[c].displayText.split('\"')[0].match(this._citeRegexAuthors);
	// 		//logger.debug(authors)
	// 		let rawMetaData = {
	// 			'title': ieeeData[c].title ? ieeeData[c].title : ieeeData[c].displayText.split('\"')[1],
	// 			'authors': {
	// 				'authors': authors ? authors.map((item: any) => { return { 'full_name': item } }) : []
	// 			},
	// 			'year': year ? year.slice(-1)[0] : undefined,
	// 			'pdf_url': ieeeData[c].googleScholarLink ? ieeeData[c].googleScholarLink : undefined,
	// 		};
	// 		citations.push(this._parseResponse(rawMetaData));
	// 	}
	// 	return citations;
	// }

	// private _getCitationsTypeNonIeee(othersData: any): IApiPaper[] {
	// 	let citations: IApiPaper[] = [];
	// 	for (let c in othersData) {
	// 		let year = othersData[c].displayText.match(this._citeRegexYear);
	// 		let authors = othersData[c].displayText.split('<i>')[0].match(this._citeRegexAuthors);
	// 		//logger.debug(authors)
	// 		let rawMetaData = {
	// 			'title': othersData[c].title ? othersData[c].title : othersData[c].displayText.match(this._citeRegexTitle)[0],
	// 			'authors': {
	// 				'authors': authors ? authors.map((item: any) => { return { 'full_name': item } }) : []
	// 			},
	// 			'year': year ? year.slice(-1)[0] : undefined,
	// 			'pdf_url': othersData[c].googleScholarLink ? othersData[c].googleScholarLink : undefined,
	// 		};
	// 		citations.push(this._parseResponse(rawMetaData));
	// 	}
	// 	return citations;
	// }

	// private async _getReferencesFromHtml(url: string): Promise<IApiPaper[]> {
	// 	//logger.debug(url);
	// 	let references: IApiPaper[] = [];

	// 	const response = await axiod.get(`${url.replace('document', 'rest/document')}references`, this._config);
	// 	this._paperReferences = response.data.references ? response.data.references.length : 0;

	// 	for (let r in response.data.references) {
	// 		//logger.debug(r);
	// 		//logger.debug(response.data.references[r]);
	// 		let data = response.data.references[r];
	// 		let regexAuthors = new RegExp(/([A-ZÀ-Ú]\. )+(([A-ZÀ-Ú][a-zà-ú]*)-*)+/g);
	// 		let regexYear = new RegExp(/(?<!pp\. |-)[\d]{4}/g);
	// 		let text = String(data.text).split(data.title);
	// 		let replacement = ",";
	// 		let rawMetadata = {};
	// 		if (data.title) {
	// 			let text = String(data.text).split(data.title);
	// 			let authors = text[0].match(regexAuthors);
	// 			let year = text[1].match(regexYear);
	// 			rawMetadata = {
	// 				'title': data.title,
	// 				'authors': {
	// 					'authors': authors ? authors.map((item: any) => { return { 'full_name': item } }) : []
	// 				},
	// 				'year': year ? year.slice(-1)[0] : undefined,
	// 				'publisher': data.text[1].split(',') ? text[1].split(',')[0].trim() : undefined,
	// 				'pdf_url': data.googleScholarLink ? data.googleScholarLink : undefined,
	// 			}
	// 		}
	// 		else {
	// 			rawMetadata = {
	// 				'raw': data.text
	// 			};
	// 		}
	// 		//logger.debug(rawMetadata);
	// 		references.push(this._parseResponse(rawMetadata));
	// 	}
	// 	//logger.debug(references);
	// 	return references;
	// }


	/**
	 * Cast the response of a single paper return by the IEEE to a ApiPaper object
	 * Used to get normalized result of all apis.
	 *
	 * @param response - single object return for a single paper by the IEEE
	 * @returns normalized ApiPaper object.
	 */
	private _parseResponse(response: any): IApiPaper {

		let parsedResponse: IApiPaper = {
			id: undefined,
			title: response.title ? [response.title] : [],
			author: response.author ? [response.author] : [],
			abstract: [],
			numberOfReferences: [],
			numberOfCitations: response.citationCount ? [Number(response.citationCount)] : [],
			year: response.year ? [Number(response.year)] : [],
			publisher: response.publisher ? [response.publisher] : [],
			type: [],
			scope: [],
			scopeName: [],
			pdf: response.pdf_url ? [response.pdf_url] : [],
			uniqueId: [],
			source: [SourceApi.GS],
			raw: []
		};
		return parsedResponse;
	}

	public async getDoi(query: IApiQuery): Promise<string | undefined> {
		logger.warning(`IE: Not able to fetch without DOI`);
		return undefined;
	}
}