import {IApiQuery} from "./iApiQuery.ts";
import {IApiResponse} from "./iApiResponse.ts";
import {IApiFetcher} from "./iApiFetcher.ts";
import {IApiPaper} from './iApiPaper.ts';
import {logger} from "./logger.ts";
import {IApiAuthor} from "./iApiAuthor.ts";
import {IApiUniqueId, idType} from "./iApiUniqueId.ts";

export class OpenCitationsApi implements IApiFetcher {
    url: string;
    private _headers: {};

    public constructor(url: string) {
        logger.info("OpenCitationsApi initialized");
        this.url = url;
        this._headers = {"Content-Type": "application/json"};
    }

    /**
     * Checks for a paper at the microsoft research api via a query object.
     * Returns the papers gathered info and all papers citated or referenced.
     *
     * @param query - Object defined by interface in IApiQuery to filter and query api calls.
     * @returns Object containing the fetched paper and all paperObjects from citations and references. Promise.
     */
    public async fetch(query: IApiQuery): Promise<IApiResponse> {
        var citations: IApiPaper[];
        var paper: IApiPaper;
        var doiReference: string;
        var references: IApiPaper[];
        //logger.debug(query);

        let response = fetch(`${this.url}/index/api/v1/metadata/${query.id}`)
            .then(data => {
                return data.json();
            })
            .then(data => {
                paper = this._parseResponse(data[0]);
                //logger.debug(data);
                doiReference = data[0].reference;
                return this._getLinkedDOIs(data[0].citation);
            })
            .then(data => {
                citations = data
                return this._getLinkedDOIs(doiReference);
            })
            .then(data => {
                references = data;
                let apiReturn: IApiResponse = {
                    "paper": paper,
                    "citations": citations,
                    "references": references
                }
                return apiReturn;
            })
            .catch(data => {
                logger.error("Error while fetching openCitations: " + data);
                return {
                    "paper": paper ? paper : undefined,
                    "citations": citations ? citations : undefined,
                    "references": references ? references : undefined
                } as IApiResponse;
            })
        return response;
    }

    /**
     * Make a single ORed http call to fetch all entries for a list of DOIs.
     * Used to get citations and references.
     * ORed http calls with multiple DOIs can be done by seperating dois with "__"
     * @param dois - string of DOIs return by the original api called. Each doi separated by ";"
     * @returns Object List of IApiPaper with all metadata for the references or citations. Promise.
     */
    private _getLinkedDOIs(dois: string): Promise<IApiPaper[]> {
        let urlQuery: string = dois.replace(/; /g, '__');
        //logger.debug(urlQuery)
        let response = fetch(`${this.url}/index/api/v1/metadata/${urlQuery}`)
            .then(data => {
                return data.json();
            })
            .then(data => {
                //console.log(data);
                var citations: Array<IApiPaper> = [];
                for (let value in data) {
                    let cit = this._parseResponse(data[value]);
                    citations.push(cit);
                }
                return citations;
            })
            .then(
                (result) => {
                    return result; // This returns undefined
                },
                (error) => {
                    return error;
                }
            );
        return response;
    }

    /**
     * Cast returns from rest api to a single IApiPaper Object
     * Tries to except missing parameters, keys and values filling them with undefined
     * @param rescccponse - api metadata for a single paper
     * @returns Object Single IApiPaper object which represents metadata of such
     */
    private _parseResponse(response: any): IApiPaper {
        var refCount: any;

        //logger.debug(response);
        try {
            if (response.reference && !(response.reference in ["", undefined])) {
                refCount = response.reference.split(";").length;
            } else if (response.doi_reference && !(response.doi_reference in ["", undefined])) {
                refCount = response.doi_reference.split[";"].length;
            } else {
                refCount = 0;
            }
        } catch (e) {
            refCount = 0;
        }

        let parsedAuthors: IApiAuthor[] = [];
        for (let a of response.author.split(';')) {
            let parsedAuthor: IApiAuthor = {
                id: undefined,
                orcid: a.split(',').length > 2 ? a.split(',')[2].trim() : undefined,
                rawString: a.split(',').length > 1 ? `${a.split(',')[0]},${a.split(',')[1]}`.trim() : a.split(',')[0].trim(),
                lastName: a.split(',').length > 1 ? a.split(',')[0].trim() : undefined,
                firstName: a.split(',').length > 1 ? a.split(',')[1].trim() : a.split(',')[0].trim(),
            }
            parsedAuthors.push(parsedAuthor);
        }

        let parsedUniqueIds: IApiUniqueId[] = [];
        parsedUniqueIds.push(
            {
                id: undefined,
                type: idType.DOI,
                value: response.doi
            } as IApiUniqueId
        )

        let parsedResponse: IApiPaper = {
            id: undefined,
            title: response.title ? response.title : undefined,
            sourceTitle: response.source_title ? response.source_title : undefined,
            author: parsedAuthors,
            abstract: undefined,
            numberOfReferences: refCount,
            numberOfCitations: response.citation_count ? parseInt(response.citation_count) : undefined,
            year: response.year ? response.year : undefined,
            publisher: undefined,
            type: undefined,
            scope: undefined,
            scopeName: undefined,
            pdf: response.oa_link ? response.oa_link.split(",") : undefined,
            uniqueId: parsedUniqueIds
        };
        return parsedResponse;
    }
}