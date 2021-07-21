import { ApiMerger } from "../../src/api/apiMerger.ts";
import { IApiResponse } from "../../src/api/iApiResponse.ts";
import { IApiPaper, SourceApi } from "../../src/api/iApiPaper.ts";
import { assertEquals } from "https://deno.land/std@0.97.0/testing/asserts.ts"
import { CrossRefApi } from "../../src/api/crossRefApi.ts";
import { Stub, stub } from "https://deno.land/x/mock@v0.10.0/stub.ts";
import { IApiQuery } from "../../src/api/iApiQuery.ts";
import { IComparisonWeight } from "../../src/api/iComparisonWeight.ts";
import { logger } from "../../src/api/logger.ts";

// Deno.test({
// 	name: "test 2 papers merge author",
// 	async fn(): Promise<void> {
// 		let apiMerger = new ApiMerger({
// 			titleWeight: 10,
// 			titleLevenshtein: 10,
// 			abstractWeight: 7,
// 			abstractLevenshtein: 0,
// 			authorWeight: 8,
// 			overallWeight: 0.85,
// 			yearWeight: 2
// 		})
// 		const firstPaper: IApiPaper = { "title": ["awsome paper title"], "author": [{ "id": [], "orcid": [], "rawString": ["samuel idowu"], "lastName": ["idowu"], "firstName": ["samuel"] }, { "id": [], "orcid": [], "rawString": ["max muster"], "lastName": ["Muster"], "firstName": ["Max"] }] }
// 		const secondPaper: IApiPaper = { "title": ["awsome paper title"], "author": [{ "id": [], "orcid": [], "rawString": ["Samuel Idowu"], "lastName": ["Idowu"], "firstName": ["Samuel"] }, { "id": [], "orcid": [], "rawString": ["Max Muster"], "lastName": ["muster"], "firstName": ["max"] }] }
// 		const firstApiResponseJustTitle: IApiResponse = { paper: firstPaper, citations: [], references: [] }
// 		const secondApiResponseJustTitle: IApiResponse = { paper: secondPaper, citations: [], references: [] }
// 		let merged = await apiMerger.compare([makePromise<IApiResponse>(firstApiResponseJustTitle), makePromise<IApiResponse>(secondApiResponseJustTitle)]);
// 		assertEquals(merged.length, 1)
// 		assertEquals(merged[0].paper.author![0]!.firstName!, ["Samuel"])
// 		assertEquals(merged[0].paper.author![0]!.lastName!, ["Idowu"])
// 		assertEquals(merged[0].paper.author![0]!.rawString!, ["Samuel Idowu"])
// 	}

// })

//const CR = new CrossRefApi("https://api.crossref.org/works", "lukas.romer@uni-ulm.de");
const CR = new CrossRefApi("");
const query: IApiQuery = {
	id: "tst",
	rawName: "alexander raschke",
	title: "Adaptive Exterior Light and Speed Control System",
	enabledApis: [SourceApi.MA, SourceApi.CR],
	aggressivity: {} as IComparisonWeight
}

let headers = new Headers({
	"access-control-allow-headers": "X-Requested-With",
	"access-control-allow-origin": "*",
	connection: "close",
	"content-type": "application/json",
	date: "Wed, 21 Jul 2021 08:56:32 GMT",
	"permissions-policy": "interest-cohort=()",
	server: "Jetty(9.4.40.v20210413)",
	vary: "Accept-Encoding",
	"x-rate-limit-interval": "1s",
	"x-rate-limit-limit": "60",
	"x-ratelimit-interval": "1s",
	"x-ratelimit-limit": "50"
})

let bodyValues = {
	'message': {
		'publisher': "Springer International Publishing",
		DOI: "10.1007/978-3-030-48077-6_24",
		type: "book-chapter",
		title: ["Adaptive Exterior Light and Speed Control System"],
		author: [
			{ given: "Frank", family: "Houdek", sequence: "first", affiliation: [Array] },
			{
				given: "Alexander",
				family: "Raschke",
				sequence: "additional",
				affiliation: [Array]
			}
		],
	}
}

let body = new Blob([JSON.stringify(bodyValues, null, 2)], { type: 'application/json' })
let response = new Response(body, { 'headers': headers });


Deno.test({
	name: "CrossRefApi empty request",
	async fn(): Promise<void> {
		let bodyValues = {};
		let body = new Blob([JSON.stringify(bodyValues, null, 2)], { type: 'application/json' })
		let response = new Response(body, { 'headers': headers });
		stub(globalThis, "fetch", () => { return response });
		let res = await CR.fetch(query);
		assertEquals(res, { "paper": {}, "citations": [], "references": [] });
	}
})

Deno.test({
	name: "CrossRefApi valid paper, but no cites and refs",
	async fn(): Promise<void> {
		//let f = fetch("google.de");

		//const a = {} as unknown;
		stub(globalThis, "fetch", () => { return response });

		let res = await CR.fetch(query);
		assertEquals(res, {
			"paper": {
				id: undefined,
				title: ["Adaptive Exterior Light and Speed Control System"],
				author: [
					{
						id: undefined,
						orcid: [],
						rawString: ["Frank, Houdek"],
						lastName: ["Houdek"],
						firstName: ["Frank"]
					},
					{
						id: undefined,
						orcid: [],
						rawString: ["Alexander, Raschke"],
						lastName: ["Raschke"],
						firstName: ["Alexander"]
					}
				],
				abstract: [],
				numberOfReferences: [4],
				numberOfCitations: [],
				year: [],
				publisher: ["Springer International Publishing"],
				type: ["book-chapter"],
				scope: [],
				scopeName: [],
				pdf: ["http://link.springer.com/content/pdf/10.1007/978-3-030-48077-6_24"],
				uniqueId: [
					{ id: undefined, type: "ISSN", value: ["0302-9743", "1611-3349"] },
					{ id: undefined, type: "DOI", value: "10.1007/978-3-030-48077-6_24" }
				],
				source: ["crossRef"],
				raw: []
			}, "citations": [], "references": []
		});
	}
})