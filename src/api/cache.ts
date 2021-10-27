import * as RemoteCache from "./local_cache/mod.ts";
import { logger, fileLogger } from "./logger.ts";
import { difference } from "https://deno.land/std/datetime/mod.ts";
import { FileCache } from "./fileCache.ts";
import { idType } from "./iApiUniqueId.ts";
import { IApiResponse } from "./iApiResponse.ts";
import { CONFIG } from "../helper/config.ts";


export enum CacheType {
	IM = "InMemoryCache",
	F = "FileCache"
}

/**
 * Allows to cache IApiResponse objects previously fetched to minimize time to wait and dont overload the apis.
 * @template V 
 */
export class Cache {
	memoryCache: RemoteCache.Cache<string, string> | undefined;
	fileCache: FileCache | undefined;
	uuid: string = "";
	private static instance: Cache;

	/**
	 * Creates an instance of cache.
	 * @param memoryCacheEnabled 
	 * @param fileCacheEnabled 
	 * @param [ttlOfMemCashInSec] 
	 * @param [ttlOfFileCacheInSec] 
	 * @param [folderName] 
	 */
	private constructor() {
		this.uuid = crypto.randomUUID();
		if (CONFIG.cache.type === CacheType.IM) { this.memoryCache = new RemoteCache.Cache<string, string>(10800! * 1000); }
		else if (CONFIG.cache.type === CacheType.F) {
			this.fileCache = new FileCache(`${new URL('.', import.meta.url).pathname}/../../fileCache`, 10800, 500);
			logger.info(`FileCache created.`)
		}
		else {
			throw new Error("Invalid Constructor")
		};
	}

	public static getInstance() {
		if (!Cache.instance) {
			Cache.instance = new Cache();
		}
		return Cache.instance;
	}

	/**
	 * Adds cache
	 * @param key 
	 * @param value 
	 */
	public async add(key: string, value: IApiResponse) {
		if (this.memoryCache) { this.memoryCache.add(String(key), JSON.stringify(value)) };
		if (this.fileCache) { await this.fileCache.add(key, JSON.stringify(value)) };
		console.log("ADDING SOME CACHE");
	}

	/**
	 * Deletes cache
	 * @param key 
	 */
	public async delete(key: string) {
		if (this.memoryCache) { this.memoryCache.delete(String(key)) };
		if (this.fileCache) { await this.fileCache.delete(key) };
	}

	/**
	 * Gets cache
	 * @param key 
	 * @returns get 
	 */
	public get(key: string): IApiResponse | undefined {
		if (this.memoryCache) {
			let result = this.memoryCache.get(String(key));
			if (result) { return JSON.parse(result) }
		}
		if (this.fileCache) { let result = this.fileCache.get(key); if (result) { return JSON.parse(result) } };
	}

	/**
	 * Has cache
	 * @param key 
	 * @returns true if has 
	 */
	public has(key: string): boolean {
		if (this.memoryCache) {
			return this.memoryCache.has(String(key))
		}
		if (this.fileCache) { return this.fileCache.has(key) };
		return false;
	}

	/**
	 * Clears cache
	 */
	public clear() {
		if (this.memoryCache) { this.memoryCache.clear() };
		if (this.fileCache) { this.fileCache.clear() };
	}

	/**
	 * Emptys cache
	 * @returns  
	 */
	public empty() {
		if (this.memoryCache) { return this.memoryCache.empty() };
		if (this.fileCache) { return this.fileCache.empty() };
	}

}