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
                let apiReturn: IApiResponse = {
                    "paper": paper,
                    "citations": citations,
                    "references": data
                }
                return apiReturn;
            })
        return response;
    }

    private _getLinkedDOIs(dois: string): Promise<IApiPaper[]> {
        let urlQuery: string = dois.replace(/; /g, '__');
        //logger.debug(urlQuery)
        let response = fetch(`${this.url}/api/v1/metadata/${urlQuery}`)
            .then(data => {
                return data.json();
            })
            .then(data => {
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

    private _parseResponse(response: any): IApiPaper {
        var refCount: any;

        logger.debug(response);
        try {
            if (response.reference && !(response.reference in ["", undefined])) {
                refCount = response.reference.split(";").length;
            } else if (response.doi_reference && !(response.doi_reference in ["", undefined])) {
                refCount = response.doi_reference.split[";"].length;
            } else {
                refCount = 0;
            }
        }
        catch (e) {
            refCount = 0;
        }

        let parsedAuthors: IApiAuthor[] = [];
        for(let a of response.author.split(';')) {
            let parsedAuthor: IApiAuthor = {
                id: undefined,
                orcid: a.split(',').length > 2 ? a.split(',')[2] : undefined,
                rawString: a,
                lastName: a.split(',').length > 1 ? a.split(',')[0] : undefined,
                firstName: a.split(',').length > 1 ? a.split(',')[1] : a.split(',')[0],
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
            pdf: response.oa_link ? response.oa_link : undefined,
            uniqueId: parsedUniqueIds
        };
        return parsedResponse;
    }
}