import { ApiMerger } from "../../src/api/apiMerger.ts";
import { IApiResponse } from "../../src/api/iApiResponse.ts";
import { IApiPaper, SourceApi } from "../../src/api/iApiPaper.ts";
import { assertEquals } from "https://deno.land/std@0.97.0/testing/asserts.ts"
import { Stub, stub } from "https://deno.land/x/mock@v0.10.0/stub.ts";
import { IApiQuery } from "../../src/api/iApiQuery.ts";
import { IComparisonWeight } from "../../src/api/iComparisonWeight.ts";
import * as Mock from "../mockObjects/fetch/crossRefMock.test.ts"
import { MicrosoftResearchApi } from "../../src/api/microsoftResearchApi.ts";

const MA = new MicrosoftResearchApi("", "");
const query: IApiQuery = {
	id: "tst",
	rawName: "alexander raschke",
	title: "Adaptive Exterior Light and Speed Control System",
	enabledApis: [SourceApi.MA],
	aggression: {} as IComparisonWeight
}

let body = new Blob([JSON.stringify(Mock.paperResponse, null, 2)], { type: 'application/json' })
let response = new Response(body);

Deno.test({
	name: "MicrosoftResearchApi empty request",
	async fn(): Promise<void> {
		let bodyValues = {} as any;
		let body = new Blob([bodyValues], { type: 'application/json' })
		let response = new Response(body, { 'headers': Mock.headers });
		stub(globalThis, "fetch", () => { return response });
		let res = await MA.fetch(query);
		assertEquals(res, { "paper": {}, "citations": [], "references": [] });
	}
})

Deno.test({
	name: "MicrosoftResearchApi valid paper, but no cites and refs",
	async fn(): Promise<void> {
		stub(globalThis, "fetch", () => { return response });

		let res = await MA.fetch(query);
		assertEquals(res, {
			"paper": Mock.parsedPaper, "citations": [], "references": []
		});
	}
})

// Deno.test({
// 	name: "MicrosoftResearchApi invalid references",
// 	async fn(): Promise<void> {
// 		stub(globalThis, "fetch", () => { return {} as Response });

// 		let res = await MA.getChildObjects([Mock.referenceRequest]);
// 		assertEquals(res, [{} as IApiPaper]);
// 	}
// })

// Deno.test({
// 	name: "MicrosoftResearchApi valid references",
// 	async fn(): Promise<void> {
// 		let body = new Blob([JSON.stringify(Mock.referenceResponse, null, 2)], { type: 'application/json' })
// 		let response = new Response(body, { 'headers': Mock.headers });
// 		stub(globalThis, "fetch", () => { return response });

// 		let res = await MA.getChildObjects([Mock.referenceRequest]);
// 		assertEquals(res, [Mock.parsedReference]);
// 	}
// })

// Deno.test({
// 	name: "MicrosoftResearchApi invalid citations",
// 	async fn(): Promise<void> {
// 		stub(globalThis, "fetch", () => { return {} as Response });

// 		let res = await MA.getChildObjects([Mock.referenceRequest]);
// 		assertEquals(res, [{} as IApiPaper]);
// 	}
// })
