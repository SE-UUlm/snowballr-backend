import { IApiQuery } from "../api/iApiQuery.ts";
import { ApiMerger } from "../api/apiMerger.ts";
import { ApiBatcher } from "../api/apiBatcher.ts";
import { SourceApi } from "../api/iApiPaper.ts";
import { IComparisonWeight } from "../api/iComparisonWeight.ts";
import { IApiBatch } from "../api/iApiBatcher.ts";
import { Context } from "https://deno.land/x/oak/mod.ts";
import { UserStatus } from "./validation.controller.ts";
import { validateUserEntry } from "./validation.controller.ts";
import { PaperScopeForStage } from "../model/db/paperScopeForStage.ts";
import { getAllAuthorsFromPaper } from "./databaseFetcher/author.ts";

export const Batcher = new ApiBatcher();
//TODO id
let id = 1;

const comparisonWeight: IComparisonWeight = {
    titleWeight: 10,
    titleLevenshtein: 10,
    abstractWeight: 7,
    abstractLevenshtein: 10,
    authorWeight: 8,
    overallWeight: 0.8,
    yearWeight: 2
}

/**
 * starts a fetch by the given info of a paper.
 * To work either a DOI or a title + authorname is needed.
 * @param doi 
 * @param title 
 * @param name a single rawname of on of the authors
 * @returns 
 */
export const makeFetching = (overallWeight: number, enabledApis: [SourceApi,string?][], doi?: string, title?: string, name?: string,) => {
    //TODO comparisons from logfile or settings
    let comparison: IComparisonWeight = {} as IComparisonWeight;
    Object.assign(comparison, comparisonWeight)
    comparison.overallWeight = overallWeight;

    const query: IApiQuery = {
        id: String(id++),
        rawName: name ? name : "",
        title: title ? title : "",
        doi: doi ? doi : undefined,
        enabledApis: enabledApis,
        aggression: comparison
    }

    return Batcher.startFetch(query);
}


export const getActiveBatchLength = (ctx: Context)=> {
    ctx.response.status = 200;
    ctx.response.body = JSON.stringify({activeBatchesCount: Batcher.activeBatchLength()})
}