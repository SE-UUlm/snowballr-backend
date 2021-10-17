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
export const makeFetching = (doi?: string, title?: string, name?: string, newComparisonWeight?: IComparisonWeight, enabledApis?: SourceApi[]) => {
    //TODO comparisons from logfile or settings


    const query: IApiQuery = {
        id: String(id++),
        rawName: name ? name : "",
        title: title ? title : "",
        doi: doi ? doi : undefined,
        enabledApis: enabledApis? enabledApis: [SourceApi.IE, SourceApi.MA, SourceApi.CR, SourceApi.OC, SourceApi.S2],
        aggression: newComparisonWeight? newComparisonWeight: comparisonWeight
    }

    return Batcher.startFetch(query);
}

export const startFetchFromProjectPaper = async (ppID: number) =>{
    let paper = await PaperScopeForStage.where("id", ppID).paper();
    let authors = await getAllAuthorsFromPaper(Number(paper.id))
    let authorName: string| undefined = undefined;
    if(Array.isArray(authors) && authors[0]){
        authorName = String(authors[0].rawString)
    }
    makeFetching(
        paper.doi? String(paper.doi): undefined,
        paper.title? String(paper.title): undefined,
        authorName
        )
    }


export const getActiveBatchLength = (ctx: Context)=> {
    ctx.response.status = 200;
    ctx.response.body = JSON.stringify({activeBatchesCount: Batcher.activeBatchLength()})
}