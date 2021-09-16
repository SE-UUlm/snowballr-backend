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
import { DOMParser, Element } from 'https://deno.land/x/deno_dom/deno-dom-wasm.ts';
import { HttpUserAgents } from './httpUserAgent.ts';
import { sleep } from "https://deno.land/x/sleep/mod.ts";
import { getRandomFromRange } from "../helper/random.ts";
import { Semaphore } from "https://deno.land/x/semaphore/mod.ts"
import { difference } from 'https://deno.land/std/datetime/mod.ts'

const semaphore = new Semaphore(1);
var lastScrappingRun: Date;

export class GoogleScholar implements IApiFetcher {
	url: string;
	cache: Cache<IApiResponse> | undefined;
	//private _rateInterval:  = new Range();
	private _paperReferences: number = 0;
	private _domParser: DOMParser;

	public constructor(url: string, token: string, cache?: Cache<IApiResponse>) {
		logger.info("GoogleScholar initialized");
		this.url = url;
		this.cache = cache;
		this._domParser = new DOMParser();
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
			let rawHtml = await this._rateLimitedScrapeRequest(`${this.url}/scholar?as_sdt=0,5&q=${query.doi}&hl=en`);
			let html: any = this._domParser.parseFromString(rawHtml.data, 'text/html');


			// console.log(title.innerHTML);
			// console.log(info.innerHTML);
			// console.log(citedLink.innerHTML);
			// console.log(citedLink.attributes.href);

			paper = this._parseResponse(this._transformScrapedData(html.querySelector('#gs_res_ccl_mid > div')));

			var apiReturn: IApiResponse = {
				"paper": paper,
				"citations": await this._getCitations(html.querySelector('#gs_res_ccl_mid > div > div > div.gs_fl > a:nth-child(3)').attributes.href),
				"references": []
			}
			if (this.cache) {
				this.cache.add(queryString, apiReturn);
			};
		}
		catch (e) {
			logger.critical(`GoogleScholar - Failed to fetch Query | ${e}`);
			var apiReturn: IApiResponse = {
				"paper": paper,
				"citations": citations ? await citations : [],
				"references": references ? await references : []
			}
		}
		console.log(apiReturn);
		return apiReturn;
	}

	private _transformScrapedData(dom: any) {
		//console.log(dom);
		let title = dom.querySelector('.gs_ri > h3 > a');
		let info = dom.querySelector('.gs_a');
		let citedLink = dom.querySelector('.gs_fl > a:nth-child(3)');
		let splittedInfo = info.innerHTML.replace(/&nbsp;/g, ' ').split('-');
		let variousData = splittedInfo[1].split(',');

		return {
			'title': title.innerHTML,
			'author': splittedInfo[0],
			'publisher': variousData.length > 1 ? variousData[0].trim() : undefined,
			'year': variousData[variousData.length - 1].trim(),
			'citationCount': citedLink.innerHTML.split('Cited by ')[1]
		}
	}

	private async _rateLimitedScrapeRequest(url: string) {
		let axiodConfig = this._randomAxiodConfig();
		let timeout = getRandomFromRange(30, 45);
		let timeDelta = lastScrappingRun ? difference(lastScrappingRun, new Date()).seconds! : timeout;
		var release = await semaphore.acquire();
		if (timeDelta < timeout) {
			logger.info(`GoogleScholar: globally ratelimiting requests. Waiting ${timeout} seconds...`)
			await sleep(timeout - timeDelta);
		}
		let html = await axiod.get(url, axiodConfig);
		lastScrappingRun = new Date();
		release();
		return html;
	}

	private async _getCitations(url: string): Promise<IApiPaper[]> {
		let citations: IApiPaper[] = [];
		console.log(url);
		let timeout = getRandomFromRange(30, 45);
		//logger.info(`GoogleScholar: locally ratelimiting requests. Waiting ${timeout} seconds...`)
		//await sleep(timeout);
		let rawHtml = await this._rateLimitedScrapeRequest(`${this.url}${url}`);
		let html: any = this._domParser.parseFromString(rawHtml.data, 'text/html');
		let citationList = html.querySelector('#gs_res_ccl_mid');
		//console.log(citationList);
		citationList.childNodes.forEach((element: any) => {
			console.log("child")
			//console.log(element);
			if (element instanceof Element) {
				console.log("adding element")
				let citation = this._transformScrapedData(element);
				citations.push(this._parseResponse(citation));
			}
		});
		return citations;
	}

	private _randomAxiodConfig(): Object {
		return {
			params: {},
			headers: {
				'Accept': 'application/json',
				'User-Agent': HttpUserAgents[Math.floor(Math.random() * HttpUserAgents.length)],
				//'Referer': json.articles[0].html_url
			},
			//validateStatus: status) => true
		}
	}

	/**
	 * Cast the response of a single paper return by the Google Scholar to a ApiPaper object
	 * Used to get normalized result of all apis.
	 *
	 * @param response - single object return for a single paper by the Google Scholar api
	 * @returns normalized ApiPaper object.
	 */
	private _parseResponse(response: any): IApiPaper {
		let parsedAuthors: IApiAuthor[] = [];

		for (let a of response.author.split(',')) {
			let authorLinkText = this._domParser.parseFromString(a, 'text/html')!.querySelector('a');
			let parsedAuthor: IApiAuthor = {
				id: undefined,
				orcid: [],
				rawString: authorLinkText && authorLinkText.innerHTML ? [authorLinkText.innerHTML.trim()] : [a.trim()],
				lastName: [],
				firstName: [],
			}
			parsedAuthors.push(parsedAuthor);
		}

		let parsedResponse: IApiPaper = {
			id: undefined,
			title: response.title ? [response.title] : [],
			author: parsedAuthors,
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
		logger.warning(`GS: Not able to fetch without DOI`);
		return undefined;
	}
}