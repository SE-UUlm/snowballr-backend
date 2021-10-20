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
import { proxyPool, Proxy } from './proxyPool.ts'
import { CONFIG } from "../helper/config.ts";
import { warnApiDisabledByConfig } from "../helper/error.ts";

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
	private _proxy: Proxy | undefined;
	private _retries: number = 0;

	public constructor(url: string, token: string, cache?: Cache<IApiResponse>) {
		logger.info("GoogleScholar initialized");
		this.url = url;
		this.cache = cache;
		this._domParser = new DOMParser();
		this._userAgent = HttpUserAgents[Math.floor(Math.random() * HttpUserAgents.length)];
	}

	/**
	 * Checks for a paper at the googleScholar website via a sequence of scrape requests.
	 * Since google scholar is highly blocked for automated requests we use a proxy to hide our identity.
	 *
	 * @param query - Object defined by interface in IApiQuery to filter and query api calls.
	 * @returns Object containing the fetched paper and all paperObjects from citations and references. Promise.
	 */
	public async fetch(query: IApiQuery): Promise<IApiResponse> {
		if (!CONFIG.googleScholar.enabled) { return warnApiDisabledByConfig("GoogleScholar"); }
		var paper: IApiPaper = {} as IApiPaper;
		let citations: Promise<IApiPaper[]> | undefined;
		let references: Promise<IApiPaper[]> | undefined;
		let queryIdentifier = createHash("sha3-256");
		queryIdentifier.update(JSON.stringify(query));
		let queryString = queryIdentifier.toString();
		try {
			var get = this.cache?.get(queryString);
			if (CONFIG.googleScholar.useCache && this.cache && get) {
				logger.info(`GS: Loaded fetch from cache.`)
				return get;
			}

			// use a random user-agent to make it harder to get rate limited.
			let rawHtml = await this._rateLimitedScrapeRequest(`${this.url}/scholar?as_sdt=0,5&q=${query.doi}&hl=en`, true);
			let html: any = this._domParser.parseFromString(rawHtml, 'text/html');

			paper = this._parseResponse(this._transformScrapedData(html.querySelector('#gs_res_ccl_mid > div')));

			try {
				var citationUrl = html.querySelector('#gs_res_ccl_mid > div > div > div.gs_fl > a:nth-child(3)').attributes.href;
				var citationCount = Number(html.querySelector('#gs_res_ccl_mid > div > div > div.gs_fl > a:nth-child(3)').innerHTML.replace('Cited by ', ''));
			}
			catch (e) {
				var citationUrl = undefined;
				var citationCount = 0;
				logger.warning(`GS: Couldn't find any Citations!`);
			}
			let citations = this._getCitations(citationUrl, citationCount);
			//console.log(JSON.stringify(await citations, function (k, v) { return v === undefined ? null : v; }, 2));
			var apiReturn: IApiResponse = {
				"paper": paper,
				"citations": citationUrl && citationCount ? await citations : [],
				"references": []
			}
			if (this.cache) {
				//console.log('GS: Adding query to cache')
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
		finally {
			this._proxy?.close();
		}
		return apiReturn;
	}

	/**
	 * Function to select relevant data for paper from a parsed dom tree to simulate som kind of json api return, which can be parsed to an IApiPaper with ease.
	 *
	 * @param dom - parsed subtree of a single paper or citation from the googleScholar scrap
	 * @returns A json like object with all relevant metadata scrapable from googleScholar for a single paper
	 */
	private _transformScrapedData(dom: any) {
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

		return {
			'title': title ? title.innerHTML : undefined,
			'author': splittedInfo ? splittedInfo[0] : undefined,
			'publisher': variousData && variousData.length > 1 ? variousData[0].trim() : undefined,
			'year': variousData ? variousData[variousData.length - 1].trim() : undefined,
			'citationCount': citedLink.innerHTML.split('Cited by ')[1],
			'pdf': pdfUrl ? pdfUrl.attributes.href : undefined
		}
	}

	/**
	 * Scrap google scholar via fetch call but only once at an interval to prevent bot detection.
	 *
	 * @param url - url to fetch google, might be more complex for citations
	 * @param refererNeeded - if this side would be clicked from a previous http site we use the previous url as referer to simulate user interaction
	 * @returns a raw html string, unparsed
	 */
	private async _rateLimitedScrapeRequest(url: string, refererNeeded?: boolean): Promise<string> {
		if (!this._proxy) {
			this._proxy = await proxyPool.acquire();
		}
		let timeout = getRandomFromRange(CONFIG.googleScholar.requestInterval.min, CONFIG.googleScholar.requestInterval.max);
		let timeDelta = lastScrappingRun ? difference(lastScrappingRun, new Date()).seconds! : timeout;

		// critical section. stop concurrency by locking with a mutex so that the ratelimiting works globally when multiple batches are running in parallel
		var release = await semaphore.acquire();
		if (timeDelta < timeout) {
			logger.warning(`GoogleScholar: globally ratelimiting requests. Waiting ${timeout} seconds...`)
			await sleep(timeout - timeDelta);
		}
		let html = await fetch(url, this._proxy!.getFetchConfig(this._lastRefererUrl, this._currentCookie));
		//console.log(html)
		lastScrappingRun = new Date();
		release();
		// leave critical section

		//detect connection problems like being blocked and exchange proxy if so
		if (html.status !== 200) {
			if (this._retries < 50) {
				await this._rotateProxy(`GS:Site blocked or not reachable. Http status code ${html.status}. Retry #${this._retries}.`);
				return this._rateLimitedScrapeRequest(url, refererNeeded ? refererNeeded : undefined)
			}
			else {
				throw new Error(`GS: Invalid http response from google ${html.status}`);
			}
		}
		let body = await html.text();
		//console.log(body)
		let parsed: any = this._domParser.parseFromString(body, 'text/html');

		if (!this._currentCookie) { this._currentCookie = html.headers.get('set-cookie')!; }
		if (refererNeeded) {
			logger.info(`Setting referer for future requests: ${parsed.url}`)
			this._lastRefererUrl = parsed.url!;
		}
		else {
			this._lastRefererUrl = undefined;
		}

		// detect captcha and change proxy if so
		if (parsed.querySelector('#gs_captcha_c')) {
			if (this._retries < 50) {
				await this._rotateProxy(`GS: Detected Captcha. Retry #${this._retries}.`);
				return this._rateLimitedScrapeRequest(url, refererNeeded ? refererNeeded : undefined)
			}
			else {
				throw new Error("GS: Captcha enabled by google scholar. Cannot fetch.");
			}
		}

		// reset retry counter since the last scrap seemed to wort (200 and no captcha detected)
		this._retries = 0;
		return body;
	}

	/**
	 * Exchange fetch config with proxy if proxy gets blocked or captchaed
	 *
	 * @param message - log message if proxy needs to be exchanged
	 */
	private async _rotateProxy(message: string): Promise<void> {
		this._lastRefererUrl = undefined;
		this._currentCookie = undefined;
		logger.warning(message)
		this._proxy = await proxyPool.exchange(this._proxy!);
		this._retries++;
	}

	/**
	 * Get a citations into an parsed array by fetching every page of the googleScholar citations.
	 *
	 * @param url - url to the first citations page of a paper
	 * @param numberOfCitations - numberOfCitations read from the initial paper page
	 * @returns fully scraped and parsed citations to compare by merger
	 */
	private async _getCitations(url: string, numberOfCitations: number): Promise<IApiPaper[]> {
		//console.log(numberOfCitations);
		let citations: IApiPaper[] = [];
		let currentPage = 0;

		// Even as a user we cannot see more than 1000 citations per paper
		if (numberOfCitations > (CONFIG.googleScholar.maxCitationCount >= 0 && CONFIG.googleScholar.maxCitationCount <= 1000 ? CONFIG.googleScholar.maxCitationCount : 1000)) {
			logger.warning(`GS: Found ${numberOfCitations} citations.But only shows max 1000 citations in detailed view.We will ignore the rest.`)
			numberOfCitations = 1000;
		}
		let iterations = (numberOfCitations / 20);
		logger.info(`GS: Got ${numberOfCitations} citations.This will need ${iterations} fetches`)

		// get each citation paper meta of the paper currently iterated. iteration works via manipulating the urls "start" param
		while (iterations > 0) {
			//console.log(iterations);
			iterations--;
			let rawHtml = await this._rateLimitedScrapeRequest(`${this.url}${url}&num=20&start=${currentPage}`);
			currentPage += 20;
			let html: any = this._domParser.parseFromString(rawHtml, 'text/html');
			let citationList = html.querySelector('#gs_res_ccl_mid');
			citationList.childNodes.forEach((element: any) => {
				if (element instanceof Element) {
					let citation = this._transformScrapedData(element);
					let cite = this._parseResponse(citation);
					//console.log(cite)
					citations.push(cite);
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