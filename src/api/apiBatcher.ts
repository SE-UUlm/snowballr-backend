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

export class ApiBatcher implements IApiBatcher {
	public cache: SourceApiToCache = {};  //= new Cache(true, false, 3000000);
	public activeBatches: IApiBatch[];
	private _apiMapper = {
		[SourceApi.MA]: MicrosoftResearchApi,
		[SourceApi.CR]: CrossRefApi,
		[SourceApi.OC]: OpenCitationsApi,
		[SourceApi.S2]: SemanticScholar,
		[SourceApi.IE]: IeeeApi
	}

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

	public async startFetch(query: IApiQuery): Promise<IApiBatch> {
		let initializedFetchers: IApiFetcher[] = this._initializeEnabledApis(query.enabledApis!);
		let response: Promise<IApiResponse>[] = [];
		if (!query.doi) {
			query = await this._getDoiByFetching(query, initializedFetchers);
		}
		for (let i in initializedFetchers) {
			response.push(initializedFetchers[i].fetch(query));
		}
		const merger = new ApiMerger(query.aggressivity);
		let apiBatch = {} as IApiBatch;
		assign(apiBatch, ApiBatch);
		apiBatch.id = crypto.randomUUID();
		apiBatch.subscribers = [query];
		apiBatch.status = BatcherStatus.R;
		apiBatch.response = merger.compare(response);

		return apiBatch;
	}

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
			logger.error(`Couldnt fetch any DOI by query: ${e}`)
		}

		return query;
	}

	private _compareDoisOfQueries(dois: (string | undefined)[]): string {
		logger.info(`List of DOIS for paper: ${dois}`)
		let validDois = dois.filter(item => item);
		return validDois[0]!;
	}

	private _initializeEnabledApis(apis: SourceApi[]): IApiFetcher[] {
		let initializedFetchers: IApiFetcher[] = [];
		for (let a of apis) {
			let ApiObject = this._apiMapper[a as SourceApi];
			let params = this._apiParamMapper[a as SourceApi];
			initializedFetchers.push(new ApiObject(params[0], params[1] ? params[1] : '', this.cache[a as SourceApi]));
		}
		return initializedFetchers;
	}

	public stopFetch(batch: IApiBatch): boolean {
		let included = this.activeBatches.some(item => item.id === batch.id)
		if (included) {
			this.activeBatches = this.activeBatches.filter(item => !(item.id === batch.id))
			return true;
		}
		return false;
	}

	public subscribeActiveFetch(query: IApiQuery): IApiBatch | undefined {
		let included = this.activeBatches.filter(item => isEqual(item, query))
		if (included.length > 0) {
			included[0].addSubscriber(query)
			return included[0]
		}
	}

	public kill() {
		Object.keys(this.cache).forEach(key => this.cache[key].clear());
		logger.info("Killed all Caches");
		//Object.keys(this.cache).forEach(key => console.log(this.cache[key].empty()));
	}
}

