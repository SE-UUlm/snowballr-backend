import { IApiResponse } from "./iApiResponse.ts";
import { IComparisonWeight } from "./iComparisonWeight.ts";
import { IApiPaper } from "./iApiPaper.ts";

/**
 * merge function unites n objects identified as same into one reponse object
 * compare function compares n objects returned by the apis and flags them as different or equal
 */

export interface IApiMerger {
    comparisonWeight: IComparisonWeight;

    merge(firstResponse: IApiPaper, secondResponse: IApiPaper): Promise<IApiPaper>;

    compare(response: Promise<IApiResponse>[]): Promise<IApiResponse[]>;
}