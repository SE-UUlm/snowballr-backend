import { logger } from "./logger.ts";
import { IApiQuery } from "./iApiQuery.ts";
import { IApiResponse } from "./iApiResponse.ts";
import { difference } from "https://deno.land/std/datetime/mod.ts";

export class FileCache {
	path: string;
	fileCaches: Map<string, string>;
	private _watchDog: number | undefined = undefined;
	ttl: number | undefined;
	checkInterval: number = 60000;

	public constructor(pathToFolder: string, ttlInSec?: number, checkInterval?: number) {
		this.path = pathToFolder;
		this.fileCaches = new Map<string, string>();
		this._initializeCache();
		this.ttl = ttlInSec;
		if (checkInterval) { this.checkInterval = checkInterval; }
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
					if (birth && difference(new Date(), birth, { units: ["seconds"] }).seconds! > this.ttl!) {
						Deno.remove(`${this.path}/${dirEntry.name}`);
						this.fileCaches.delete(dirEntry.name);
						logger.info(`Deleted file ${dirEntry.name} of fileCache ${this.path} due to TTL`);
					}
				}
			}
				, this.checkInterval)
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

	public async add(key: string, value: string) {
		await Deno.writeTextFile(`${this.path}/${key}`, value);
		this.fileCaches.set(key, value);
	}

	public async delete(key: string) {
		await Deno.remove(`${this.path}/${key}`);
		this.fileCaches.delete(key);
	}

	public get(key: string): string | undefined {

		if (this.fileCaches.has(key)) {

			return this.fileCaches.get(key);
		}
		return undefined;
	}

	public has(key: string): boolean {
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
		await Deno.remove(this.path, { recursive: true });
		this.fileCaches.clear();
	}
}