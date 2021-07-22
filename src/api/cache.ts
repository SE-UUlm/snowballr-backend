import * as RemoteCache from "https://deno.land/x/local_cache/mod.ts";
import { IApiQuery } from "./iApiQuery.ts";
import { IApiResponse } from "./iApiResponse.ts";



export class Cache {
	memoryCache: RemoteCache.Cache<string, string> | undefined;
	uuid: string = "";

	public constructor(memoryCacheEnabled: boolean, fileCacheEnabled: boolean, ttlOfMemCashInMiliSecs: number) {

		if (!memoryCacheEnabled && !fileCacheEnabled) {
			throw new Error("Cache implementation needs eiter memoryCache or fileCache enabled. Neither is. Control via constructor params.")
		}
		if (memoryCacheEnabled) { this.memoryCache = new RemoteCache.Cache<string, string>(ttlOfMemCashInMiliSecs); }
		this.uuid = crypto.randomUUID();

	}

	public add(query: IApiQuery, response: IApiResponse) {
		if (this.memoryCache) { this.memoryCache.add(JSON.stringify(query), JSON.stringify(response)) }

	}

	public delete(query: IApiQuery) {
		if (this.memoryCache) { this.memoryCache.delete(JSON.stringify(query)) }
	}

	public get(query: IApiQuery): IApiResponse | undefined {
		if (this.memoryCache) {
			let result = this.memoryCache.get(JSON.stringify(query))
			if (result) { return JSON.parse(result) }
		}
	}

	public clear() {
		console.log("killing cache" + this.uuid)

		// let entries = this.memoryCache?.entries();
		// for (let i in entries) {
		// 	delete entries[<any>i];
		// }
		if (this.memoryCache) { this.memoryCache.clear(); clearTimeout() }
	}

	public empty() {
		if (this.memoryCache) { return this.memoryCache.empty() }
	}
}