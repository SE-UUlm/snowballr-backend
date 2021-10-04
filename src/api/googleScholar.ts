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

/*Captcha Google Scholar Rate Limits Enabled
Fetching same Query, 30-45 sec: 3,3,4
Fetching same Query, 300-360 sec: 4,5,4

Fetching 10 citations pages, 30-60sec: -, 8, -, -
Fetching 10 citations pages, 300-360sec: -, -, -, -

Fetching 10 queries, 100times, 30-45sec: 23, 28, 13, 17
Fetching 10 queries, 100times, 300-360sec: 54, 48, 36
*/


const semaphore = new Semaphore(1);
var lastScrappingRun: Date;

export class GoogleScholar implements IApiFetcher {
	url: string;
	cache: Cache<IApiResponse> | undefined;
	//private _rateInterval:  = new Range();
	private _paperReferences: number = 0;
	private _domParser: DOMParser;
	private _lastRefererUrl: string | undefined;
	private _userAgent: Object;
	private _currentCookie: string | undefined = undefined;

	public constructor(url: string, token: string, cache?: Cache<IApiResponse>) {
		logger.info("GoogleScholar initialized");
		this.url = url;
		this.cache = cache;
		this._domParser = new DOMParser();
		this._userAgent = HttpUserAgents[Math.floor(Math.random() * HttpUserAgents.length)];
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
			let get = this.cache!.get(queryString);
			if (this.cache && get) {
				logger.info(`GS: Loaded fetch from cache.`)
				return get;
			}

			// use a random user-agent to make it harder to get rate limited.
			let rawHtml = await this._rateLimitedScrapeRequest(`${this.url}/scholar?as_sdt=0,5&q=${query.doi}&hl=en`, true);
			let html: any = this._domParser.parseFromString(rawHtml, 'text/html');

			// if (html.querySelector('#gs_captcha_c')) {
			// 	throw new Error("Captcha enabled by google scholar. Cannot fetch.");
			// }

			paper = this._parseResponse(this._transformScrapedData(html.querySelector('#gs_res_ccl_mid > div')));

			var apiReturn: IApiResponse = {
				"paper": paper,
				"citations": await this._getCitations(html.querySelector('#gs_res_ccl_mid > div > div > div.gs_fl > a:nth-child(3)').attributes.href, html.querySelector('#gs_res_ccl_mid > div > div > div.gs_fl > a:nth-child(3)').innerHTML.replace('Cited by ', '') as number),
				"references": []
			}
			if (this.cache) {
				console.log('GS: Adding query to cache')
				await this.cache.add(queryString, apiReturn);
			};
		}
		catch (e) {
			logger.warning(`GoogleScholar - Failed to fetch Query | ${e}`);
			var apiReturn: IApiResponse = {
				"paper": paper,
				"citations": citations ? await citations : [],
				"references": references ? await references : []
			}
		}
		//console.log(apiReturn);
		return apiReturn;
	}

	private _transformScrapedData(dom: any) {
		//console.log(dom);
		let title = dom.querySelector('.gs_ri > h3 > a') ? dom.querySelector('.gs_ri > h3 > a') : dom.querySelector('.gs_ri > h3 > span:not(.gs_ctu)');
		let info = dom.querySelector('.gs_a');
		let citedLink = dom.querySelector('.gs_fl > a:nth-child(3)');
		try {
			var splittedInfo = info.innerHTML.replace(/&nbsp;/g, ' ').replace(/<\/?[^>]+(>|$)/g, "").split(' - ');
			var variousData = splittedInfo[1].split(',');
		}
		catch (e) {
			var splittedInfo = undefined;
			var variousData = undefined;
		}
		let pdfUrl = dom.querySelector('div.gs_ggs.gs_fl > div > div > a');

		//console.log(splittedInfo);

		return {
			'title': title ? title.innerHTML : undefined,
			'author': splittedInfo ? splittedInfo[0] : undefined,
			'publisher': variousData && variousData.length > 1 ? variousData[0].trim() : undefined,
			'year': variousData ? variousData[variousData.length - 1].trim() : undefined,
			'citationCount': citedLink.innerHTML.split('Cited by ')[1],
			'pdf': pdfUrl ? pdfUrl.attributes.href : undefined
		}
	}

	private async _rateLimitedScrapeRequest(url: string, refererNeeded?: boolean) {
		let axiodConfig = this._randomFetchConfig();
		let timeout = getRandomFromRange(30, 60);
		let timeDelta = lastScrappingRun ? difference(lastScrappingRun, new Date()).seconds! : timeout;
		var release = await semaphore.acquire();
		if (timeDelta < timeout) {
			logger.warning(`GoogleScholar: globally ratelimiting requests. Waiting ${timeout} seconds...`)
			await sleep(timeout - timeDelta);
		}
		let html = await fetch(url, axiodConfig);
		//console.log(await html.text());
		let body = await html.text();

		//console.log(html.headers.get('set-cookie'));
		if (!this._currentCookie) { this._currentCookie = html.headers.get('set-cookie')!; }
		if (refererNeeded) {
			//logger.info(`Setting referer for future requests: ${html.config.url}`)
			//this._lastRefererUrl = html.config.url!;
		}
		else {
			this._lastRefererUrl = undefined;
		}
		lastScrappingRun = new Date();
		release();
		let parsed: any = this._domParser.parseFromString(body, 'text/html');

		if (parsed.querySelector('#gs_captcha_c')) {
			throw new Error("Captcha enabled by google scholar. Cannot fetch.");
		}
		return body;
	}

	private _randomFetchConfig(): Object {
		let config: any = {
			params: {},
			headers: {
				'accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9',
				'user-agent': this._userAgent,
				"accept-encoding": "gzip, deflate, br",
				"accept-language": "en-US,en;q=0.9,de;q=0.8",
				'referer': this._lastRefererUrl ? this._lastRefererUrl : 'https://www.google.com/',
				"sec-fetch-dest": "document",
				"sec-fetch-mode": "navigate",
				"sec-fetch-site": "same-origin",
				"sec-fetch-user": "?1",
				"upgrade-insecure-requests": 1
			},
			proxy: {
				url: 'http://localhost:5000',
				basicAuth: {
					username: 'username1',
					password: 'password1'
				}
			}
			//validateStatus: status) => true
		}
		if (this._currentCookie) { config.headers['cookie'] = this._currentCookie; }
		return config;
	}

	private async _getCitations(url: string, numberOfCitations: number): Promise<IApiPaper[]> {
		console.log(numberOfCitations);
		let citations: IApiPaper[] = [];
		let currentPage = 0;
		//let mod = (numberOfCitations % 20) != 0 ? 1 : 0;
		let iterations = (numberOfCitations / 20);
		logger.info(`GoogleScholar: Got ${numberOfCitations} citations. This will need ${iterations} fetches`)
		while (iterations > 0) {
			console.log(iterations);
			iterations--;
			//let timeout = getRandomFromRange(30, 45);
			//logger.info(`GoogleScholar: locally ratelimiting requests. Waiting ${timeout} seconds...`)
			//await sleep(timeout);
			let rawHtml = await this._rateLimitedScrapeRequest(`${this.url}${url}&num=20&start=${currentPage}`);
			currentPage += 20;
			let html: any = this._domParser.parseFromString(await rawHtml, 'text/html');
			let citationList = html.querySelector('#gs_res_ccl_mid');
			//console.log(citationList);
			citationList.childNodes.forEach((element: any) => {
				//console.log("child")
				//console.log(element);
				if (element instanceof Element) {
					//console.log("adding element")
					let citation = this._transformScrapedData(element);
					citations.push(this._parseResponse(citation));
				}
			});
		}
		return citations;
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

		if (response.author) {
			for (let a of response.author.split(',')) {
				//let authorLinkText = this._domParser.parseFromString(a, 'text/html')!.querySelector('a');
				if (a.replace(' ', '').length === 0) { continue; }
				let parsedAuthor: IApiAuthor = {
					id: undefined,
					orcid: [],
					rawString: [a.trim()],
					lastName: [],
					firstName: [],
				}
				parsedAuthors.push(parsedAuthor);
			}
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
			pdf: response.pdf ? [response.pdf] : [],
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