import {IApiQuery} from "./iApiQuery.ts";
import {IApiResponse} from "./iApiResponse.ts";

export interface IApiFetcher {
    url?: string;
    fetch(query: IApiQuery): Promise<IApiResponse>;
}