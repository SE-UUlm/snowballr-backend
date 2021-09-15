import { IApiUniqueId } from "./iApiUniqueId.ts";
import { IApiAuthor } from "./iApiAuthor.ts";

/**
 * Maps database scheme to object to be returnable by the apis.
 * If a value is not parsable it should be set to type undefined (default value for optionals)
 */
export interface IApiPaper {
	id?: number;
	title: string[];
	author: IApiAuthor[];
	abstract: string[];
	numberOfReferences: number[];
	numberOfCitations: number[];
	year: number[];
	publisher: string[];
	type: string[];
	scope: string[];
	scopeName: string[];
	pdf: string[];
	uniqueId: IApiUniqueId[];
	source: SourceApi[];
	raw: string[];

	titleSource?: string[];
	abstractSource?: string[];
	yearSource?: number[];
	publisherSource?: string[];
	typeSource?: string[];
	scopeSource?: string[];
	scopeNameSource?: string[];
}

export enum SourceApi {
	MA = "microsoftAcademic",
	CR = "crossRef",
	OC = "openCitations",
	S2 = "semanticScholar",
	IE = "IEEE",
	GS = "googleScholar"
}