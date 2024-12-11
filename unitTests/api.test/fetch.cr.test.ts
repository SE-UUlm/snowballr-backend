import { IApiPaper, SourceApi } from "../../src/api/iApiPaper.ts";
import { assertEquals } from "https://deno.land/std@0.150.0/testing/asserts.ts";
import { CrossRefApi } from "../../src/api/crossRefApi.ts";
import { stub } from "https://deno.land/x/mock@0.15.2/mock.ts";
import { IApiQuery } from "../../src/api/iApiQuery.ts";
import { IComparisonWeight } from "../../src/api/iComparisonWeight.ts";
import * as Mock from "../mockObjects/fetch/crossRefMock.test.ts";

const CR = new CrossRefApi("");
const query: IApiQuery = {
  id: "tst",
  rawName: "alexander raschke",
  title: "Adaptive Exterior Light and Speed Control System",
  enabledApis: [[SourceApi.CR, "luca999@web.de"], [
    SourceApi.MA,
    "9a02225751354cd29397eba3f5382101",
  ]],
  aggression: {} as IComparisonWeight,
};

const body = new Blob([JSON.stringify(Mock.paperResponse, null, 2)], {
  type: "application/json",
});
const response = new Response(body, { "headers": Mock.headers });

Deno.test({
  name: "CrossRefApi empty request",
  async fn(): Promise<void> {
    const bodyValues = {} as any;
    const body = new Blob([bodyValues], { type: "application/json" });
    const response = new Response(body, { "headers": Mock.headers });
    stub(globalThis, "fetch", () => {
      return response;
    });
    const res = await CR.fetch(query);
    assertEquals(res, { "paper": {}, "citations": [], "references": [] });
  },
});

Deno.test({
  name: "CrossRefApi valid paper, but no cites and refs",
  async fn(): Promise<void> {
    stub(globalThis, "fetch", () => {
      return response;
    });

    const res = await CR.fetch(query);
    assertEquals(res, {
      "paper": Mock.parsedPaper,
      "citations": [],
      "references": [],
    });
  },
});

Deno.test({
  name: "CrossRefApi invalid references",
  async fn(): Promise<void> {
    stub(globalThis, "fetch", () => {
      return {} as Response;
    });

    const res = await CR.getChildObjects([Mock.referenceRequest]);
    assertEquals(res, [{} as IApiPaper]);
  },
});

Deno.test({
  name: "CrossRefApi valid references",
  async fn(): Promise<void> {
    const body = new Blob([JSON.stringify(Mock.referenceResponse, null, 2)], {
      type: "application/json",
    });
    const response = new Response(body, { "headers": Mock.headers });
    stub(globalThis, "fetch", () => {
      return response;
    });

    const res = await CR.getChildObjects([Mock.referenceRequest]);
    assertEquals(res, [Mock.parsedReference]);
  },
});

Deno.test({
  name: "CrossRefApi invalid citations",
  async fn(): Promise<void> {
    stub(globalThis, "fetch", () => {
      return {} as Response;
    });

    const res = await CR.getChildObjects([Mock.referenceRequest]);
    assertEquals(res, [{} as IApiPaper]);
  },
});
