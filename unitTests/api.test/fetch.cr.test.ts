import { ApiMerger } from "../../src/api/apiMerger.ts";
import { IApiResponse } from "../../src/api/iApiResponse.ts";
import { IApiPaper, SourceApi } from "../../src/api/iApiPaper.ts";
import { assertEquals } from "https://deno.land/std@0.97.0/testing/asserts.ts"
import { CrossRefApi } from "../../src/api/crossRefApi.ts";
import { Stub, stub } from "https://deno.land/x/mock@v0.10.0/stub.ts";
import { IApiQuery } from "../../src/api/iApiQuery.ts";
import { IComparisonWeight } from "../../src/api/iComparisonWeight.ts";
import * as Mock from "../mockObjects/fetch/crossRefMock.test.ts"

const CR = new CrossRefApi("");
const query: IApiQuery = {
	id: "tst",
	rawName: "alexander raschke",
	title: "Adaptive Exterior Light and Speed Control System",
	enabledApis: [[SourceApi.CR,"luca999@web.de"], [SourceApi.MA,"9a02225751354cd29397eba3f5382101"]],
	aggression: {} as IComparisonWeight
}

let body = new Blob([JSON.stringify(Mock.paperResponse, null, 2)], { type: 'application/json' })
let response = new Response(body, { 'headers': Mock.headers });

Deno.test({
	name: "CrossRefApi empty request",
	async fn(): Promise<void> {
		let bodyValues = {} as any;
		let body = new Blob([bodyValues], { type: 'application/json' })
		let response = new Response(body, { 'headers': Mock.headers });
		stub(globalThis, "fetch", () => { return response });
		let res = await CR.fetch(query);
		assertEquals(res, { "paper": {}, "citations": [], "references": [] });
	}
})

Deno.test({
	name: "CrossRefApi valid paper, but no cites and refs",
	async fn(): Promise<void> {
		stub(globalThis, "fetch", () => { return response });

		let res = await CR.fetch(query);
		assertEquals(res, {
			"paper": Mock.parsedPaper, "citations": [], "references": []
		});
	}
})

Deno.test({
	name: "CrossRefApi invalid references",
	async fn(): Promise<void> {
		stub(globalThis, "fetch", () => { return {} as Response });

		let res = await CR.getChildObjects([Mock.referenceRequest]);
		assertEquals(res, [{} as IApiPaper]);
	}
})

Deno.test({
	name: "CrossRefApi valid references",
	async fn(): Promise<void> {
		let body = new Blob([JSON.stringify(Mock.referenceResponse, null, 2)], { type: 'application/json' })
		let response = new Response(body, { 'headers': Mock.headers });
		stub(globalThis, "fetch", () => { return response });

		let res = await CR.getChildObjects([Mock.referenceRequest]);
		assertEquals(res, [Mock.parsedReference]);
	}
})

Deno.test({
	name: "CrossRefApi invalid citations",
	async fn(): Promise<void> {
		stub(globalThis, "fetch", () => { return {} as Response });

		let res = await CR.getChildObjects([Mock.referenceRequest]);
		assertEquals(res, [{} as IApiPaper]);
	}
})

