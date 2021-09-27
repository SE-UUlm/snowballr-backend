import { IApiQuery } from "../../src/api/iApiQuery.ts";
import { IApiResponse } from "../../src/api/iApiResponse.ts";
import { MicrosoftResearchApi } from "../../src/api/microsoftResearchApi.ts";
import { OpenCitationsApi } from "../../src/api/openCitationsApi.ts";
import { logger, fileLogger } from "../../src/api/logger.ts";
import { CrossRefApi } from "../../src/api/crossRefApi.ts";
import { ApiMerger } from "../../src/api/apiMerger.ts";
import { SemanticScholar } from "../../src/api/semanticScholar.ts"
import { IeeeApi } from "../../src/api/ieeeApi.ts";
import { GoogleScholar } from "../../src/api/googleScholar.ts";
import { ApiBatcher } from "../../src/api/apiBatcher.ts";
import { SourceApi } from "../../src/api/iApiPaper.ts";
import { IComparisonWeight } from "../../src/api/iComparisonWeight.ts";
import { logResponse } from "../../src/helper/loggerHelper.ts"
import { TestDoi } from "./testDoi.ts"



const BATCHER = new ApiBatcher();

const comparisonWeight = {
	titleWeight: 15,
	titleLevenshtein: 10,
	abstractWeight: 7,
	abstractLevenshtein: 0,
	authorWeight: 8,
	overallWeight: 0.8,
	yearWeight: 2
} as IComparisonWeight;


/* TODO hash for cash out of query without enabled apis*/
for (let i = 0; i < 1; i++) {
	const query: IApiQuery = {
		id: "tst",
		rawName: undefined,
		doi: TestDoi[i % 1],
		title: undefined,
		enabledApis: [SourceApi.GS, SourceApi.MA], //[SourceApi.GS, SourceApi.CR, SourceApi.IE, SourceApi.MA, SourceApi.OC, SourceApi.S2],
		aggression: comparisonWeight
	}
	console.log(`Iteration ${i}`);
	let batch = await BATCHER.startFetch(query);
	logResponse(await batch.response);
}

BATCHER.kill();

//cd Deno.exit(0)
