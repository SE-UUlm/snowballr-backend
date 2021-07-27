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
//import { v4 } from "https://deno.land/std@$STD_VERSION/uuid/mod.ts";
import { ApiMerger } from "./apiMerger.ts";
import { logger, fileLogger } from "./logger.ts";
import { Cache } from "./cache.ts";


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
		[SourceApi.IE]: IeeeApi
	}

	// Map a constructor parameters to the api type so it can be implemented in a loop. ADD NEW PARAMETES HERE IF NEW CLASS IMPLEMENTED TO BE APPLIED
	private _apiParamMapper = {
		[SourceApi.MA]: ["https://api.labs.cognitive.microsoft.com/academic/v1.0/evaluate", "9a02225751354cd29397eba3f5382101"],
		[SourceApi.OC]: ["https://opencitations.net"],
		[SourceApi.CR]: ["https://api.crossref.org/works", "luca999@web.de"],
		[SourceApi.S2]: ["https://api.semanticscholar.org/v1/paper"],
		[SourceApi.IE]: ["http://ieeexploreapi.ieee.org/api/v1/search/articles", "4yk5d9an52ejynjsmzqxe62r"]
	}

	public constructor() {
		this.activeBatches = []
		for (let s in this._apiMapper) {
			this.cache[s] = new Cache<IApiResponse>(false, true, 60, 10080, s.toString());
		}
	}

	/**
	 * Start fetching a query. Batch is added to public var activeBatches.
	 * @param query Query to fetch
	 * @return apiBatch An open apiBatch object with the open, subscribable fetch promise of the apis 
	 */
	public async startFetch(query: IApiQuery): Promise<IApiBatch> {
		let initializedFetchers: IApiFetcher[] = this._initializeEnabledApis(query.enabledApis!);
		let response: Promise<IApiResponse>[] = [];
		// try to prefetch a doi for the query objects by querying for the other variables. Only implemented on selected apis.
		if (!query.doi) {
			query = await this._getDoiByFetching(query, initializedFetchers);
		}
		for (let i in initializedFetchers) {
			response.push(initializedFetchers[i].fetch(query));
		}
		const merger = new ApiMerger(query.aggression);
		let apiBatch = {} as IApiBatch;
		assign(apiBatch, ApiBatch);
		apiBatch.id = crypto.randomUUID();
		apiBatch.subscribers = [query];
		apiBatch.status = BatcherStatus.R;
		apiBatch.response = merger.compare(response);
		this.activeBatches.push(apiBatch);
		return apiBatch;
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
			let fetchedDois: Promise<string | undefined>[] = []
			for (let i in initializedFetchers) {
				fetchedDois.push(initializedFetchers[i].getDoi(query));
			}
			let fetchedQueries = await Promise.all(fetchedDois);
			query.doi = this._compareDoisOfQueries(fetchedQueries);
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
		let validDois = dois.filter(item => item);
		return validDois[0]!;
	}

	/**
	 * Initialize all IApiFetcher implementations and return them to make them calls
	 * @param apis list of enabled api enums
	 * @return list of IApiFetcher instances
	 */
	private _initializeEnabledApis(apis: SourceApi[]): IApiFetcher[] {
		let initializedFetchers: IApiFetcher[] = [];
		for (let a of apis) {
			let ApiObject = this._apiMapper[a as SourceApi];
			let params = this._apiParamMapper[a as SourceApi];
			initializedFetchers.push(new ApiObject(params[0], params[1] ? params[1] : '', this.cache[a as SourceApi]));
		}
		return initializedFetchers;
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

	/**
	 * Kills api batcher. InMemoryCache is deleted but fileCache is kept, yet it's ttl eventHandling is stopped.
	 */
	public kill() {
		Object.keys(this.cache).forEach(key => this.cache[key].clear());
		logger.info("Killed all Caches");
		//Object.keys(this.cache).forEach(key => console.log(this.cache[key].empty()));
	}
}

