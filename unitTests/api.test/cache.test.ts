import { Cache, CacheType } from "../../src/api/cache.ts";
import { IApiResponse } from "../../src/api/iApiResponse.ts";
import * as Mock from "../mockObjects/mockCache.test.ts"
import { assertEquals, assertNotEquals } from "https://deno.land/std/testing/asserts.ts"
import { sleep } from "https://deno.land/x/sleep/mod.ts";
Deno.test({
	name: "InMemoryCache: Put Cache",
	async fn(): Promise<void> {
		let cache: Cache<IApiResponse> = new Cache<IApiResponse>(CacheType.IM, 60, "test");
		await cache.add("1", Mock.apiResponse);
		let cacheValue = cache.get("1");
		cache.clear();
		//console.log(cacheValue)
		assertEquals(cacheValue, Mock.apiResponse);
	}
})

Deno.test({
	name: "InMemoryCache: Put and delete Cache",
	async fn(): Promise<void> {
		let cache: Cache<IApiResponse> = new Cache<IApiResponse>(CacheType.IM, 60, "test");
		await cache.add("1", Mock.apiResponse);
		cache.delete("1");
		let cacheValue = cache.get("1");
		cache.clear();

		assertEquals(cacheValue, undefined);
	}
})

Deno.test({
	name: "FileCache: Put Cache",
	async fn(): Promise<void> {
		let cache: Cache<IApiResponse> = new Cache<IApiResponse>(CacheType.F, 10080, "test");
		await cache.add("2", Mock.apiResponse);
		let cacheValue = cache.get("2");
		cache.clear();
		await cache.fileCache!.purge();
		assertEquals(cacheValue, Mock.apiResponse);
	}
})

Deno.test({
	name: "FileCache: Put and delete Cache",
	async fn(): Promise<void> {
		let cache: Cache<IApiResponse> = new Cache<IApiResponse>(CacheType.F, 10080, "test");
		await cache.add("1", Mock.apiResponse);
		await cache.delete("1");
		let cacheValue = cache.get("1");
		cache.clear();
		await cache.fileCache!.purge();
		assertEquals(cacheValue, undefined);
	}
})

Deno.test({
	name: "FileCache: CacheObject is reinitialized",
	async fn(): Promise<void> {
		let cache: Cache<IApiResponse> = new Cache<IApiResponse>(CacheType.F, 10080, "test");
		await cache.add("1", Mock.apiResponse);
		cache.clear();
		delete cache.fileCache;
		let cache2: Cache<IApiResponse> = new Cache<IApiResponse>(CacheType.F, 10080, "test");
		let cacheValue = cache2.get("1");
		//console.log(cacheValue)
		cache2.clear();
		await cache2.fileCache!.purge();
		assertEquals(cacheValue, Mock.apiResponse);
	}
})

Deno.test({
	name: "InMemoryCache: Check TTL",
	async fn(): Promise<void> {
		let cache: Cache<IApiResponse> = new Cache<IApiResponse>(CacheType.IM, 2, "test");
		await cache.add("1", Mock.apiResponse);
		await sleep(4);
		let cacheValue = cache.get("1");
		//console.log(cacheValue)
		cache.clear();
		assertEquals(cacheValue, undefined);
	}
})

Deno.test({
	name: "FileCache: Check TTL",
	async fn(): Promise<void> {
		let cache: Cache<IApiResponse> = new Cache<IApiResponse>(CacheType.IM, 2, "test", 1000);
		await cache.add("1", Mock.apiResponse);
		await sleep(4);
		let cacheValue = cache.get("1");
		//console.log(cacheValue)
		cache.clear();
		assertEquals(cacheValue, undefined);
	}
})
