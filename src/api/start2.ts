import { IApiQuery } from "./iApiQuery.ts";
import { IApiResponse } from "./iApiResponse.ts";
import { MicrosoftResearchApi } from "./microsoftResearchApi.ts";
import { OpenCitationsApi } from "./openCitationsApi.ts";
import { logger, fileLogger } from "./logger.ts";
import { CrossRefApi } from "./crossRefApi.ts";
import { ApiMerger } from "./apiMerger.ts";
import { SemanticScholar } from "./semanticScholar.ts"
import { IeeeApi } from "./ieeeApi.ts";
import { ApiBatcher } from "./apiBatcher.ts";
import { SourceApi } from "./iApiPaper.ts";
import { IComparisonWeight } from "./iComparisonWeight.ts";


const Batcher = new ApiBatcher();

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
	rawName: "sebastian erdweg",
	doi: "10.1109/SEAA.2009.60",
	title: "The State of the Art in Language Workbenches",
	enabledApis: [SourceApi.IE, SourceApi.MA, SourceApi.CR, SourceApi.OC, SourceApi.S2],
	aggressivity: comparisonWeight
}

let batch = Batcher.startFetch(query);

ApiMerger.logResponse(await batch.response);