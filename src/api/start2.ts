import { IApiQuery } from "./iApiQuery.ts";
import { IApiResponse } from "./iApiResponse.ts";
import { IApiFetcher } from "./iApiFetcher.ts";
import { MicrosoftResearchApi } from "./microsoftResearchApi.ts";
import { OpenCitationsApi } from "./openCitationsApi.ts";
import { logger, fileLogger } from "./logger.ts";
import { CrossRefApi } from "./crossRefApi.ts";
import { ApiMerger } from "./apiMerger.ts";
import { SemanticScholar } from "./semanticScholar.ts"
import { IApiPaper } from "./iApiPaper.ts";
import { IeeeApi } from "./ieeeApi.ts";
import { ApiBatcher } from "./apiBatcher.ts";
import { SourceApi } from "./iApiPaper.ts";

const Batcher = new ApiBatcher();

const query: IApiQuery = {
	id: "tst",
	rawName: "sebastian erdweg",
	doi: "10.1109/SEAA.2009.60",
	title: "The State of the Art in Language Workbenches",
	enabledApis: [SourceApi.MA, SourceApi.S2]
}

let batch = Batcher.startFetch(query);

ApiMerger.logResponse(await batch.response);