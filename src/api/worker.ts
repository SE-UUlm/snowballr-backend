import { ApiMerger } from "./apiMerger.ts";
import { IApiResponse } from "./iApiResponse.ts";

//@ts-ignore: linter detects error although its none
self.onmessage = async (e) => {

	const parsedData = JSON.parse(e.data);
	// let query: IApiQuery = parsedData.query;
	const response: Promise<IApiResponse>[] = parsedData.response;

	const finished = merger.compare(response)

	//@ts-ignore
	self.postMessage({ payload: JSON.stringify(await finished), type: "finished" })
	self.close();
}

const merger: ApiMerger = new ApiMerger({ "titleWeight": 15, "titleLevenshtein": 10, "abstractWeight": 7, "abstractLevenshtein": 0, "authorWeight": 8, "overallWeight": 0.8, "yearWeight": 2 });
//@ts-ignore
self.postMessage({ type: "initialized" })
