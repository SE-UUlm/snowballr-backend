import { assign, isEqual, makePromise } from "../helper/assign.ts";
import { CrossRefApi } from "./crossRefApi.ts";
import { IApiBatcher, IApiBatch, BatcherStatus } from "./iApiBatcher.ts";
import { IApiFetcher } from "./iApiFetcher.ts";
import { SourceApi } from "./iApiPaper.ts";
import { IApiQuery } from "./iApiQuery.ts";
import { IApiResponse } from "./iApiResponse.ts";
import { IeeeApi } from "./ieeeApi.ts";
import { MicrosoftResearchApi } from "./microsoftResearchApi.ts";
import { OpenCitationsApi } from "./openCitationsApi.ts";
import { SemanticScholar } from "./semanticScholar.ts";
import { ApiMerger } from "./apiMerger.ts";
import { logger } from "./logger.ts";
import { Cache, CacheType } from "./cache.ts";
import { GoogleScholar } from "./googleScholar.ts";
import { CONFIG } from "../helper/config.ts";
import { sleep } from "https://deno.land/x/sleep@v1.2.0/sleep.ts";


/**
 * Contains all data for a single batch fetching a query.
 * Can be subscribed so a long requests is only run once even if multiple frontend calls are received.
 */
export const ApiBatch: IApiBatch = {
	id: undefined,
	subscribers: [],
	status: BatcherStatus.D,
	response: makePromise([]),
	addSubscriber(subscriber: IApiQuery): void {
		this.subscribers.push(subscriber);
	}
}



type SourceApiToCache = {
	[key: string]: Cache<IApiResponse>;
}

// Map a class declaration to the api type so it can be implemented in a loop. ADD NEW CLASS HERE IF IMPLEMENTED TO BE APPLIED
export const APIMAPPER = {
	[SourceApi.MA]: MicrosoftResearchApi,
	[SourceApi.CR]: CrossRefApi,
	[SourceApi.OC]: OpenCitationsApi,
	[SourceApi.S2]: SemanticScholar,
	[SourceApi.IE]: IeeeApi,
	[SourceApi.GS]: GoogleScholar
}

// Map a constructor parameters to the api type so it can be implemented in a loop. ADD NEW PARAMETES HERE IF NEW CLASS IMPLEMENTED TO BE APPLIED
export const APIPARAMMAPPER = {
	[SourceApi.MA]: CONFIG.microsoftAcademic.baseUrl,
	[SourceApi.OC]: CONFIG.openCitations.baseUrl,
	[SourceApi.CR]: CONFIG.crossRef.baseUrl,
	[SourceApi.S2]: CONFIG.semanticScholar.baseUrl,
	[SourceApi.IE]: CONFIG.ieee.baseUrl,
	[SourceApi.GS]: CONFIG.googleScholar.baseUrl
}

/**
 * Singleton managing all active fetches. Allowing to return the same request fetch to multiple queries. 
 * Eg if diffent users in the frontend look for the same paper at the same time 
 */
export class ApiBatcher implements IApiBatcher {
	public cache: SourceApiToCache = {};  //= new Cache(true, false, 3000000);
	public activeBatches: IApiBatch[];

	public constructor() {
		this.activeBatches = []
		for (let s in APIMAPPER) {
			this.cache[s] = new Cache<IApiResponse>(CONFIG.cache.type, CONFIG.cache.timeToLiveInSeconds, s.toString());
		}
	}

	/**
	 * Start fetching a query. Batch is added to public var activeBatches.
	 * @param query Query to fetch
	 * @return apiBatch An open apiBatch object with the open, subscribable fetch promise of the apis 
	 */
	public startFetch(query: IApiQuery): Promise<IApiBatch> {

		let apiBatch = {} as IApiBatch;
		assign(apiBatch, ApiBatch);
		apiBatch.id = crypto.randomUUID();
		apiBatch.subscribers = [query];
		apiBatch.status = BatcherStatus.R;

		apiBatch.response = this._mergerWorker(query); //.then((data) => {

		this.activeBatches.push(apiBatch);
		return makePromise(apiBatch);
	}

	private async _mergerWorker(query: IApiQuery): Promise<IApiResponse[]> {
		let done = false;
		let initialized = false;
		let data: IApiResponse[];

		const worker = new Worker(new URL("worker.ts", import.meta.url).href, { type: 'module', deno: { namespace: true, permissions: "inherit" } });
		let mergedPapers: Promise<IApiResponse[]> = new Promise<IApiResponse[]>((resolve) => {
			const checkIfDone = () => {
				if (done) {
					resolve(data)
				}
				else setTimeout(checkIfDone, CONFIG.batcher.workerPollingRateInMilSecs);
			};
			checkIfDone();
		});
		let workerInit: Promise<void> = new Promise<void>((resolve) => {
			const checkIfDone = () => {
				if (initialized) {
					resolve(worker.postMessage({ query: JSON.stringify(query), type: 'run' }))
				}
				else {
					setTimeout(checkIfDone, CONFIG.batcher.workerPollingRateInMilSecs);
				}
			};
			checkIfDone();
		});

		worker.addEventListener('message', message => {
			//console.log("INCOMING MESSAGE " + message.data.type)
			switch (message.data.type) {
				case 'finished':
					data = JSON.parse(message.data.payload);
					done = true;
					break;
				case 'initialized':
					initialized = true;
					break;
				case 'getCache': {
					const cacheName: SourceApi = message.data.cacheName;
					const queryHash: string = message.data.queryHash;
					//console.log("-----QUERYHASH: " + queryHash);
					const res: IApiResponse | undefined = this.cache[cacheName].get(queryHash);
					//console.log("-----RES: " + res);
					worker.postMessage({ type: 'returningCache', cacheName: cacheName, queryHash: queryHash, cachePayload: JSON.stringify(res) });
					break;
				}
				case 'addCache': {
					const cacheName = message.data.cacheName;
					const queryHash: string = message.data.queryHash;
					const cachePayload = JSON.parse(message.data.cachePayload);
					this.cache[cacheName].add(queryHash, cachePayload);
					break;
				}
			}
		})

		await workerInit;

		return mergedPapers;
	}

	/**
	 * Stop a running fetch of a batch object
	 * @param batch list of enabled api enums
	 * @return list of IApiFetcher instances
	 */
	public stopFetch(batch: IApiBatch): boolean {
		let included = this.activeBatches.some(item => item.id === batch.id)
		if (included) {
			this.activeBatches = this.activeBatches.filter(item => !(item.id === batch.id))
			return true;
		}
		return false;
	}

	/**
	 * Subscribes a query object to a active, running batch to receive its results.
	 * @param query object to be subscribed.
	 * @returns the batch instance thq query is subscribed to now. if there is no fitting active fetch undefined is returned.
	 */
	public subscribeActiveFetch(query: IApiQuery): IApiBatch | undefined {
		let included = this.activeBatches.filter(item => isEqual(item, query))
		if (included.length > 0) {
			included[0].addSubscriber(query)
			return included[0]
		}
	}

	public activeBatchLength() {
		return this.activeBatches.length
	}

	/**
	 * Kills api batcher. InMemoryCache is deleted but fileCache is kept, yet it's ttl eventHandling is stopped.
	 */
	public kill() {
		Object.keys(this.cache).forEach(key => this.cache[key].clear());
		logger.info("Killed all Caches");
	}

	public purge() {
		this.kill()
		Object.keys(this.cache).forEach(key => { if (this.cache[key].fileCache) { this.cache[key].fileCache!.purge() } });

		logger.info("Killed all Caches");
	}
	public async register(query: IApiQuery): Promise<IApiBatch> {
		let active = this.subscribeActiveFetch(query);
		if (active) { return makePromise(active) }
		return await this.startFetch(query);
	}
}

