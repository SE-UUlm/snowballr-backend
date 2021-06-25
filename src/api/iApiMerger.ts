import {IApiResponse} from "./iApiResponse.ts";
import {IComparisonWeight} from "./iComparisonWeight.ts";

/**
 * merge function unites n objects identified as same into one reponse object
 * compare function compares n objects returned by the apis and flags them as different or equal
 */

export interface IApiMerger {
    comparisonWeight: IComparisonWeight;
    merge(firstResponse: IApiResponse, secondResponse: IApiResponse): IApiResponse;
    compare(response: Promise<IApiResponse>[]) : Promise<IApiResponse[]>;
}