import { SourceApi } from "../api/iApiPaper.ts";
import { IApiResponse } from "../api/iApiResponse.ts";
import { CONFIG } from "./config.ts";

export const loadingCaches: Map<string, IApiResponse | undefined> = new Map<string, IApiResponse | undefined>();


export const getCache = (queryHash: string, cacheName: SourceApi): Promise<IApiResponse | undefined> => {
	//console.log("Webworker. Getting Cache from main thread.")
	//@ts-ignore
	globalThis.postMessage({ type: "getCache", queryHash: queryHash, cacheName: cacheName });
	return new Promise<IApiResponse | undefined>((resolve) => {
		const checkIfDone = () => {

			//console.log("... CHECKING loadingCaches");
			//console.log(loadingCaches);
			if (loadingCaches.has(queryHash + cacheName as string)) {
				const res = loadingCaches.get(queryHash + cacheName as string);
				loadingCaches.delete(queryHash + cacheName as string);
				//console.log("------------------------")
				//console.log(res)
				resolve(res);
			}
			else {
				setTimeout(checkIfDone, CONFIG.batcher.workerPollingRateInMilSecs);
			}
		};
		checkIfDone();
	})
}

export const addCache = (queryHash: string, cacheName: SourceApi, payload: IApiResponse): void => {
	//@ts-ignore
	globalThis.postMessage({ type: "addCache", queryHash: queryHash, cacheName: cacheName, cachePayload: JSON.stringify(payload) });
}
