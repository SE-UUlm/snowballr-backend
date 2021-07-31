import { ApiMerger } from "../../src/api/apiMerger.ts";
import { IApiResponse } from "../../src/api/iApiResponse.ts";
import { IApiPaper, SourceApi } from "../../src/api/iApiPaper.ts";
import { assertEquals } from "https://deno.land/std@0.97.0/testing/asserts.ts"
import { idType } from "../../src/api/iApiUniqueId.ts";

export const standardPaper: IApiPaper = { title: [], abstract: [], author: [], year: [], publisher: [], numberOfCitations: [], numberOfReferences: [], type: [], scope: [], scopeName: [], pdf: [], uniqueId: [], source: [], raw: [] }


Deno.test({
	name: "test 2 papers merge author",
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
		let firstPaper = {} as IApiPaper;
		let secondPaper = {} as IApiPaper;
		Object.assign(firstPaper, standardPaper)
		Object.assign(firstPaper, { "title": ["awsome paper title"], "author": [{ "orcid": [], "rawString": ["samuel idowu"], "lastName": ["idowu"], "firstName": ["samuel"] }, { "orcid": [], "rawString": ["max muster"], "lastName": ["Muster"], "firstName": ["Max"] }] })
		Object.assign(secondPaper, standardPaper)
		Object.assign(secondPaper, { "title": ["awsome paper title"], "author": [{ "orcid": [], "rawString": ["Samuel Idowu"], "lastName": ["Idowu"], "firstName": ["Samuel"] }, { "orcid": [], "rawString": ["Max Muster"], "lastName": ["muster"], "firstName": ["max"] }] })
		const firstApiResponseJustTitle: IApiResponse = { paper: firstPaper, citations: [], references: [] }
		const secondApiResponseJustTitle: IApiResponse = { paper: secondPaper, citations: [], references: [] }
		let merged = await apiMerger.compare([makePromise<IApiResponse>(firstApiResponseJustTitle), makePromise<IApiResponse>(secondApiResponseJustTitle)]);
		assertEquals(merged.length, 1)
		assertEquals(merged[0].paper.author![0]!.firstName!, ["Samuel"])
		assertEquals(merged[0].paper.author![0]!.lastName!, ["Idowu"])
		assertEquals(merged[0].paper.author![0]!.rawString!, ["Samuel Idowu"])
	}

})

Deno.test({
	name: "test 2 papers shortened author",
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
		let firstPaper = {} as IApiPaper;
		let secondPaper = {} as IApiPaper;
		Object.assign(firstPaper, standardPaper)
		Object.assign(firstPaper, { "title": ["awsome paper title"], "author": [{ "id": [], "orcid": [], "rawString": ["samuel idowu"], "lastName": ["idowu"], "firstName": ["samuel"] }, { "id": [], "orcid": [], "rawString": ["max muster"], "lastName": [], "firstName": [] }] })

		Object.assign(secondPaper, standardPaper)
		Object.assign(secondPaper, { "title": ["awsome paper title"], "author": [{ "id": [], "orcid": [], "rawString": ["Samuel Idowu"], "lastName": ["Idowu"], "firstName": ["Samuel"] }, { "id": [], "orcid": [], "rawString": ["M. Muster"], "lastName": [], "firstName": [] }] })


		const firstApiResponseJustTitle: IApiResponse = { paper: firstPaper, citations: [], references: [] }
		const secondApiResponseJustTitle: IApiResponse = { paper: secondPaper, citations: [], references: [] }
		let merged = await apiMerger.compare([makePromise<IApiResponse>(firstApiResponseJustTitle), makePromise<IApiResponse>(secondApiResponseJustTitle)]);
		assertEquals(merged.length, 1)
		assertEquals(merged[0].paper.author![0]!.firstName!, ["Samuel"])
		assertEquals(merged[0].paper.author![0]!.lastName!, ["Idowu"])
		assertEquals(merged[0].paper.author![0]!.rawString!, ["Samuel Idowu"])
		assertEquals(merged[0].paper.author![1]!.rawString!, ["max muster"])
	}

})

Deno.test({
	name: "test 3 papers merge author",
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
		let firstPaper = {} as IApiPaper;
		let secondPaper = {} as IApiPaper;
		let thirdPaper = {} as IApiPaper;

		Object.assign(firstPaper, standardPaper)
		Object.assign(firstPaper, { "title": ["awsome paper title"], "abstract": ["one good abstract"], "author": [{ "id": [], "orcid": [], "rawString": ["samuel idowu"], "lastName": ["idowu"], "firstName": [] }, { "id": [], "orcid": [], "rawString": ["max muster"], "lastName": ["muster"], "firstName": ["Max"] }] })

		Object.assign(secondPaper, standardPaper)
		Object.assign(secondPaper, { "title": ["awsome paper title"], "abstract": ["one good abstract"], "author": [{ "id": [], "orcid": [], "rawString": ["Samuel Idowu"], "lastName": [], "firstName": ["samuel"] }, { "id": [], "orcid": [], "rawString": [], "lastName": ["Muster"], "firstName": ["max"] }] })

		Object.assign(thirdPaper, standardPaper)
		Object.assign(thirdPaper, { "title": ["awsome paper title"], "abstract": ["one good abstract"], "author": [{ "id": [], "orcid": [], "rawString": ["Samuel Idowu"], "lastName": ["Idowu"], "firstName": ["Samuel"] }, { "id": [], "orcid": [], "rawString": ["Max Muster"], "lastName": [], "firstName": [] }] })


		const firstApiResponseJustTitle: IApiResponse = { paper: firstPaper, citations: [], references: [] }
		const secondApiResponseJustTitle: IApiResponse = { paper: secondPaper, citations: [], references: [] }
		const thirdApiResponseJustTitle: IApiResponse = { paper: thirdPaper, citations: [], references: [] }
		let merged = await apiMerger.compare([makePromise<IApiResponse>(firstApiResponseJustTitle), makePromise<IApiResponse>(secondApiResponseJustTitle), makePromise<IApiResponse>(thirdApiResponseJustTitle)]);
		console.debug(merged)
		assertEquals(merged.length, 1)
		assertEquals(merged[0].paper.author![0]!.firstName!, ["Samuel"])
		assertEquals(merged[0].paper.author![0]!.lastName!, ["Idowu"])
		assertEquals(merged[0].paper.author![0]!.rawString!, ["Samuel Idowu"])
		assertEquals(merged[0].paper.author![1]!.firstName!, ["Max"])
		assertEquals(merged[0].paper.author![1]!.lastName!, ["Muster"])
		assertEquals(merged[0].paper.author![1]!.rawString!, ["Max Muster"])
	}

})

Deno.test({
	name: "test 3 papers merge author",
	async fn(): Promise<void> {
		let apiMerger = new ApiMerger({
			titleWeight: 10,
			titleLevenshtein: 10,
			abstractWeight: 7,
			abstractLevenshtein: 0,
			authorWeight: 8,
			overallWeight: 0.8,
			yearWeight: 2
		})

		let firstPaper = {} as IApiPaper;
		let secondPaper = {} as IApiPaper;
		let thirdPaper = {} as IApiPaper;
		Object.assign(firstPaper, standardPaper)
		Object.assign(firstPaper, { "title": ["awsome paper title"], "abstract": ["one good abstract"], "author": [{ "id": [], "orcid": [], "rawString": ["samuel idowu"], "lastName": ["idowu"], "firstName": [] }, { "id": [], "orcid": [], "rawString": ["max muster"], "lastName": ["muster"], "firstName": ["Max"] }] })

		Object.assign(secondPaper, standardPaper)
		Object.assign(secondPaper, { "title": ["awsome paper title"], "abstract": ["one good abstract"], "author": [{ "id": [], "orcid": [], "rawString": ["Samuel Idowu"], "lastName": [], "firstName": ["samuel"] }, { "id": [], "orcid": [], "rawString": [], "lastName": ["Muster"], "firstName": ["max"] }] })

		Object.assign(thirdPaper, standardPaper)
		Object.assign(thirdPaper, { "title": ["awsome paper title"], "abstract": ["one good abstract"], "author": [{ "id": [], "orcid": [], "rawString": ["Samuel Idowu"], "lastName": ["Idowu"], "firstName": ["Samuel"] }, { "id": [], "orcid": [], "rawString": ["Max Muster"], "lastName": [], "firstName": [] }] })


		const firstApiResponseJustTitle: IApiResponse = { paper: firstPaper, citations: [], references: [] }
		const secondApiResponseJustTitle: IApiResponse = { paper: secondPaper, citations: [], references: [] }
		const thirdApiResponseJustTitle: IApiResponse = { paper: thirdPaper, citations: [], references: [] }
		let merged = await apiMerger.compare([makePromise<IApiResponse>(firstApiResponseJustTitle), makePromise<IApiResponse>(secondApiResponseJustTitle), makePromise<IApiResponse>(thirdApiResponseJustTitle)]);
		assertEquals(merged.length, 1)
		assertEquals(merged[0].paper.author![0]!.firstName!, ["Samuel"])
	}

})


Deno.test({
	name: "test 4 papers merge author",
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

		let firstPaper = {} as IApiPaper;
		let secondPaper = {} as IApiPaper;
		let thirdPaper = {} as IApiPaper;
		let fourthPaper = {} as IApiPaper;
		Object.assign(firstPaper, standardPaper)
		Object.assign(firstPaper, { "title": ["awsome paper title"], "abstract": ["one good abstract"], "author": [{ "id": [], "orcid": [], "rawString": ["Samuel Idowu"], "lastName": ["idowu"], "firstName": [] }, { "id": [], "orcid": [], "rawString": ["max muster"], "lastName": ["muster"], "firstName": ["Max"] }] })
		Object.assign(secondPaper, standardPaper)
		Object.assign(secondPaper, { "title": ["awsome paper title"], "abstract": ["one good abstract"], "author": [{ "id": [], "orcid": [], "rawString": ["samuel idowu"], "lastName": [], "firstName": ["samuel"] }, { "id": [], "orcid": [], "rawString": [], "lastName": ["Muster"], "firstName": ["max"] }, { "id": [], "orcid": [], "rawString": ["timmy turner"], "lastName": [], "firstName": [] }] })
		Object.assign(thirdPaper, standardPaper)
		Object.assign(thirdPaper, { "title": ["awsome paper title"], "abstract": ["one good abstract"], "author": [{ "id": [], "orcid": [], "rawString": [], "lastName": ["Idowu"], "firstName": ["Samuel"] }, { "id": [], "orcid": [], "rawString": ["Max Muster"], "lastName": [], "firstName": [] }, { "id": [], "orcid": [], "rawString": [], "lastName": ["Turner"], "firstName": ["Timmy"] }] })
		Object.assign(fourthPaper, standardPaper)
		Object.assign(fourthPaper, { "title": ["awsome paper title"], "abstract": ["one good abstract"], "author": [{ "id": [], "orcid": [], "rawString": ["Samuel Idowu"], "lastName": ["idowu"], "firstName": [] }, { "id": [], "orcid": [], "rawString": ["Max Muster"], "lastName": [], "firstName": [] }, { "id": [], "orcid": [], "rawString": ["Timmy Turner"], "lastName": ["turner"], "firstName": ["timmy"] }] })

		const firstApiResponseJustTitle: IApiResponse = { paper: firstPaper, citations: [], references: [] }
		const secondApiResponseJustTitle: IApiResponse = { paper: secondPaper, citations: [], references: [] }
		const thirdApiResponseJustTitle: IApiResponse = { paper: thirdPaper, citations: [], references: [] }
		const fourthApiResponseJustTitle: IApiResponse = { paper: fourthPaper, citations: [], references: [] }
		let merged = await apiMerger.compare([makePromise<IApiResponse>(firstApiResponseJustTitle), makePromise<IApiResponse>(secondApiResponseJustTitle), makePromise<IApiResponse>(thirdApiResponseJustTitle), makePromise<IApiResponse>(fourthApiResponseJustTitle)]);
		assertEquals(merged.length, 1)
		assertEquals(merged[0].paper.author![0]!.firstName!, ["Samuel"])
		assertEquals(merged[0].paper.author![0]!.lastName!, ["Idowu"])
		assertEquals(merged[0].paper.author![0]!.rawString!, ["Samuel Idowu"])
		assertEquals(merged[0].paper.author![1]!.firstName!, ["Max"])
		assertEquals(merged[0].paper.author![1]!.lastName!, ["Muster"])
		assertEquals(merged[0].paper.author![1]!.rawString!, ["Max Muster"])
		assertEquals(merged[0].paper.author![2]!.firstName!, ["Timmy"])
		assertEquals(merged[0].paper.author![2]!.lastName!, ["Turner"])
		assertEquals(merged[0].paper.author![2]!.rawString!, ["Timmy Turner"])
	}

})


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
		let firstPaper = {} as IApiPaper;
		let secondPaper = {} as IApiPaper;
		let thirdPaper = {} as IApiPaper;
		Object.assign(firstPaper, standardPaper)
		Object.assign(firstPaper, { title: ["I am a Great Paper"] })
		Object.assign(secondPaper, standardPaper)
		Object.assign(secondPaper, { title: ["i am a great paper"] })
		Object.assign(thirdPaper, standardPaper)
		Object.assign(thirdPaper, { title: ["I am a Great-Paper"] })

		const firstApiResponseJustTitle: IApiResponse = { paper: firstPaper, citations: [], references: [] }
		const secondApiResponseJustTitle: IApiResponse = { paper: secondPaper, citations: [], references: [] }
		const thirdApiResponseJustTitle: IApiResponse = { paper: thirdPaper, citations: [], references: [] }
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
		let firstPaper = {} as IApiPaper;
		let secondPaper = {} as IApiPaper;
		let thirdPaper = {} as IApiPaper;
		Object.assign(firstPaper, standardPaper)
		Object.assign(firstPaper, { title: ["I am a Great-Paper"] })
		Object.assign(secondPaper, standardPaper)
		Object.assign(secondPaper, { title: ["i am a great paper"] })
		Object.assign(thirdPaper, standardPaper)
		Object.assign(thirdPaper, { title: ["I am a Great Paper"] })

		const firstApiResponseJustTitle: IApiResponse = { paper: firstPaper, citations: [], references: [] }
		const secondApiResponseJustTitle: IApiResponse = { paper: secondPaper, citations: [], references: [] }
		const thirdApiResponseJustTitle: IApiResponse = { paper: thirdPaper, citations: [], references: [] }

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

		let firstPaper = {} as IApiPaper;
		let secondPaper = {} as IApiPaper;
		let thirdPaper = {} as IApiPaper;
		Object.assign(firstPaper, standardPaper)
		Object.assign(firstPaper, { title: ["I am a Great-Paper"] })
		Object.assign(secondPaper, standardPaper)
		Object.assign(secondPaper, { title: ["I am a Great Paper"] })
		Object.assign(thirdPaper, standardPaper)
		Object.assign(thirdPaper, { title: ["i am a great paper"] })

		const firstApiResponseJustTitle: IApiResponse = { paper: firstPaper, citations: [], references: [] }
		const secondApiResponseJustTitle: IApiResponse = { paper: secondPaper, citations: [], references: [] }
		const thirdApiResponseJustTitle: IApiResponse = { paper: thirdPaper, citations: [], references: [] }
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

		let firstPaper = {} as IApiPaper;
		let secondPaper = {} as IApiPaper;
		let thirdPaper = {} as IApiPaper;
		Object.assign(firstPaper, standardPaper)
		Object.assign(firstPaper, { title: ["i am a great paper"] })
		Object.assign(secondPaper, standardPaper)
		Object.assign(secondPaper, { title: ["I am a Great Paper"] })
		Object.assign(thirdPaper, standardPaper)
		Object.assign(thirdPaper, { title: ["I am a Great-Paper"] })


		const firstApiResponseJustTitle: IApiResponse = { paper: firstPaper, citations: [], references: [] }
		const secondApiResponseJustTitle: IApiResponse = { paper: secondPaper, citations: [], references: [] }
		const thirdApiResponseJustTitle: IApiResponse = { paper: thirdPaper, citations: [], references: [] }
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
		let firstPaper = {} as IApiPaper;
		let secondPaper = {} as IApiPaper;
		let thirdPaper = {} as IApiPaper;
		Object.assign(firstPaper, standardPaper)
		Object.assign(firstPaper, { title: ["I am a Great Paper"] })
		Object.assign(secondPaper, standardPaper)
		Object.assign(secondPaper, { title: ["i am another paper"] })
		Object.assign(thirdPaper, standardPaper)
		Object.assign(thirdPaper, { title: ["I am a Great-Paper"] })


		const firstApiResponseJustTitle: IApiResponse = { paper: firstPaper, citations: [], references: [] }
		const secondApiResponseJustTitle: IApiResponse = { paper: secondPaper, citations: [], references: [] }
		const thirdApiResponseJustTitle: IApiResponse = { paper: thirdPaper, citations: [], references: [] }
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

		let firstPaper = {} as IApiPaper;
		let secondPaper = {} as IApiPaper;
		let thirdPaper = {} as IApiPaper;
		Object.assign(firstPaper, standardPaper)
		Object.assign(firstPaper, { title: ["I am a Great-Paper"], abstract: ["i am the abstract of the light side"] })
		Object.assign(secondPaper, standardPaper)
		Object.assign(secondPaper, { title: ["i am a great paper"] })
		Object.assign(thirdPaper, standardPaper)
		Object.assign(thirdPaper, { title: ["I am a Great Paper"] })


		const firstApiResponseJustTitle: IApiResponse = { paper: firstPaper, citations: [], references: [] }
		const secondApiResponseJustTitle: IApiResponse = { paper: secondPaper, citations: [], references: [] }
		const thirdApiResponseJustTitle: IApiResponse = { paper: thirdPaper, citations: [], references: [] }

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


		let firstPaper = {} as IApiPaper;
		let secondPaper = {} as IApiPaper;
		let thirdPaper = {} as IApiPaper;
		Object.assign(firstPaper, standardPaper)
		Object.assign(firstPaper, { title: ["I am a Great Paper"] })
		Object.assign(secondPaper, standardPaper)
		Object.assign(secondPaper, { title: ["I am a Great-Paper"], abstract: ["i am the abstract of the light side"] })
		Object.assign(thirdPaper, standardPaper)
		Object.assign(thirdPaper, { title: ["i am a great paper"] })

		const firstApiResponseJustTitle: IApiResponse = { paper: firstPaper, citations: [], references: [] }
		const secondApiResponseJustTitle: IApiResponse = { paper: secondPaper, citations: [], references: [] }
		const thirdApiResponseJustTitle: IApiResponse = { paper: thirdPaper, citations: [], references: [] }

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

		let firstPaper = {} as IApiPaper;
		let secondPaper = {} as IApiPaper;
		let thirdPaper = {} as IApiPaper;
		Object.assign(firstPaper, standardPaper)
		Object.assign(firstPaper, { title: ["I am a Great Paper"] })
		Object.assign(secondPaper, standardPaper)
		Object.assign(secondPaper, { title: ["I am a Great-Paper"], abstract: ["i am the abstract of the light side"] })
		Object.assign(thirdPaper, standardPaper)
		Object.assign(thirdPaper, { abstract: ["I am the Abstract: of the Light Side"] })


		const firstApiResponseJustTitle: IApiResponse = { paper: firstPaper, citations: [], references: [] }
		const secondApiResponseJustTitle: IApiResponse = { paper: secondPaper, citations: [], references: [] }
		const thirdApiResponseJustTitle: IApiResponse = { paper: thirdPaper, citations: [], references: [] }

		let merged = await apiMerger.compare([makePromise<IApiResponse>(firstApiResponseJustTitle), makePromise<IApiResponse>(secondApiResponseJustTitle), makePromise<IApiResponse>(thirdApiResponseJustTitle)]);
		assertEquals(merged.length, 1)
		assertEquals(merged[0].paper.title, ["I am a Great-Paper"])
		assertEquals(merged[0].paper.abstract, ["I am the Abstract: of the Light Side"])

	}

})

export const makePromise = async <T>(t: T) => {
	return t
}
