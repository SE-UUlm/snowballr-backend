import { logger } from "./logger.ts";
import { difference } from "https://deno.land/std@0.150.0/datetime/mod.ts";

export class FileCache {
	path: string;
	fileCaches: Map<string, string>;
	private _watchDog: number | undefined = undefined;
	ttl: number;
	checkInterval: number = 60000;

	public constructor(pathToFolder: string, ttlInSec?: number, checkInterval?: number) {
		this.path = pathToFolder;
		this.fileCaches = new Map<string, string>();
		this._initializeCache();
		ttlInSec ? this.ttl = ttlInSec : this.ttl = -1;
		if (checkInterval) { this.checkInterval = checkInterval; }
		if (this.ttl > 0) {
			this._watchTTL();
			logger.info("Watchdog for fileCache started.")
		};
	}

	private _initializeCache() {
		this._ensureDir(this.path.slice(0, this.path.lastIndexOf("/")))
		this._ensureDir(this.path);
		const files = Deno.readDirSync(this.path);
		for (const dirEntry of files) {
			if (!dirEntry.isFile) {
				continue;
			}
			try {
				const fileContent = Deno.readTextFileSync(`${this.path}/${dirEntry.name}`);
				JSON.parse(fileContent);
				this.fileCaches.set(dirEntry.name, fileContent);
			}
			catch (_) {
				logger.warning(`Couldn't load fileCache ${dirEntry.name}`);
			}
		}
	}

	private _watchTTL() {
		try {
			this._watchDog = setInterval(async () => {
				const files = Deno.readDir(this.path);
				for await (const dirEntry of files) {
					const birth = (await Deno.stat(`${this.path}/${dirEntry.name}`)).birthtime;
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
		catch (_) {
			Deno.mkdirSync(path, { mode: 0o777 });
			logger.info(`Created directory for fileCacher ${this.path}`)
		}
	}

	public async add(key: string, value: string) {
		await Deno.writeTextFile(`${this.path}/${key}`, value, { mode: 0o777 });
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
		try {
			if (this._watchDog) clearInterval(this._watchDog);
			logger.info(`Stopped watchdog for ttl`);
		}
		catch (e) {
			logger.error(`Failed to stop watchdog. Might be stopped tho. ${e}`)
		}
	}

	public empty(): boolean {
		return this.fileCaches.size > 0 ? false : true;
	}

	public async purge() {
		logger.warning(`Deleting all of fileCache. Good luck restoring.`)
		await Deno.remove(this.path, { recursive: true });
		this._ensureDir(this.path)
		this.fileCaches.clear();
	}
}