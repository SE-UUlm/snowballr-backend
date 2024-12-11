import * as RemoteCache from "./local_cache/mod.ts";
import { logger } from "./logger.ts";
import { FileCache } from "./fileCache.ts";

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
	public constructor(type: CacheType, ttl?: number, folderName?: string, checkInterval?: number) {
		this.uuid = crypto.randomUUID();
		if (type === CacheType.IM) { this.memoryCache = new RemoteCache.Cache<string, string>(ttl! * 1000); }
		else if (type === CacheType.F) {
			this.fileCache = new FileCache(`${new URL('.', import.meta.url).pathname}/../../fileCache/${folderName}`, ttl, checkInterval);
			logger.info(`FileCache created. FolderName: ${folderName}`)
		}
		else {
			throw new Error("Invalid Constructor")
		};
	}

	/**
	 * Adds cache
	 * @param key 
	 * @param value 
	 */
	public async add(key: string, value: V) {
		if (this.memoryCache) { this.memoryCache.add(String(key), JSON.stringify(value)) };
		if (this.fileCache) { await this.fileCache.add(key, JSON.stringify(value)) };
		console.log("ADDING SOME CACHE")
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
			const result = this.memoryCache.get(String(key));
			if (result) { return JSON.parse(result) }
		}
		if (this.fileCache) { const result = this.fileCache.get(key); if (result) { return JSON.parse(result) } };
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

	public getAllKeys(): string[] {
		if (this.memoryCache) {
			return this.memoryCache.keys()
		}
		if (this.fileCache) { return Array.from(this.fileCache.fileCaches.keys()) };

		return []
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