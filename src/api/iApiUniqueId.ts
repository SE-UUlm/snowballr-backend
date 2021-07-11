/**
 * Used api types defined in https://gitlab.uni-ulm.de/groups/se/snowballr/-/wikis/LiteratureAPIs
 */
export enum idType {
	DOI = "DOI",
	ISSN = "ISSN",
	ISBN = "ISBN",
	MicrosoftAcademic = "MicrosoftAcademic",
	OpenCitationsIndex = "OpenCitationsIndex",
	GoogleScholar = "GoogleScholar",
	IEEE = "IEEE",
	CrossRef = "CrossRef",
	SemanticScholar = "SemanticScholar"
}

/**
 * Maps database scheme to object to be returnable by the apis.
 */
export interface IApiUniqueId {
	id?: number;
	type?: idType;
	value?: string;
}