import {ApiMerger} from "../../src/api/apiMerger.ts";
import {IApiResponse} from "../../src/api/iApiResponse.ts";
import {IApiPaper} from "../../src/api/iApiPaper.ts";
import {assertEquals} from "https://deno.land/std@0.97.0/testing/asserts.ts"
import { idType } from "../../src/api/iApiUniqueId.ts";

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
            overallWeight: 0.85,
            yearWeight: 2
        })
        const firstPaper: IApiPaper = {title: ["I am a Great Paper"]}
        const secondPaper = {title: ["i am a great paper"]}
        const thirdPaper = {title: ["I am a Great-Paper"]}
        const firstApiResponseJustTitle: IApiResponse = {paper: firstPaper}
        const secondApiResponseJustTitle: IApiResponse = {paper: secondPaper}
        const thirdApiResponseJustTitle: IApiResponse = {paper: thirdPaper}
        let merged = await apiMerger.compare([makePromise<IApiResponse>(firstApiResponseJustTitle), makePromise<IApiResponse>(secondApiResponseJustTitle), makePromise<IApiResponse>(thirdApiResponseJustTitle)]);
        assertEquals(merged.length, 1)
        assertEquals(merged[0].paper.title, ["I am a Great-Paper"])
    }

})

Deno.test({
    name: "Merge 3 same Titles shuffle",
    async fn(): Promise<void> {
        let apiMerger = new ApiMerger({
            titleWeight: 10,
            titleLevenshtein: 10,
            abstractWeight: 7,
            abstractLevenshtein: 0,
            authorWeight: 8,
            overallWeight: 0.85,
            yearWeight: 2
        })
        const firstPaper: IApiPaper = {title: ["I am a Great-Paper"]}
        const secondPaper = {title: ["i am a great paper"]}
        const thirdPaper = {title: ["I am a Great Paper"]}
        const firstApiResponseJustTitle: IApiResponse = {paper: firstPaper}
        const secondApiResponseJustTitle: IApiResponse = {paper: secondPaper}
        const thirdApiResponseJustTitle: IApiResponse = {paper: thirdPaper}
        let merged = await apiMerger.compare([makePromise<IApiResponse>(firstApiResponseJustTitle), makePromise<IApiResponse>(secondApiResponseJustTitle), makePromise<IApiResponse>(thirdApiResponseJustTitle)]);
        assertEquals(merged.length, 1)
        assertEquals(merged[0].paper.title, ["I am a Great-Paper"])
    }

})

Deno.test({
    name: "Merge 3 same Titles shuffle",
    async fn(): Promise<void> {
        let apiMerger = new ApiMerger({
            titleWeight: 10,
            titleLevenshtein: 10,
            abstractWeight: 7,
            abstractLevenshtein: 0,
            authorWeight: 8,
            overallWeight: 0.85,
            yearWeight: 2
        })
        const firstPaper: IApiPaper = {title: ["I am a Great-Paper"]}
        const secondPaper = {title: ["I am a Great Paper"]}
        const thirdPaper = {title: ["i am a great paper"]}
        const firstApiResponseJustTitle: IApiResponse = {paper: firstPaper}
        const secondApiResponseJustTitle: IApiResponse = {paper: secondPaper}
        const thirdApiResponseJustTitle: IApiResponse = {paper: thirdPaper}
        let merged = await apiMerger.compare([makePromise<IApiResponse>(firstApiResponseJustTitle), makePromise<IApiResponse>(secondApiResponseJustTitle), makePromise<IApiResponse>(thirdApiResponseJustTitle)]);
        assertEquals(merged.length, 1)
        assertEquals(merged[0].paper.title, ["I am a Great-Paper"])
    }

})

Deno.test({
    name: "Merge 3 same Titles shuffle",
    async fn(): Promise<void> {
        let apiMerger = new ApiMerger({
            titleWeight: 10,
            titleLevenshtein: 10,
            abstractWeight: 7,
            abstractLevenshtein: 0,
            authorWeight: 8,
            overallWeight: 0.85,
            yearWeight: 2
        })
        const firstPaper: IApiPaper = {title: ["i am a great paper"]}
        const secondPaper = {title: ["I am a Great Paper"]}
        const thirdPaper = {title: ["I am a Great-Paper"]}
        const firstApiResponseJustTitle: IApiResponse = {paper: firstPaper}
        const secondApiResponseJustTitle: IApiResponse = {paper: secondPaper}
        const thirdApiResponseJustTitle: IApiResponse = {paper: thirdPaper}
        let merged = await apiMerger.compare([makePromise<IApiResponse>(firstApiResponseJustTitle), makePromise<IApiResponse>(secondApiResponseJustTitle), makePromise<IApiResponse>(thirdApiResponseJustTitle)]);
        assertEquals(merged.length, 1)
        assertEquals(merged[0].paper.title, ["I am a Great-Paper"])
    }

})
Deno.test({
    name: "Merge 3 different Titles",
    async fn(): Promise<void> {
        let apiMerger = new ApiMerger({
            titleWeight: 10,
            titleLevenshtein: 10,
            abstractWeight: 7,
            abstractLevenshtein: 0,
            authorWeight: 8,
            overallWeight: 0.85,
            yearWeight: 2
        })
        const firstPaper: IApiPaper = {title: ["I am a Great Paper"]}
        const secondPaper = {title: ["i am another paper"]}
        const thirdPaper = {title: ["I am a Great-Paper"]}
        const firstApiResponseJustTitle: IApiResponse = {paper: firstPaper}
        const secondApiResponseJustTitle: IApiResponse = {paper: secondPaper}
        const thirdApiResponseJustTitle: IApiResponse = {paper: thirdPaper}
        let merged = await apiMerger.compare([makePromise<IApiResponse>(firstApiResponseJustTitle), makePromise<IApiResponse>(secondApiResponseJustTitle), makePromise<IApiResponse>(thirdApiResponseJustTitle)]);
        assertEquals(merged.length, 2)

    }

})


Deno.test({
    name: "Merge 3 same Titles with abstract shuffle",
    async fn(): Promise<void> {
        let apiMerger = new ApiMerger({
            titleWeight: 10,
            titleLevenshtein: 10,
            abstractWeight: 7,
            abstractLevenshtein: 0,
            authorWeight: 8,
            overallWeight: 0.85,
            yearWeight: 2
        })
        const firstPaper: IApiPaper = {title: ["I am a Great-Paper"], abstract: ["i am the abstract of the light side"]}
        const secondPaper = {title: ["i am a great paper"]}
        const fourthPaper = {title: ["I am a Great Paper"]}
        const firstApiResponseJustTitle: IApiResponse = {paper: firstPaper}
        const secondApiResponseJustTitle: IApiResponse = {paper: secondPaper}
        const thirdApiResponseJustTitle: IApiResponse = {paper: fourthPaper}

        let merged = await apiMerger.compare([makePromise<IApiResponse>(firstApiResponseJustTitle), makePromise<IApiResponse>(secondApiResponseJustTitle), makePromise<IApiResponse>(thirdApiResponseJustTitle)]);
        assertEquals(merged.length, 1)
        assertEquals(merged[0].paper.title, ["I am a Great-Paper"])
        assertEquals(merged[0].paper.abstract, ["i am the abstract of the light side"])

    }

})

Deno.test({
    name: "Merge 3 same Titles with abstract shuffle",
    async fn(): Promise<void> {
        let apiMerger = new ApiMerger({
            titleWeight: 10,
            titleLevenshtein: 10,
            abstractWeight: 7,
            abstractLevenshtein: 0,
            authorWeight: 8,
            overallWeight: 0.85,
            yearWeight: 2
        })
        const firstPaper: IApiPaper = {title: ["I am a Great Paper"]}
        const secondPaper = {title: ["I am a Great-Paper"], abstract: ["i am the abstract of the light side"]}
        const fourthPaper = {title: ["i am a great paper"]}
        const firstApiResponseJustTitle: IApiResponse = {paper: firstPaper}
        const secondApiResponseJustTitle: IApiResponse = {paper: secondPaper}
        const thirdApiResponseJustTitle: IApiResponse = {paper: fourthPaper}

        let merged = await apiMerger.compare([makePromise<IApiResponse>(firstApiResponseJustTitle), makePromise<IApiResponse>(secondApiResponseJustTitle), makePromise<IApiResponse>(thirdApiResponseJustTitle)]);
        assertEquals(merged.length, 1)
        assertEquals(merged[0].paper.title, ["I am a Great-Paper"])
        assertEquals(merged[0].paper.abstract, ["i am the abstract of the light side"])

    }

})

Deno.test({
    name: "Merge 3 same Titles with 2 abstracts",
    async fn(): Promise<void> {
        let apiMerger = new ApiMerger({
            titleWeight: 10,
            titleLevenshtein: 10,
            abstractWeight: 7,
            abstractLevenshtein: 0,
            authorWeight: 8,
            overallWeight: 0.85,
            yearWeight: 2
        })

        const firstPaper: IApiPaper = {title: ["I am a Great Paper"]}
        const fourthPaper = {title: ["I am a Great-Paper"], abstract: ["i am the abstract of the light side"]}
        const fifthPaper = {abstract: ["I am the Abstract: of the Light Side"]}
        const firstApiResponseJustTitle: IApiResponse = {paper: firstPaper}
        const secondApiResponseJustTitle: IApiResponse = {paper: fourthPaper}
        const thirdApiResponseJustTitle: IApiResponse = {paper: fifthPaper}

        let merged = await apiMerger.compare([makePromise<IApiResponse>(firstApiResponseJustTitle), makePromise<IApiResponse>(secondApiResponseJustTitle), makePromise<IApiResponse>(thirdApiResponseJustTitle)]);
        assertEquals(merged.length, 1)
        assertEquals(merged[0].paper.title, ["I am a Great-Paper"])
        assertEquals(merged[0].paper.abstract, ["I am the Abstract: of the Light Side"])

    }

})


export const makePromise = async <T>(t: T) => {
    return t
}
