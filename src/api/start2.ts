import { IApiQuery } from "./iApiQuery.ts";
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
	rawName: "alexander raschke",
	title: "Modeling Companion for Software Practitioners",
	enabledApis: [SourceApi.MA, SourceApi.S2],
	aggressivity: comparisonWeight
}

let batch = await Batcher.startFetch(query);

ApiMerger.logResponse(await batch.response);