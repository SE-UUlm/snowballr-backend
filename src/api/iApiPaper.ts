import {IApiUniqueId} from "./iApiUniqueId.ts";
import {IApiAuthor} from "./iApiAuthor.ts";

/**
 * Maps database scheme to object to be returnable by the apis.
 * If a value is not parsable it should be set to type undefined (default value for optionals)
 */
export interface IApiPaper {
    id?: number;
    title?: string;
    sourceTitle?: string;
    author?: IApiAuthor[];
    abstract?: string;
    numberOfReferences?: number;
    numberOfCitations?: number;
    year?: number;
    publisher?: string;
    type?: string;
    scope?: string;
    scopeName?: string;
    pdf?: string[];
    uniqueId?: IApiUniqueId[];
}
