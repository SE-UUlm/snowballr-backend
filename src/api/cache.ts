import * as RemoteCache from "./local_cache/mod.ts";
import { IApiQuery } from "./iApiQuery.ts";
import { IApiResponse } from "./iApiResponse.ts";
import { logger, fileLogger } from "./logger.ts";
import { difference } from "https://deno.land/std/datetime/mod.ts";


export class Cache<V> {
	memoryCache: RemoteCache.Cache<string, string> | undefined;
	fileCache: FileCache | undefined;
	uuid: string = "";

	public constructor(memoryCacheEnabled: boolean, fileCacheEnabled: boolean, ttlOfMemCashInMin?: number, ttlOfFileCacheInMin?: number, folderName?: string) {
		this.uuid = crypto.randomUUID();
		if (!memoryCacheEnabled && !fileCacheEnabled) {
			throw new Error("Cache implementation needs eiter memoryCache or fileCache enabled. Neither is. Control via constructor params.")
		}
		if (memoryCacheEnabled) { this.memoryCache = new RemoteCache.Cache<string, string>(ttlOfMemCashInMin! * 60000); }
		if (fileCacheEnabled) {
			//`${new URL('.', import.meta.url).pathname}/a.log`
			this.fileCache = new FileCache(`${new URL('.', import.meta.url).pathname}/../../fileCache/${folderName}`, ttlOfFileCacheInMin);
			logger.info(`FileCache created. FolderName: ${folderName}`)
		}
	}

	public add(key: string | number, value: V) {
		if (this.memoryCache) { this.memoryCache.add(String(key), JSON.stringify(value)) };
		if (this.fileCache) { this.fileCache.add(key, JSON.stringify(value)) };

	}

	public delete(key: string | number) {
		if (this.memoryCache) { this.memoryCache.delete(String(key)) };
		if (this.fileCache) { this.fileCache.delete(key) };
	}

	public get(key: string | number): V | undefined {
		if (this.memoryCache) {
			let result = this.memoryCache.get(String(key));
			if (result) { return JSON.parse(result) }
		}
		if (this.fileCache) { this.fileCache.get(key) };
	}

	public has(key: string | number): boolean {
		if (this.memoryCache) {
			return this.memoryCache.has(String(key))
		}
		if (this.fileCache) { return this.fileCache.has(key) };
		return false;
	}

	public clear() {
		if (this.memoryCache) { this.memoryCache.clear() };
		if (this.fileCache) { this.fileCache.clear() };
	}

	public empty() {
		if (this.memoryCache) { return this.memoryCache.empty() };
		if (this.fileCache) { return this.fileCache.empty() };
	}



}

type FileCacheIndex = string | number

export class FileCache {
	path: string;
	fileCaches: Map<FileCacheIndex, string>;
	private _watchDog: number | undefined = undefined;
	ttl: number | undefined;

	public constructor(pathToFolder: string, ttlInMinutes?: number) {
		this.path = pathToFolder;
		this.fileCaches = new Map<string, string>();
		this._initializeCache();
		this.ttl = ttlInMinutes;
		if (this.ttl) {
			this._watchTTL();
			logger.info("Watchdog for fileCache started.")
		};
	}

	private _initializeCache() {
		this._ensureDir(this.path.slice(0, this.path.lastIndexOf("/")))
		this._ensureDir(this.path);
		let files = Deno.readDirSync(this.path);
		for (const dirEntry of files) {
			if (!dirEntry.isFile) {
				continue;
			}
			try {
				let fileContent = Deno.readTextFileSync(`${this.path}/${dirEntry.name}`);
				JSON.parse(fileContent);
				this.fileCaches.set(dirEntry.name, fileContent);
			}
			catch (e) {
				logger.warning(`Couldn't load fileCache ${dirEntry.name}`);
			}
		}
	}

	private _watchTTL() {
		try {
			this._watchDog = setInterval(async () => {
				let files = Deno.readDir(this.path);
				for await (const dirEntry of files) {
					let birth = (await Deno.stat(`${this.path}/${dirEntry.name}`)).birthtime;
					//logger.debug(`${new Date()} - ${birth!} = ${difference(new Date(), birth!, { units: ["minutes"] })}`)
					if (birth && difference(new Date(), birth, { units: ["minutes"] }).minutes! > this.ttl!) {
						Deno.remove(`${this.path}/${dirEntry.name}`);
						this.fileCaches.delete(dirEntry.name);
						logger.info(`Deleted file ${dirEntry.name} of fileCache ${this.path} due to TTL`);
					}
				}
			}
				, 60000)
		} catch (e) {
			logger.error(e)
		}
	}

	private _ensureDir(path: string) {
		try {
			Deno.readDirSync(path);
			//logger.info(`Directory already existing for fileCache ${this.path}`)
		}
		catch (e) {
			Deno.mkdirSync(path);
			logger.info(`Created directory for fileCacher ${this.path}`)
		}
	}

	public async add(key: FileCacheIndex, value: string) {
		await Deno.writeTextFile(`${this.path}/${key}`, value);
		this.fileCaches.set(key, value);
	}

	public async delete(key: FileCacheIndex) {
		await Deno.remove(`${this.path}/${key}`);
		this.fileCaches.delete(key);
	}

	public get(key: FileCacheIndex): string | undefined {

		if (this.fileCaches.has(key)) {

			return this.fileCaches.get(key);
		}
		return undefined;
	}

	public has(key: FileCacheIndex): boolean {
		if (this.fileCaches.has(key)) {
			return true;
		}
		return false;
	}

	public clear() {
		clearInterval(this._watchDog);
		logger.info(`Stopped watchdog for ttl`);
	}

	public empty(): boolean {
		return this.fileCaches.size > 0 ? false : true;
	}

	public async purge() {
		logger.warning(`Deleting all of fileCache. Good luck restoring.`)
		await Deno.remove(this.path);
		this.fileCaches.clear();
	}
}