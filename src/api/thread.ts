import { CONFIG } from "../helper/config.ts";
import { SourceApi } from "./iApiPaper.ts";
import { IApiQuery } from "./iApiQuery.ts";
import { IApiResponse } from "./iApiResponse.ts";
import { ILimitedWorkers } from "./iLimitedWorkers.ts";
import { logger } from "./logger.ts";

class ThreadManager {
	private static _instance: ThreadManager;
	public activeLimitedWorkers: ILimitedWorkers = { googleScholar: 0, crossRef: 0 };
	private _activeWorkers: WorkerThread[];
	public maxWorkers: number;

	private constructor(maxWorkers: number) {
		logger.info("Thread Manager started");
		this.maxWorkers = maxWorkers;
		this._activeWorkers = [];
	}

	public static Instance(maxWorkers: number) {
		return this._instance || (this._instance = new this(maxWorkers));
	}

	public register(worker: WorkerThread) {
		console.log("ThreadManager - Worker registered")
		this._activeWorkers.push(worker);
		let alert = false;
		//console.log(worker.query.enabledApis)
		if (worker.query.enabledApis?.find(item => item[0] === SourceApi.GS)) {
			this.activeLimitedWorkers.googleScholar += 1;
		}
		if (worker.query.enabledApis?.find(item => item[0] === SourceApi.CR)) {
			this.activeLimitedWorkers.crossRef += 1;
		}
	}

	public workerCountIncreasedNotification() {
		//console.log("here");
		this._activeWorkers.forEach(worker => worker.updateRateLimit(this.activeLimitedWorkers))
	}

	public unregister(worker: WorkerThread) {
		this._activeWorkers.slice(this._activeWorkers.indexOf(worker, 0), 1);
		if (worker.query.enabledApis?.includes([SourceApi.GS])) {
			this.activeLimitedWorkers.googleScholar -= 1;
		}
		if (worker.query.enabledApis?.includes([SourceApi.CR])) {
			this.activeLimitedWorkers.googleScholar -= 1;
		}
	}
}

export const threadManager = ThreadManager.Instance(CONFIG.batcher.maxWorkers);

export class WorkerThread {
	public currentAction: 'finished' | 'getCache' | 'addCache' | 'initialized' | 'starting' | 'updateRateLimit';
	public worker: Worker;
	public done = false;
	public initialized = false;
	private _mergedPapers: Promise<IApiResponse[]> | undefined = undefined;
	private _workerInit: Promise<void> | undefined = undefined;
	public query: IApiQuery;
	//private _data: IApiResponse[];
	public constructor(query: IApiQuery) {
		this.query = query;
		this.currentAction = 'starting';
		this.worker = new Worker(new URL("worker.ts", import.meta.url).href, { type: 'module', deno: { namespace: true, permissions: "inherit" } });
		threadManager.register(this);
	}

	public init(cache: any) {
		let data: IApiResponse[];
		this._mergedPapers = new Promise<IApiResponse[]>((resolve) => {
			const checkIfDone = () => {
				if (this.done) {
					resolve(data);
				}
				else setTimeout(checkIfDone, CONFIG.batcher.workerPollingRateInMilSecs);
			};
			checkIfDone();
		});
		this._workerInit = new Promise<void>((resolve) => {
			const checkIfDone = () => {
				if (this.initialized) {
					threadManager.workerCountIncreasedNotification();
					resolve(this.worker.postMessage({ query: JSON.stringify(this.query), type: 'run' }))
				}
				else {
					setTimeout(checkIfDone, CONFIG.batcher.workerPollingRateInMilSecs);
				}
			};
			checkIfDone();
		});

		this.worker.addEventListener('message', message => {
			//console.log("INCOMING MESSAGE " + message.data.type)
			switch (message.data.type) {
				case 'finished':
					this.currentAction = 'finished';
					data = JSON.parse(message.data.payload);
					this.done = true;
					this.close();
					break;
				case 'initialized':
					this.currentAction = 'initialized';
					this.initialized = true;
					break;
				case 'getCache': {
					this.currentAction = 'getCache';
					const cacheName: SourceApi = message.data.cacheName;
					const queryHash: string = message.data.queryHash;
					//console.log("-----QUERYHASH: " + queryHash);
					const res: IApiResponse | undefined = cache[cacheName].get(queryHash);
					//console.log("-----RES: " + res);
					this.worker.postMessage({ type: 'returningCache', cacheName: cacheName, queryHash: queryHash, cachePayload: JSON.stringify(res) });
					break;
				}
				case 'addCache': {
					this.currentAction = 'addCache'
					const cacheName = message.data.cacheName;
					const queryHash: string = message.data.queryHash;
					const cachePayload = JSON.parse(message.data.cachePayload);
					cache[cacheName].add(queryHash, cachePayload);
					break;
				}
			}
		})
	}
	public close() {
		this.worker.terminate;
		threadManager.unregister(this);
	}

	public getMergedPapers() {
		return this._mergedPapers;
	}

	public getWorkerInit() {
		return this._workerInit;
	}

	public updateRateLimit(newRateLimit: ILimitedWorkers) {
		console.log("#####################################")
		this.currentAction = 'updateRateLimit';
		this.worker.postMessage({ type: 'updateRateLimit', newRateLimit: JSON.stringify(newRateLimit) });
	}
}