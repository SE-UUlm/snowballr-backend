import { ApiMerger } from "../../src/api/apiMerger.ts";
import { IApiResponse } from "../../src/api/iApiResponse.ts";
import { IApiPaper, SourceApi } from "../../src/api/iApiPaper.ts";
import { assertEquals } from "https://deno.land/std@0.150.0/testing/asserts.ts"
import { idType } from "../../src/api/iApiUniqueId.ts";

export const standardPaper: IApiPaper = { title: [], abstract: [], author: [], year: [], publisher: [], numberOfCitations: [], numberOfReferences: [], type: [], scope: [], scopeName: [], pdf: [], uniqueId: [], source: [], raw: [] }

Deno.test({
	name: "test author split firstname, lastname",
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
		Object.assign(firstPaper, { "title": ["awsome paper title"], "author": [{ "orcid": [], "rawString": ["samuel idowu"], "lastName": [], "firstName": [] }, { "orcid": [], "rawString": ["max muster"], "lastName": ["Muster"], "firstName": ["Max"] }] })
		Object.assign(secondPaper, standardPaper)
		Object.assign(secondPaper, { "title": ["awsome paper title"], "author": [{ "orcid": [], "rawString": ["Samuel Idowu"], "lastName": [], "firstName": [] }, { "orcid": [], "rawString": ["Max Muster"], "lastName": ["muster"], "firstName": ["max"] }] })
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

Deno.test({
	name: "Merge Cites",
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
		let fifthPaper = {} as IApiPaper;
		let sixthPaper = {} as IApiPaper;
		Object.assign(firstPaper, standardPaper)
		Object.assign(firstPaper, { title: ["Hello There"] })
		Object.assign(secondPaper, standardPaper)
		Object.assign(secondPaper, { title: ["Hello There"] })
		Object.assign(thirdPaper, standardPaper)
		Object.assign(thirdPaper, { title: ["general kenobi"], abstract: ["I am the Abstract: of the Light Side"] })
		Object.assign(fourthPaper, standardPaper)
		Object.assign(fourthPaper, { title: ["general Kenobi"], abstract: ["I am the Abstract: of the Light Side"] })
		Object.assign(fifthPaper, standardPaper)
		Object.assign(fifthPaper, { title: ["General Kenobi"], abstract: ["I am the Abstract: of the Light Side"] })
		Object.assign(sixthPaper, standardPaper)
		Object.assign(sixthPaper, { title: ["general kenobi"], abstract: ["I am the Abstract: of the Light Side"] })


		const firstApiResponseJustTitle: IApiResponse = { paper: firstPaper, citations: [thirdPaper, fourthPaper], references: [] }
		const secondApiResponseJustTitle: IApiResponse = { paper: secondPaper, citations: [fifthPaper, sixthPaper], references: [] }

		let merged = await apiMerger.compare([makePromise<IApiResponse>(firstApiResponseJustTitle), makePromise<IApiResponse>(secondApiResponseJustTitle)]);
		assertEquals(merged.length, 1)
		assertEquals(merged[0].paper.title, ["Hello There"])
		assertEquals(merged[0].citations!.length, 1)

	}

})

Deno.test({
	name: "Merge Refs",
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
		let fifthPaper = {} as IApiPaper;
		let sixthPaper = {} as IApiPaper;
		Object.assign(firstPaper, standardPaper)
		Object.assign(firstPaper, { title: ["Hello There"] })
		Object.assign(secondPaper, standardPaper)
		Object.assign(secondPaper, { title: ["Hello There"] })
		Object.assign(thirdPaper, standardPaper)
		Object.assign(thirdPaper, { title: ["general kenobi"], abstract: ["I am the Abstract: of the Light Side"] })
		Object.assign(fourthPaper, standardPaper)
		Object.assign(fourthPaper, { title: ["general Kenobi"], abstract: ["I am the Abstract: of the Light Side"] })
		Object.assign(fifthPaper, standardPaper)
		Object.assign(fifthPaper, { title: ["General Kenobi"], abstract: ["I am the Abstract: of the Light Side"] })
		Object.assign(sixthPaper, standardPaper)
		Object.assign(sixthPaper, { title: ["general kenobi"], abstract: ["I am the Abstract: of the Light Side"] })


		const firstApiResponseJustTitle: IApiResponse = { paper: firstPaper, citations: [], references: [thirdPaper, fourthPaper] }
		const secondApiResponseJustTitle: IApiResponse = { paper: secondPaper, citations: [], references: [fifthPaper, sixthPaper] }

		let merged = await apiMerger.compare([makePromise<IApiResponse>(firstApiResponseJustTitle), makePromise<IApiResponse>(secondApiResponseJustTitle)]);
		assertEquals(merged.length, 1)
		assertEquals(merged[0].paper.title, ["Hello There"])
		assertEquals(merged[0].references!.length, 1)

	}

})

export const makePromise = async <T>(t: T) => {
	return t
}

Deno.test({
	name: "Merge 2 same doi, but different title",
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
		Object.assign(firstPaper, { uniqueId: [{ type: idType.DOI, value: "1234" }], title: ["I am a Great Paper"], source: ["MA"] })
		Object.assign(secondPaper, standardPaper)
		Object.assign(secondPaper, { uniqueId: [{ type: idType.DOI, value: "1234" }], title: ["I am a different Title"], source: ["BA"] })


		const firstApiResponseJustTitle: IApiResponse = { paper: firstPaper, citations: [], references: [] }
		const secondApiResponseJustTitle: IApiResponse = { paper: secondPaper, citations: [], references: [] }

		let merged = await apiMerger.compare([makePromise<IApiResponse>(firstApiResponseJustTitle), makePromise<IApiResponse>(secondApiResponseJustTitle)]);
		assertEquals(merged.length, 1)
		assertEquals(merged[0].paper.title, ["I am a Great Paper", "I am a different Title"])
		assertEquals(merged[0].paper.titleSource, [["MA"], ["BA"]])

	}

})

Deno.test({
	name: "Merge 3 same doi, but 2different titles",
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
		Object.assign(firstPaper, { uniqueId: [{ type: idType.DOI, value: "1234" }], title: ["I am a Great Paper"], source: ["MA"] })
		Object.assign(secondPaper, standardPaper)
		Object.assign(secondPaper, { uniqueId: [{ type: idType.DOI, value: "1234" }], title: ["I am a different Title"], source: ["BA"] })
		Object.assign(thirdPaper, standardPaper)
		Object.assign(thirdPaper, { uniqueId: [{ type: idType.DOI, value: "1234" }], title: ["I am a different-Title"], source: ["CA"] })

		const firstApiResponseJustTitle: IApiResponse = { paper: firstPaper, citations: [], references: [] }
		const secondApiResponseJustTitle: IApiResponse = { paper: secondPaper, citations: [], references: [] }
		const thirdApiResponseJustTitle: IApiResponse = { paper: thirdPaper, citations: [], references: [] }
		let merged = await apiMerger.compare([makePromise<IApiResponse>(firstApiResponseJustTitle), makePromise<IApiResponse>(secondApiResponseJustTitle), makePromise<IApiResponse>(thirdApiResponseJustTitle)]);
		assertEquals(merged.length, 1)
		assertEquals(merged[0].paper.title, ["I am a different-Title", "I am a Great Paper"])
		assertEquals(merged[0].paper.titleSource, [["BA", "CA"], ["MA"]])

	}

})

Deno.test({
	name: "Merge 3 same doi, but 3 different titles",
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
		Object.assign(firstPaper, { uniqueId: [{ type: idType.DOI, value: "1234" }], title: ["I am a Great Paper"], source: ["MA"] })
		Object.assign(secondPaper, standardPaper)
		Object.assign(secondPaper, { uniqueId: [{ type: idType.DOI, value: "1234" }], title: ["I am a different Title"], source: ["BA"] })
		Object.assign(thirdPaper, standardPaper)
		Object.assign(thirdPaper, { uniqueId: [{ type: idType.DOI, value: "1234" }], title: ["I am a very different-Title"], source: ["CA"] })

		const firstApiResponseJustTitle: IApiResponse = { paper: firstPaper, citations: [], references: [] }
		const secondApiResponseJustTitle: IApiResponse = { paper: secondPaper, citations: [], references: [] }
		const thirdApiResponseJustTitle: IApiResponse = { paper: thirdPaper, citations: [], references: [] }
		let merged = await apiMerger.compare([makePromise<IApiResponse>(firstApiResponseJustTitle), makePromise<IApiResponse>(secondApiResponseJustTitle), makePromise<IApiResponse>(thirdApiResponseJustTitle)]);
		assertEquals(merged.length, 1)
		assertEquals(merged[0].paper.title, ["I am a Great Paper", "I am a different Title", "I am a very different-Title"])
		assertEquals(merged[0].paper.titleSource, [["MA"], ["BA"], ["CA"]])

	}

})

Deno.test({
	name: "Merge 3 same doi, but 2 different titles with one not there",
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
		Object.assign(firstPaper, { uniqueId: [{ type: idType.DOI, value: "1234" }], title: ["I am a Great Paper"], source: ["MA"] })
		Object.assign(secondPaper, standardPaper)
		Object.assign(secondPaper, { uniqueId: [{ type: idType.DOI, value: "1234" }], title: [], source: ["BA"] })
		Object.assign(thirdPaper, standardPaper)
		Object.assign(thirdPaper, { uniqueId: [{ type: idType.DOI, value: "1234" }], title: ["I am a very different-Title"], source: ["CA"] })

		const firstApiResponseJustTitle: IApiResponse = { paper: firstPaper, citations: [], references: [] }
		const secondApiResponseJustTitle: IApiResponse = { paper: secondPaper, citations: [], references: [] }
		const thirdApiResponseJustTitle: IApiResponse = { paper: thirdPaper, citations: [], references: [] }
		let merged = await apiMerger.compare([makePromise<IApiResponse>(firstApiResponseJustTitle), makePromise<IApiResponse>(secondApiResponseJustTitle), makePromise<IApiResponse>(thirdApiResponseJustTitle)]);
		assertEquals(merged.length, 1)
		assertEquals(merged[0].paper.title, ["I am a Great Paper", "I am a very different-Title"])
		assertEquals(merged[0].paper.titleSource, [["MA"], ["CA"]])

	}

})


Deno.test({
	name: "Merge 9 same doi, but different titles",
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
		let fifthPaper = {} as IApiPaper;
		let sixthPaper = {} as IApiPaper;
		let seventhPaper = {} as IApiPaper;
		let eighthPaper = {} as IApiPaper;
		let ninethPaper = {} as IApiPaper;
		Object.assign(firstPaper, standardPaper)
		Object.assign(firstPaper, { uniqueId: [{ type: idType.DOI, value: "1234" }], title: [], source: ["B1"] })
		Object.assign(secondPaper, standardPaper)
		Object.assign(secondPaper, { uniqueId: [{ type: idType.DOI, value: "1234" }], title: ["I am a Great Paper"], source: ["B2"] })
		Object.assign(thirdPaper, standardPaper)
		Object.assign(thirdPaper, { uniqueId: [{ type: idType.DOI, value: "1234" }], title: ["I am a very different-Title"], source: ["B3"] })
		Object.assign(fourthPaper, standardPaper)
		Object.assign(fourthPaper, { uniqueId: [{ type: idType.DOI, value: "1234" }], title: ["I am a great paper"], source: ["B4"] })
		Object.assign(fifthPaper, standardPaper)
		Object.assign(fifthPaper, { uniqueId: [{ type: idType.DOI, value: "1234" }], title: ["I am a very different Title"], source: ["B5"] })
		Object.assign(sixthPaper, standardPaper)
		Object.assign(sixthPaper, { uniqueId: [{ type: idType.DOI, value: "1234" }], title: ["I am a very different Title"], source: ["B6"] })
		Object.assign(seventhPaper, standardPaper)
		Object.assign(seventhPaper, { uniqueId: [{ type: idType.DOI, value: "1234" }], title: ["I am a great paper"], source: ["B7"] })
		Object.assign(eighthPaper, standardPaper)
		Object.assign(eighthPaper, { uniqueId: [{ type: idType.DOI, value: "1234" }], title: [], source: ["B8"] })
		Object.assign(ninethPaper, standardPaper)
		Object.assign(ninethPaper, { uniqueId: [{ type: idType.DOI, value: "1234" }], title: ["I am a very different-Title"], source: ["B9"] })
		const firstApiResponseJustTitle: IApiResponse = { paper: firstPaper, citations: [], references: [] }
		const secondApiResponseJustTitle: IApiResponse = { paper: secondPaper, citations: [], references: [] }
		const thirdApiResponseJustTitle: IApiResponse = { paper: thirdPaper, citations: [], references: [] }
		const fourthApiResponseJustTitle: IApiResponse = { paper: fourthPaper, citations: [], references: [] }
		const fifthApiResponseJustTitle: IApiResponse = { paper: fifthPaper, citations: [], references: [] }
		const sixthApiResponseJustTitle: IApiResponse = { paper: sixthPaper, citations: [], references: [] }
		const seventhApiResponseJustTitle: IApiResponse = { paper: seventhPaper, citations: [], references: [] }
		const eighthApiResponseJustTitle: IApiResponse = { paper: eighthPaper, citations: [], references: [] }
		const ninethApiResponseJustTitle: IApiResponse = { paper: ninethPaper, citations: [], references: [] }

		let merged = await apiMerger.compare([makePromise<IApiResponse>(firstApiResponseJustTitle), makePromise<IApiResponse>(secondApiResponseJustTitle)
			, makePromise<IApiResponse>(thirdApiResponseJustTitle)
			, makePromise<IApiResponse>(fourthApiResponseJustTitle)
			, makePromise<IApiResponse>(fifthApiResponseJustTitle)
			, makePromise<IApiResponse>(sixthApiResponseJustTitle)
			, makePromise<IApiResponse>(seventhApiResponseJustTitle)
			, makePromise<IApiResponse>(eighthApiResponseJustTitle)
			, makePromise<IApiResponse>(ninethApiResponseJustTitle)
		]);
		assertEquals(merged.length, 1)
		assertEquals(merged[0].paper.title, ["I am a very different-Title", "I am a Great Paper"])
		assertEquals(merged[0].paper.titleSource, [["B3", "B5", "B6", "B9"], ["B2", "B4", "B7"]])

	}

})

Deno.test({
	name: "Merge 2 same doi, but different year",
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
		Object.assign(firstPaper, { uniqueId: [{ type: idType.DOI, value: "1234" }], year: [2009], source: ["MA"] })
		Object.assign(secondPaper, standardPaper)
		Object.assign(secondPaper, { uniqueId: [{ type: idType.DOI, value: "1234" }], year: [2010], source: ["BA"] })


		const firstApiResponseJustTitle: IApiResponse = { paper: firstPaper, citations: [], references: [] }
		const secondApiResponseJustTitle: IApiResponse = { paper: secondPaper, citations: [], references: [] }

		let merged = await apiMerger.compare([makePromise<IApiResponse>(firstApiResponseJustTitle), makePromise<IApiResponse>(secondApiResponseJustTitle)]);
		assertEquals(merged.length, 1)
		assertEquals(merged[0].paper.year, [2009, 2010])
		assertEquals(merged[0].paper.yearSource, [["MA"], ["BA"]])

	}

})

Deno.test({
	name: "Merge 9 same doi, but different years",
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
		let fifthPaper = {} as IApiPaper;
		let sixthPaper = {} as IApiPaper;
		let seventhPaper = {} as IApiPaper;
		let eighthPaper = {} as IApiPaper;
		let ninethPaper = {} as IApiPaper;
		Object.assign(firstPaper, standardPaper)
		Object.assign(firstPaper, { uniqueId: [{ type: idType.DOI, value: "1234" }], year: [2009], source: ["B1"] })
		Object.assign(secondPaper, standardPaper)
		Object.assign(secondPaper, { uniqueId: [{ type: idType.DOI, value: "1234" }], year: [2010], source: ["B2"] })
		Object.assign(thirdPaper, standardPaper)
		Object.assign(thirdPaper, { uniqueId: [{ type: idType.DOI, value: "1234" }], year: [2009], source: ["B3"] })
		Object.assign(fourthPaper, standardPaper)
		Object.assign(fourthPaper, { uniqueId: [{ type: idType.DOI, value: "1234" }], title: ["I am a great paper"], source: ["B4"] })
		Object.assign(fifthPaper, standardPaper)
		Object.assign(fifthPaper, { uniqueId: [{ type: idType.DOI, value: "1234" }], year: [2009], source: ["B5"] })
		Object.assign(sixthPaper, standardPaper)
		Object.assign(sixthPaper, { uniqueId: [{ type: idType.DOI, value: "1234" }], year: [2012], source: ["B6"] })
		Object.assign(seventhPaper, standardPaper)
		Object.assign(seventhPaper, { uniqueId: [{ type: idType.DOI, value: "1234" }], year: [2009], source: ["B7"] })
		Object.assign(eighthPaper, standardPaper)
		Object.assign(eighthPaper, { uniqueId: [{ type: idType.DOI, value: "1234" }], year: [2012], source: ["B8"] })
		Object.assign(ninethPaper, standardPaper)
		Object.assign(ninethPaper, { uniqueId: [{ type: idType.DOI, value: "1234" }], year: [], source: ["B9"] })
		const firstApiResponseJustTitle: IApiResponse = { paper: firstPaper, citations: [], references: [] }
		const secondApiResponseJustTitle: IApiResponse = { paper: secondPaper, citations: [], references: [] }
		const thirdApiResponseJustTitle: IApiResponse = { paper: thirdPaper, citations: [], references: [] }
		const fourthApiResponseJustTitle: IApiResponse = { paper: fourthPaper, citations: [], references: [] }
		const fifthApiResponseJustTitle: IApiResponse = { paper: fifthPaper, citations: [], references: [] }
		const sixthApiResponseJustTitle: IApiResponse = { paper: sixthPaper, citations: [], references: [] }
		const seventhApiResponseJustTitle: IApiResponse = { paper: seventhPaper, citations: [], references: [] }
		const eighthApiResponseJustTitle: IApiResponse = { paper: eighthPaper, citations: [], references: [] }
		const ninethApiResponseJustTitle: IApiResponse = { paper: ninethPaper, citations: [], references: [] }

		let merged = await apiMerger.compare([makePromise<IApiResponse>(firstApiResponseJustTitle), makePromise<IApiResponse>(secondApiResponseJustTitle)
			, makePromise<IApiResponse>(thirdApiResponseJustTitle)
			, makePromise<IApiResponse>(fourthApiResponseJustTitle)
			, makePromise<IApiResponse>(fifthApiResponseJustTitle)
			, makePromise<IApiResponse>(sixthApiResponseJustTitle)
			, makePromise<IApiResponse>(seventhApiResponseJustTitle)
			, makePromise<IApiResponse>(eighthApiResponseJustTitle)
			, makePromise<IApiResponse>(ninethApiResponseJustTitle)
		]);
		assertEquals(merged.length, 1)
		assertEquals(merged[0].paper.year, [2009, 2010, 2012])
		assertEquals(merged[0].paper.yearSource, [["B1", "B3", "B5", "B7"], ["B2"], ["B6", "B8"]])

	}

})
