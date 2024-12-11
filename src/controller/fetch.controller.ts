import { IApiQuery } from "../api/iApiQuery.ts";
import { ApiBatcher } from "../api/apiBatcher.ts";
import { SourceApi } from "../api/iApiPaper.ts";
import { IComparisonWeight } from "../api/iComparisonWeight.ts";
import { IApiBatch } from "../api/iApiBatcher.ts";
import { Context } from "https://deno.land/x/oak@v11.1.0/mod.ts";

export const Batcher = new ApiBatcher();
//TODO id
let id = 1;

export const comparisonWeight: IComparisonWeight = {
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
export const makeFetching = (overallWeight: number, enabledApis: [SourceApi,string?][], doi?: string, title?: string, name?: string, projectName?: string) => {
    //TODO comparisons from logfile or settings
    const comparison: IComparisonWeight = {} as IComparisonWeight;
    Object.assign(comparison, comparisonWeight)
    comparison.overallWeight = overallWeight;

    const query: IApiQuery = {
        id: String(id++),
        rawName: name ? name : "",
        title: title ? title : "",
        doi: doi ? doi : undefined,
        enabledApis: enabledApis,
        aggression: comparison,
        projectName: projectName

    }

    return Batcher.startFetch(query);
}


export const getActiveBatches = (ctx: Context)=> {
    ctx.response.status = 200;
    let batches= JSON.parse(JSON.stringify(Batcher.activeBatches))
    batches = batches.map((batch: IApiBatch) => {
        batch.subscribers = batch.subscribers.map(subscriber => {
            //removes credentials from the enabled apis
            subscriber.enabledApis = subscriber.enabledApis!.map(eA =>{
                return [eA[0]]
            })
            return subscriber
           });
        return batch
    });
    ctx.response.body = JSON.stringify({batches: batches})
}