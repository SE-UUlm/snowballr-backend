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
import { logger } from "./logger.ts";
import { Cache } from "./cache.ts";
import { GoogleScholar } from "./googleScholar.ts";
import { CONFIG } from "../helper/config.ts";

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

/**
 * Singleton managing all active fetches. Allowing to return the same request fetch to multiple queries. 
 * Eg if diffent users in the frontend look for the same paper at the same time 
 */
export class ApiBatcher implements IApiBatcher {
	public cache: SourceApiToCache = {};  //= new Cache(true, false, 3000000);
	public activeBatches: IApiBatch[];

	// Map a class declaration to the api type so it can be implemented in a loop. ADD NEW CLASS HERE IF IMPLEMENTED TO BE APPLIED
	private _apiMapper = {
		[SourceApi.MA]: MicrosoftResearchApi,
		[SourceApi.CR]: CrossRefApi,
		[SourceApi.OC]: OpenCitationsApi,
		[SourceApi.S2]: SemanticScholar,
		[SourceApi.IE]: IeeeApi,
		[SourceApi.GS]: GoogleScholar
	}

	// Map a constructor parameters to the api type so it can be implemented in a loop. ADD NEW PARAMETES HERE IF NEW CLASS IMPLEMENTED TO BE APPLIED
	private _apiParamMapper = {
		[SourceApi.MA]: CONFIG.microsoftAcademic.baseUrl,
		[SourceApi.OC]: CONFIG.openCitations.baseUrl,
		[SourceApi.CR]: CONFIG.crossRef.baseUrl,
		[SourceApi.S2]: CONFIG.semanticScholar.baseUrl,
		[SourceApi.IE]: CONFIG.ieee.baseUrl,
		[SourceApi.GS]: CONFIG.googleScholar.baseUrl
	}


	public constructor() {
		this.activeBatches = []
		for (const s in this._apiMapper) {
			this.cache[s] = new Cache<IApiResponse>(CONFIG.cache.type, CONFIG.cache.timeToLiveInSeconds, s.toString());
		}
	}

	/**
	 * Start fetching a query. Batch is added to public var activeBatches.
	 * @param query Query to fetch
	 * @return apiBatch An open apiBatch object with the open, subscribable fetch promise of the apis 
	 */
	public async startFetch(query: IApiQuery): Promise<IApiBatch> {
		const initializedFetchers: IApiFetcher[] = this._initializeEnabledApis(query.enabledApis!);
		const response: IApiResponse[] = [];
		// try to prefetch a doi for the query objects by querying for the other variables. Only implemented on selected apis.
		if (!query.doi) {
			query = await this._getDoiByFetching(query, initializedFetchers);
		}
		for (const i in initializedFetchers) {
			response.push(await initializedFetchers[i].fetch(query));
		}
		//const merger = new ApiMerger(query.aggression);
		const apiBatch = {} as IApiBatch;
		assign(apiBatch, ApiBatch);
		apiBatch.id = crypto.randomUUID();
		apiBatch.subscribers = [query];
		apiBatch.status = BatcherStatus.R;
		// apiBatch.response = merger.compare(response).then((data) => {
		// 	apiBatch.status = BatcherStatus.F
		// 	this.stopFetch(apiBatch)
		// 	return data
		// })
		apiBatch.response = this._mergerWorker(query, response); //.then((data) => {
		// 	apiBatch.status = BatcherStatus.F
		// 	this.stopFetch(apiBatch)
		// 	return data
		// })
		console.log(await apiBatch.response);
		this.activeBatches.push(apiBatch);
		return apiBatch;
	}

	private async _mergerWorker(query: IApiQuery, response: IApiResponse[]) {
		let done = false;
		let initialized = false;
		let data: IApiResponse[];

		const worker = new Worker(new URL("worker.ts", import.meta.url).href, { type: 'module', deno: { namespace: true, permissions: "inherit" } });
		const mergedPapers: Promise<IApiResponse[]> = new Promise<IApiResponse[]>((resolve) => {
			const checkIfDone = () => {
				if (done) {
					resolve(data)
				}
				else setTimeout(checkIfDone, CONFIG.batcher.workerPollingRateInMilSecs);
			};
			checkIfDone();
		});
		const workerInit: Promise<void> = new Promise<void>((resolve) => {
			const checkIfDone = () => {
				if (initialized) {
					//console.log("WORKER START NOW")
					resolve(worker.postMessage(JSON.stringify({ query: query, response: response })))
				}
				else {
					//console.log("------------- WAITING FOR ANDREAS ------------")
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
			}
		})

		await workerInit;

		return mergedPapers;
	}

	/**
	 * Try to prefetch a doi for the query objects by querying for the other variables. Only implemented on selected apis.
	 * @param query Query to fetch
	 * @param initializedFetchers Initialied IApiFetcher implementations
	 * @return query with hopefully containing a prefteched doi
	 */
	private async _getDoiByFetching(query: IApiQuery, initializedFetchers: IApiFetcher[]): Promise<IApiQuery> {
		try {
			logger.info("Trying to fetch DOI for query without one");
			const fetchedDois: (string | undefined)[] = []
			for (const i in initializedFetchers) {
				fetchedDois.push(await initializedFetchers[i].getDoi(query));
			}

			//TODO HERE
			// let fetchedQueries = await Promise.all(fetchedDois);
			query.doi = this._compareDoisOfQueries(fetchedDois);
			logger.info(`Fetched DOI ${query.doi} for title ${query.title}`);

		}
		catch (e) {
			logger.warning(`Couldnt prefetch any DOI by query: ${e}`)
		}

		return query;
	}

	/**
	 * TODO: Implement logic to compare and select correct DOI.
	 * Select one of the prefetched dois
	 * @param dois list of prefetched doi strings from the different apis
	 * @return validDoi to make the actual fetch
	 */
	private _compareDoisOfQueries(dois: (string | undefined)[]): string {
		logger.info(`List of DOIS for paper: ${dois}`)
		const validDois = dois.filter(item => item);
		return validDois[0]!;
	}

	/**
	 * Initialize all IApiFetcher implementations and return them to make them calls
	 * @param apis list of enabled api enums
	 * @return list of IApiFetcher instances
	 */
	private _initializeEnabledApis(apis: [SourceApi, string?][]): IApiFetcher[] {
		const initializedFetchers: IApiFetcher[] = [];
		for (const a of apis) {
			const ApiObject = this._apiMapper[a[0] as SourceApi];
			const params = [this._apiParamMapper[a[0] as SourceApi], a[1]];
			initializedFetchers.push(new ApiObject(params[0] ? params[0] : '', params[1] ? params[1] : '', this.cache[a[0] as SourceApi]));
		}
		return initializedFetchers;
	}

	/**
	 * Stop a running fetch of a batch object
	 * @param batch list of enabled api enums
	 * @return list of IApiFetcher instances
	 */
	public stopFetch(batch: IApiBatch): boolean {
		const included = this.activeBatches.some(item => item.id === batch.id)
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
		const included = this.activeBatches.filter(item => isEqual(item, query))
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
		const active = this.subscribeActiveFetch(query);
		if (active) { return makePromise(active) }
		return await this.startFetch(query);
	}
}

