import { IApiQuery } from "../../src/api/iApiQuery.ts";
import { ApiBatcher } from "../../src/api/apiBatcher.ts";
import { SourceApi } from "../../src/api/iApiPaper.ts";
import { IComparisonWeight } from "../../src/api/iComparisonWeight.ts";
import { logResponse } from "../../src/helper/loggerHelper.ts"

import { TestDoi } from "./testDoi.ts"



const BATCHER = new ApiBatcher();

const comparisonWeight = {
	titleWeight: 15,
	titleLevenshtein: 10,
	abstractWeight: 7,
	abstractLevenshtein: 0,
	authorWeight: 8,
	overallWeight: 0.8,
	yearWeight: 2
} as IComparisonWeight;


/* TODO hash for cash out of query without enabled apis*/
for (let i = 0; i < 1; i++) {
	const query: IApiQuery = {
		id: "tst",
		rawName: undefined,
		doi: TestDoi[i],
		title: undefined,
		enabledApis: [[SourceApi.MA, "9a02225751354cd29397eba3f5382101"]], //"luca999@web.de"], [SourceApi.IE, "4yk5d9an52ejynjsmzqxe62r"], [SourceApi.MA, "9a02225751354cd29397eba3f5382101"], [SourceApi.OC], [SourceApi.S2], [SourceApi.GS]],
		aggression: comparisonWeight
	}
	console.log(`Iteration ${i}`);
	const batch = await BATCHER.startFetch(query);
	logResponse(await batch.response);
}


BATCHER.kill();

//cd Deno.exit(0)
