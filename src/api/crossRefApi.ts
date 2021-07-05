import {IApiQuery} from "./iApiQuery.ts";
import {IApiResponse} from "./iApiResponse.ts";
import {IApiFetcher} from "./iApiFetcher.ts";
import {IApiPaper} from './iApiPaper.ts';
import {logger} from "./logger.ts";
import {IApiAuthor} from "./iApiAuthor.ts";
import {IApiUniqueId, idType} from "./iApiUniqueId.ts";

export class CrossRefApi implements IApiFetcher {
    url: string;
    private _headers: {};

    public constructor(url: string) {
        logger.info("CrossRefApi initialized");
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
        var references: IApiPaper[];
        var rawReferences: [];
        //logger.debug(query);

        let response = fetch(`${this.url}/${query.id}`)
            .then(data => {
                return data.json();
            })
            .then(data => {
                logger.debug(data);
                paper = this._parseResponse(data);
                //logger.debug(JSON.stringify(paper, null, 2));
                //doiReference = data[0].reference;
                //console.log(data.message);
                rawReferences = data.message.reference;
                return data.message.relation ? this._getChildObjects(data.message.relation.cites) : [];
                //return this._getLinkedDOIs(data[0].citation);
            })
            .then(data => {
                citations = data;
                return this._getChildObjects(rawReferences);
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
                logger.error("Error while fetching crossRefApi: " + data);
                return {
                    "paper": paper ? paper : undefined,
                    "citations": citations ? citations : undefined,
                    "references": references ? references : undefined
                } as IApiResponse;
            })
        return response;
    }

    /**
     * Makes a http fetch call for each entry for citations and references provided by the original paper.
     * If a DOI is provided prefers to http fetch via the doi sind its way faster and provides a unique result.
     * If there's no DOI http fetch is done by querying the unstructured string provided in the entry.
     * @param rawChildren - list of citation or reference objcets return by the api
     * @returns Object list of citations or references implemented via IApiPaper
     */
    private _getChildObjects(rawChildren: any): Promise<IApiPaper[]> {
        var fetchableByDoi: string[] = [];
        var fetchableByBibliographic: string[] = [];
        let children: Array<IApiPaper> = [];
        var fetches = [];

        for (let r of rawChildren) {
            if (r.DOI) {
                fetchableByDoi.push(r.DOI);
            } else {
                //console.log(r);
                fetchableByBibliographic.push(r.unstructured);
            }
        }

        for (let d in fetchableByDoi) {
            fetches.push(fetch(`${this.url}/${fetchableByDoi[d]}`)
                .then(data => {
                    return data.json();
                })
                .then(data => {
                    let ref = this._parseResponse(data);
                    return ref;
                })
                .then(data => {
                    //console.log(data);
                    children.push(data);
                }))
        }
        for (let b in fetchableByBibliographic) {
            let query: string = fetchableByBibliographic[b].replace(/ /g, '+');
            //console.log(query);
            fetches.push(fetch(`${this.url}/?query.bibliographic=${query}&rows=1`)
                .then(data => {
                    return data.json();
                })
                .then(data => {
                    //console.log(data);
                    let ref = this._parseResponse({'message': data.message.items[0]});
                    return ref;
                })
                .then(data => {
                    //console.log(data);
                    children.push(data);
                }))
        }
        return Promise.all(fetches).then(() => children);
    }

    /**
     * Cast returns from rest api to a single IApiPaper Object
     * Tries to except missing parameters, keys and values filling them with undefined
     * @param response - api metadata for a single paper
     * @returns Object Single IApiPaper object which represents metadata of such
     */
    private _parseResponse(response: any): IApiPaper {
        //logger.debug(JSON.stringify(response, null, 2));

        let parsedAuthors: IApiAuthor[] = [];
        if (response.message.author) {
            for (let a of response.message.author) {
                let parsedAuthor: IApiAuthor = {
                    id: undefined,
                    orcid: undefined,
                    rawString: a.given && a.family ? `${a.given}, ${a.family}` : undefined,
                    lastName: a.family ? a.family : undefined,
                    firstName: a.given ? a.given : undefined,
                }
                parsedAuthors.push(parsedAuthor);
            }
        }

        let parsedUniqueIds: IApiUniqueId[] = [];
        parsedUniqueIds.push(
            {
                id: undefined,
                type: idType.ISSN,
                value: response.message.ISSN ? response.message.ISSN : undefined,
            } as IApiUniqueId
        )
        parsedUniqueIds.push(
            {
                id: undefined,
                type: idType.ISBN,
                value: response.message.ISBN ? response.message.ISBN : undefined,
            } as IApiUniqueId
        )
        parsedUniqueIds.push(
            {
                id: undefined,
                type: idType.DOI,
                value: response.message.DOI ? response.message.DOI : undefined,
            } as IApiUniqueId
        )

        let parsedResponse: IApiPaper = {
            id: undefined,
            title: response.message.title[0] ? response.message.title[0] : undefined,
            author: parsedAuthors,
            abstract: undefined,
            numberOfReferences: response.message['reference-count'] ? response.message['reference-count'] : undefined,
            numberOfCitations: response.message.relation && response.message.relation.cites ? response.message.relation.cites.length : undefined,
            year: response.Y ? response.Y : undefined,
            publisher: response.message.publisher ? response.message.publisher : undefined,
            type: response.message.type ? response.message.type : undefined,
            scope: undefined,
            scopeName: undefined,
            pdf: response.message.link ? response.message.link.map((item: any) => item.URL) : undefined,
            uniqueId: parsedUniqueIds
        };
        return parsedResponse;
    }
}