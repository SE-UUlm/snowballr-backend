import { makePromise } from "../helper/assign.ts";
import { getCache, loadingCaches } from "../helper/workerHelper.ts";
import { APIMAPPER, APIPARAMMAPPER } from "./apiBatcher.ts";
import { ApiMerger } from "./apiMerger.ts";
import { IApiFetcher } from "./iApiFetcher.ts";
import { SourceApi } from "./iApiPaper.ts";
import { IApiQuery } from "./iApiQuery.ts";
import { IApiResponse } from "./iApiResponse.ts";
import { ILimitedWorkers } from "./iLimitedWorkers.ts";
//import { ILimitedWorkers } from "./iLimitedWorkers.ts";
import { logger } from "./logger.ts";

export let currentRateLimit: ILimitedWorkers = { googleScholar: 1, crossRef: 1 };

function initializeEnabledApis(apis: [SourceApi, string?][]): IApiFetcher[] {
	let initializedFetchers: IApiFetcher[] = [];
	for (let a of apis) {
		let ApiObject = APIMAPPER[a[0] as SourceApi];
		let params = [APIPARAMMAPPER[a[0] as SourceApi], a[1]];
		//console.log(CACHES);
		console.log("NEW FETCH: " + a)
		initializedFetchers.push(new ApiObject(params[0] ? params[0] : '', params[1] ? params[1] : ''));
	}
	return initializedFetchers;
}

async function getDoiByFetching(query: IApiQuery, initializedFetchers: IApiFetcher[]): Promise<IApiQuery> {
	try {
		logger.info("Trying to fetch DOI for query without one");
		let fetchedDois: (string | undefined)[] = []
		for (let i in initializedFetchers) {
			fetchedDois.push(await initializedFetchers[i].getDoi(query));
		}

		//TODO HERE
		// let fetchedQueries = await Promise.all(fetchedDois);
		query.doi = compareDoisOfQueries(fetchedDois);
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
function compareDoisOfQueries(dois: (string | undefined)[]): string {
	logger.info(`List of DOIS for paper: ${dois}`)
	let validDois = dois.filter(item => item);
	return validDois[0]!;
}


//@ts-ignore: linter detects error although its none
self.onmessage = async (e) => {
	let data = e.data;
	//console.log(`WORKER GOT MESSAGE: ${data.type}`)
	switch (data.type) {
		case 'run': {

			let query: IApiQuery = JSON.parse(data.query);
			let initializedFetchers: IApiFetcher[] = initializeEnabledApis(query.enabledApis!);
			let response: Promise<IApiResponse>[] = [];

			if (!query.doi) {
				query = await getDoiByFetching(query, initializedFetchers);
			}
			for (let i in initializedFetchers) {
				response.push(initializedFetchers[i].fetch(query));
			}

			const merger: ApiMerger = new ApiMerger(query.aggression);
			let finished = merger.compare(response);

			//await getCache('f102952052fe8bf5bd53e44a9dd47a8ec3a4a2fff056b4cc18a290be858882a6', SourceApi.MA);
			//console.log("CACHE FINALLY LOADED")
			//@ts-ignore
			self.postMessage({ payload: JSON.stringify(await finished), type: "finished" })
			self.close();
			break;
		}
		case 'returningCache': {
			let cachePayload: IApiResponse | undefined = undefined;
			try {
				cachePayload = JSON.parse(data.cachePayload)
			}
			catch (e) {
				cachePayload = undefined;
			}
			loadingCaches.set(data.queryHash + data.cacheName as string, cachePayload);
			//console.log(loadingCaches)
			break;
		}
		case 'updateRateLimit': {
			console.log("-------------Rate limit updated------------ " + data.newRateLimit);
			currentRateLimit = data.newRateLimit;
			break;
		}
	}
}


logger.info("----NEW WEBWORKER INITIALIZED");

//@ts-ignore
self.postMessage({ type: "initialized" })




