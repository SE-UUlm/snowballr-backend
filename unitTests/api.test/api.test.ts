import {ApiMerger} from "../../src/api/apiMerger.ts";
import {IApiResponse} from "../../src/api/iApiResponse.ts";

export const firstPaper = {title: "i am a great paper"}
export const secondPaper = {title: "I am a Great Paper"}

Deno.test({
    name: "",
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
        //assertEquals(apiMerger.compare(firstPaper, secondPaper), true)

    }

})