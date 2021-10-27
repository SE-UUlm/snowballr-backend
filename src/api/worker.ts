import { makePromise } from "../helper/assign.ts";
import { APIMAPPER, APIPARAMMAPPER } from "./apiBatcher.ts";
import { ApiMerger } from "./apiMerger.ts";
import { IApiFetcher } from "./iApiFetcher.ts";
import { SourceApi } from "./iApiPaper.ts";
import { IApiQuery } from "./iApiQuery.ts";
import { IApiResponse } from "./iApiResponse.ts";
import { logger } from "./logger.ts";
import { IeeeApi } from "./ieeeApi.ts";
import { MicrosoftResearchApi } from "./microsoftResearchApi.ts";
import { OpenCitationsApi } from "./openCitationsApi.ts";
import { SemanticScholar } from "./semanticScholar.ts";
import { GoogleScholar } from "./googleScholar.ts";
import { CrossRefApi } from "./crossRefApi.ts";

function initializeEnabledApis(apis: [SourceApi, string?][]): IApiFetcher[] {
	let initializedFetchers: IApiFetcher[] = [];
	for (let a of apis) {
		let ApiObject = APIMAPPER[a[0] as SourceApi];
		let params = [APIPARAMMAPPER[a[0] as SourceApi], a[1]];
		//console.log(CACHES);
		initializedFetchers.push(new ApiObject(params[0] ? params[0] : '', params[1] ? params[1] : '', CACHES[a[0] as SourceApi]));
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

	let parsedData = JSON.parse(e.data);
	let query: IApiQuery = parsedData.query;
	//console.log(query);

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

	//@ts-ignore
	self.postMessage({ payload: JSON.stringify(await finished), type: "finished" });
	logger.info("Closing Webworker")
	self.close();
}

//const merger: ApiMerger = new ApiMerger({ "titleWeight": 15, "titleLevenshtein": 10, "abstractWeight": 7, "abstractLevenshtein": 0, "authorWeight": 8, "overallWeight": 0.8, "yearWeight": 2 });
//@ts-ignore

self.postMessage({ type: "initialized" });
logger.info("Initialized Webworker");



