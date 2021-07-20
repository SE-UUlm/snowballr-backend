import { IApiQuery } from "./iApiQuery.ts";
import { IApiResponse } from "./iApiResponse.ts";

export interface IApiBatcher {
	activeBatches: IApiBatch[],
	startFetch(query: IApiQuery): IApiBatch,
	subscribeActiveFetch(query: IApiQuery): IApiBatch | undefined,
	stopFetch(batch: IApiBatch): boolean,
}

export interface IApiBatch {
	subscribers: IApiQuery[],
	status: BatcherStatus,
	id: string | undefined,
	response: Promise<IApiResponse[]>,
	addSubscriber(subscriber: IApiQuery): void,
}

export enum BatcherStatus {
	R = "running",
	F = "finished",
	D = "defined"
}
