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

export const ApiBatch: IApiBatch = {
	id: undefined,
	subscribers: [],
	status: BatcherStatus.D,
	response: makePromise([]),
	addSubscriber(subscriber: IApiQuery): void {
		this.subscribers.push(subscriber);
	}
}
export class ApiBatcher implements IApiBatcher {
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
		[SourceApi.CR]: ["https://opencitations.net"],
		[SourceApi.OC]: ["https://api.crossref.org/works", "lukas.romer@uni-ulm.de"],
		[SourceApi.S2]: ["https://api.semanticscholar.org/v1/paper"],
		[SourceApi.IE]: ["http://ieeexploreapi.ieee.org/api/v1/search/articles", "4yk5d9an52ejynjsmzqxe62r"]
	}

	public constructor() {
		this.activeBatches = []
	}

	public startFetch(query: IApiQuery): IApiBatch {
		let initializedFetchers: IApiFetcher[] = this._initializeEnabledApis(query.enabledApis!);
		let response: Promise<IApiResponse>[] = [];
		for (let i in initializedFetchers) {
			response.push(initializedFetchers[i].fetch(query));

		}
		const merger = new ApiMerger();
		let apiBatch = {} as IApiBatch;
		assign(apiBatch, ApiBatch);
		apiBatch.id = crypto.randomUUID();
		apiBatch.subscribers = [query];
		apiBatch.status = BatcherStatus.R;
		apiBatch.response = merger.compare(response);

		return apiBatch;
	}

	private _initializeEnabledApis(apis: SourceApi[]): IApiFetcher[] {
		let initializedFetchers: IApiFetcher[] = [];
		for (let a of apis) {
			let ApiObject = this._apiMapper[a as SourceApi];
			let params = this._apiParamMapper[a as SourceApi];
			initializedFetchers.push(new ApiObject(params[0], params[1] ? params[1] : ''));
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
}

