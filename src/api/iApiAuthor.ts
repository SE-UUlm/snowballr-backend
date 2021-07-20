/**
 * Maps database scheme to object to be returnable by the apis.
 */
export interface IApiAuthor {
	id?: number;
	orcid: string[];
	rawString: string[];
	lastName: string[];
	firstName: string[];
}