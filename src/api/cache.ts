import * as RemoteCache from "./local_cache/mod.ts";
import { logger, fileLogger } from "./logger.ts";
import { difference } from "https://deno.land/std/datetime/mod.ts";
import { FileCache } from "./fileCache.ts";
import { idType } from "./iApiUniqueId.ts";


export enum CacheType {
	IM = "InMemoryCache",
	F = "FileCache"
}

/**
 * Allows to cache IApiResponse objects previously fetched to minimize time to wait and dont overload the apis.
 * @template V 
 */
export class Cache<V> {
	memoryCache: RemoteCache.Cache<string, string> | undefined;
	fileCache: FileCache | undefined;
	uuid: string = "";

	/**
	 * Creates an instance of cache.
	 * @param memoryCacheEnabled 
	 * @param fileCacheEnabled 
	 * @param [ttlOfMemCashInSec] 
	 * @param [ttlOfFileCacheInSec] 
	 * @param [folderName] 
	 */
	public constructor(type: CacheType, ttl: number, folderName?: string, checkInterval?: number) {
		this.uuid = crypto.randomUUID();
		// if (!memoryCacheEnabled && !fileCacheEnabled) {
		// 	throw new Error("Cache implementation needs either memoryCache or fileCache enabled. Neither is. Control via constructor params.")
		// }
		if (type === CacheType.IM) { this.memoryCache = new RemoteCache.Cache<string, string>(ttl! * 1000); }
		else if (type === CacheType.F) {
			//`${new URL('.', import.meta.url).pathname}/a.log`
			this.fileCache = new FileCache(`${new URL('.', import.meta.url).pathname}/../../fileCache/${folderName}`, ttl, checkInterval);
			logger.info(`FileCache created. FolderName: ${folderName}`)
		}
		else { throw new Error("Invalid Constructor") };
	}

	/**
	 * Adds cache
	 * @param key 
	 * @param value 
	 */
	public async add(key: string, value: V) {
		if (this.memoryCache) { this.memoryCache.add(String(key), JSON.stringify(value)) };
		if (this.fileCache) { await this.fileCache.add(key, JSON.stringify(value)) };

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
	public get(key: string): V | undefined {
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