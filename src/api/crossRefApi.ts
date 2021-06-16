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