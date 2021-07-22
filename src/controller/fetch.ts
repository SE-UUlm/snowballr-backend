import { IApiQuery } from "../api/iApiQuery.ts";
import { ApiMerger } from "../api/apiMerger.ts";
import { ApiBatcher } from "../api/apiBatcher.ts";
import { SourceApi } from "../api/iApiPaper.ts";
import { IComparisonWeight } from "../api/iComparisonWeight.ts";
import { IApiBatch } from "../api/iApiBatcher.ts";

const Batcher = new ApiBatcher();
//TODO id
let id = 1;



export const startDoiFetch = (doi: string): IApiBatch => {

    //TODO comparisons from logfile
    const comparisonWeight: IComparisonWeight = {
        titleWeight: 15,
        titleLevenshtein: 10,
        abstractWeight: 7,
        abstractLevenshtein: 0,
        authorWeight: 8,
        overallWeight: 0.8,
        yearWeight: 2
    }

    const query: IApiQuery = {
        id: String(id++),
        rawName: "",
        title: "",
        doi: doi,
        enabledApis: [SourceApi.IE, SourceApi.MA, SourceApi.CR, SourceApi.OC, SourceApi.S2],
        aggressivity: comparisonWeight
    }

    return Batcher.startFetch(query);
}

