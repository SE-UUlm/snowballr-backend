/**
 * Parameters which are queriable by most apis. TBI
 */

import { SourceApi } from "./iApiPaper.ts";

export interface IApiQuery {
	id: string;
	firstName?: string;
	lastName?: string;
	rawName: string;
	title: string;
	doi: string;
	year?: number;
	publisher?: string;
	type?: string;
	enabledApis?: SourceApi[];
}