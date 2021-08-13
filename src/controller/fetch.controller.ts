import { IApiQuery } from "../api/iApiQuery.ts";
import { ApiMerger } from "../api/apiMerger.ts";
import { ApiBatcher } from "../api/apiBatcher.ts";
import { SourceApi } from "../api/iApiPaper.ts";
import { IComparisonWeight } from "../api/iComparisonWeight.ts";
import { IApiBatch } from "../api/iApiBatcher.ts";

export const Batcher = new ApiBatcher();
//TODO id
let id = 1;


/**
 * starts a fetch by the given info of a paper.
 * To work either a DOI or a title + authorname is needed.
 * @param doi 
 * @param title 
 * @param name a single rawname of on of the authors
 * @returns 
 */
export const makeFetching = (doi?: string, title?: string, name?: string) => {
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
        rawName: name ? name : "",
        title: title ? title : "",
        doi: doi ? doi : undefined,
        enabledApis: [SourceApi.IE, SourceApi.MA, SourceApi.CR, SourceApi.OC, SourceApi.S2],
        aggression: comparisonWeight
    }

    return Batcher.startFetch(query);
}

