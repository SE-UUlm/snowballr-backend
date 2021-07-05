import {ApiMerger} from "../../src/api/apiMerger.ts";
import {IApiResponse} from "../../src/api/iApiResponse.ts";
import {IApiPaper} from "../../src/api/iApiPaper.ts";
import {assertEquals} from "https://deno.land/std@0.97.0/testing/asserts.ts"

export const firstPaper: IApiPaper = {title: ["I am a Great Paper"]}
export const secondPaper = {title: ["i am a great paper"]}
export const thirdPaper = {title: ["I am a Great-Paper"]}
Deno.test({
    name: "Merge 3 same Titles",
    async fn(): Promise<void> {
        let apiMerger = new ApiMerger({
            titleWeight: 10,
            titleLevenshtein: 10,
            abstractWeight: 7,
            abstractLevenshtein: 0,
            authorWeight: 8,
            overallWeight: 0.7,
            yearWeight: 2
        })

        let firstApiResponse: IApiResponse = {paper: firstPaper}
        let secondApiResponse: IApiResponse = {paper: secondPaper}
        let thirdApiResponse: IApiResponse = {paper: thirdPaper}
        let merged = await apiMerger.compare([makePromise<IApiResponse>(firstApiResponse), makePromise<IApiResponse>(secondApiResponse), makePromise<IApiResponse>(thirdApiResponse)]);
        console.error(merged)
        assertEquals(merged.length, 1)
        assertEquals(merged[0].paper.title, "I am a Great-Paper")

    }

})


export const makePromise = async <T>(t: T) => {
    return t
}