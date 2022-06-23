/**
 * Parameters which are queriable by most apis. TBI
 */

import { SourceApi } from "./iApiPaper.ts";
import { IComparisonWeight } from "./iComparisonWeight.ts";

export interface IApiQuery {
	id?: string;
	rawName?: string;
	title?: string;
	doi?: string;
	year?: number;
	publisher?: string;
	type?: string;
	enabledApis?: [SourceApi,string?][];
	aggression: IComparisonWeight;
	projectName?: string
}