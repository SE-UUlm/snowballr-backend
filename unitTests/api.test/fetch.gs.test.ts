import { ApiMerger } from "../../src/api/apiMerger.ts";
import { IApiResponse } from "../../src/api/iApiResponse.ts";
import { IApiPaper, SourceApi } from "../../src/api/iApiPaper.ts";
import { assertEquals } from "https://deno.land/std@0.150.0/testing/asserts.ts"
import { GoogleScholar } from "../../src/api/googleScholar.ts";
import { stub } from "https://deno.land/x/mock@0.15.2/mock.ts";
import { IApiQuery } from "../../src/api/iApiQuery.ts";
import { IComparisonWeight } from "../../src/api/iComparisonWeight.ts";
import * as Mock from "../mockObjects/fetch/googleScholarMock.test.ts"
import { logger } from "../../src/api/logger.ts";

const GS = new GoogleScholar("https://scholar.google.com", "");
const query: IApiQuery = {
	id: "tst",
	rawName: "alexander raschke",
	title: "Adaptive Exterior Light and Speed Control System",
	enabledApis: [[SourceApi.GS]],
	aggression: {} as IComparisonWeight
}

//console.log(Mock.paperResponse)
let body = new Blob([Mock.paperResponse], { type: 'application/json' });
let response = new Response(body, { 'headers': Mock.headers });

Deno.test({
	name: "GoogleScholar empty request",
	async fn(): Promise<void> {
		const bodyValues = {} as any;
		const body = new Blob([bodyValues], { type: 'text/html' })
		let response = new Response(body, { 'headers': Mock.headers });
		stub(globalThis, "fetch", () => { return response });
		let res = await GS.fetch(query);

		assertEquals(res, { "paper": {}, "citations": [], "references": [] });
	}
})

Deno.test({
	name: "GoogleScholar valid paper, but no cites and refs",
	async fn(): Promise<void> {
		let noCites = Mock.paperResponse.replace("Cited by 8", "Cited by 0");
		body = new Blob([noCites], { type: 'application/json' });
		response = new Response(body, { 'headers': Mock.headers });
		stub(globalThis, "fetch", () => { return response });
		let res = await GS.fetch(query);
		assertEquals(res, {
			"paper": Mock.parsedPaperNoCites, "citations": [], "references": []
		});
	}
})


Deno.test({
	name: "GoogleScholar valid citations",
	async fn(): Promise<void> {
		let responseList = [Mock.paperResponse, Mock.citationResponse];
		let counter = 0;
		stub(globalThis, "fetch", () => {
			let body = new Blob([responseList[counter++]], { type: 'application/json' })
			let response = new Response(body, { 'headers': Mock.headers });
			return response;
		});

		let res = await GS.fetch(query);
		assertEquals(res, {
			"paper": Mock.parsedPaper, "citations": Mock.parsedCitations, "references": []
		});
	}
})

Deno.test({
	name: "GoogleScholar: detect captcha and exchange proxy",
	async fn(): Promise<void> {
		let responseList = [Mock.captchaResponse];
		let counter = 0;
		stub(globalThis, "fetch", () => {
			let body = new Blob([responseList[counter++]], { type: 'application/json' })
			let response = new Response(body, { 'headers': Mock.headers });
			return response;
		});
		let proxyExchanged = false
		//stub(GS, "_getCitations", () => { logger.info("here"); throw new Error("") })
		stub(GS, "_rotateProxy", () => { proxyExchanged = true; throw new Error("Cancelled by unittest") })


		let res = await GS.fetch(query);
		assertEquals(proxyExchanged, true);
	}
})

Deno.test({
	name: "GoogleScholar get blocked http call and exchange proxy",
	async fn(): Promise<void> {

		stub(globalThis, "fetch", () => {
			let body = new Blob([""], { type: 'application/json' })
			let response = new Response(body, { 'headers': Mock.headers, 'status': 503 });
			return response;
		});
		let proxyExchanged = false
		stub(GS, "_rotateProxy", () => { proxyExchanged = true; throw new Error("Cancelled by unittest") })

		await GS.fetch(query);
		assertEquals(proxyExchanged, true);
	}
})


Deno.test({
	name: "GoogleScholarProxy: Tor docker container exchange",
	async fn(): Promise<void> {

		stub(globalThis, "fetch", () => {
			let body = new Blob([""], { type: 'application/json' })
			let response = new Response(body, { 'headers': Mock.headers, 'status': 503 });
			return response;
		});
		let proxyExchanged = false
		stub(GS, "_rotateProxy", () => { proxyExchanged = true; throw new Error("Cancelled by unittest") })

		await GS.fetch(query);
		assertEquals(proxyExchanged, true);
	}
})
// Deno.test({
// 	name: "CrossRefApi valid paper, but no cites and refs",
// 	async fn(): Promise<void> {
// 		stub(globalThis, "fetch", () => { return response });

// 		let res = await CR.fetch(query);
// 		assertEquals(res, {
// 			"paper": Mock.parsedPaper, "citations": [], "references": []
// 		});
// 	}
// })

// Deno.test({
// 	name: "CrossRefApi invalid references",
// 	async fn(): Promise<void> {
// 		stub(globalThis, "fetch", () => { return {} as Response });

// 		let res = await CR.getChildObjects([Mock.referenceRequest]);
// 		assertEquals(res, [{} as IApiPaper]);
// 	}
// })

// Deno.test({
// 	name: "CrossRefApi valid references",
// 	async fn(): Promise<void> {
// 		let body = new Blob([JSON.stringify(Mock.referenceResponse, null, 2)], { type: 'application/json' })
// 		let response = new Response(body, { 'headers': Mock.headers });
// 		stub(globalThis, "fetch", () => { return response });

// 		let res = await CR.getChildObjects([Mock.referenceRequest]);
// 		assertEquals(res, [Mock.parsedReference]);
// 	}
// })

// Deno.test({
// 	name: "CrossRefApi invalid citations",
// 	async fn(): Promise<void> {
// 		stub(globalThis, "fetch", () => { return {} as Response });

// 		let res = await CR.getChildObjects([Mock.referenceRequest]);
// 		assertEquals(res, [{} as IApiPaper]);
// 	}
// })

