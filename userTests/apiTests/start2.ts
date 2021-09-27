import { IApiQuery } from "../../src/api/iApiQuery.ts";
import { IApiResponse } from "../../src/api/iApiResponse.ts";
import { MicrosoftResearchApi } from "../../src/api/microsoftResearchApi.ts";
import { OpenCitationsApi } from "../../src/api/openCitationsApi.ts";
import { logger, fileLogger } from "../../src/api/logger.ts";
import { CrossRefApi } from "../../src/api/crossRefApi.ts";
import { ApiMerger } from "../../src/api/apiMerger.ts";
import { SemanticScholar } from "../../src/api/semanticScholar.ts"
import { IeeeApi } from "../../src/api/ieeeApi.ts";
import { ApiBatcher } from "../../src/api/apiBatcher.ts";
import { SourceApi } from "../../src/api/iApiPaper.ts";
import { IComparisonWeight } from "../../src/api/iComparisonWeight.ts";
import { logResponse } from "../../src/helper/loggerHelper.ts"



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

const query: IApiQuery = {
	id: "tst",
	rawName: undefined,
	doi: "10.1109/SEAA.2009.60",
	title: "Translation of UML 2 Activity Diagrams into Finite State Machines for Model Checking",
	enabledApis: [SourceApi.MA, SourceApi.OC, SourceApi.IE, SourceApi.CR, SourceApi.S2],
	aggression: comparisonWeight
}

let batch = await BATCHER.startFetch(query);

logResponse(await batch.response);

BATCHER.kill();

//cd Deno.exit(0)
