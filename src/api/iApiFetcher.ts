import { IApiQuery } from "./iApiQuery.ts";
import { IApiResponse } from "./iApiResponse.ts";

/**
 * Class interface for to implement the process of fetching of a single provider api
 */
export interface IApiFetcher {
  url?: string;
  enableCache?: boolean;
  fetch(query: IApiQuery): Promise<IApiResponse>;
  getDoi(query: IApiQuery): Promise<string | undefined>;
}
