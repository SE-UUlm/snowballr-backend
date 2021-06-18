import {IApiQuery} from "./iApiQuery.ts";
import {IApiResponse} from "./iApiResponse.ts";
import {IApiFetcher} from "./iApiFetcher.ts";
import {IApiPaper} from './iApiPaper.ts';
import {logger} from "./logger.ts";
import {IApiAuthor} from "./iApiAuthor.ts";
import {IApiUniqueId, idType} from "./iApiUniqueId.ts";

export class MicrosoftResearchApi implements IApiFetcher {
    url: string;
    private _authToken: string;
    private _headers: {};
    private _attributes: string;
    private _paperTypeMapper: string[];
    private _queryAttributeMapper: {};

    public constructor(url: string, authToken: string) {
        logger.info("MicrosoftResearchApi initialized");
        this.url = url;
        this._authToken = authToken;
        this._headers = {"Ocp-Apim-Subscription-Key": this._authToken, "Content-Type": "application/json"};
        this._attributes = "Id,IA,RId,CC,Y,PB,Pt,S,Ti,AA.AuN,DOI";
        this._paperTypeMapper = ["Unknown", "Journal article", "Patent", "Conference paper", "Book chapter", "Book", "Book reference entry", "Dataset", "Repository"];
        this._queryAttributeMapper = {
            "author": "AA.AuN",
            "title": "Ti",
            "id": "DOI",
            "year": "Y",
            "publisher": "PB",
            "type": "Pt"
        };
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
        var RIds: number[];
        var references: IApiPaper[];

        let response = fetch(this.url, {
            method: 'POST',
            headers: this._headers,
            body: JSON.stringify({
                expr: this._parseQuery(query),
                attributes: this._attributes
            })
        })
            .then(data => {
                return data.json();
            })
            .then(data => {
                paper = this._parseResponse(data.entities[0]);
                RIds = data.entities[0].RId;
                return this._getCitations(data.entities[0].Id);
            })
            .then(data => {
                citations = data
                return this._getReferences(RIds);
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
                logger.error("Error while fetching microsoftResearch: " + data);
                return {
                    "paper": paper ? paper : undefined,
                    "citations": citations ? citations : undefined,
                    "references": references ? references : undefined
                } as IApiResponse;
            })
        return response;
    }

    /**
     * Abstract is provided via dictionary. This function casts an IA-object to a single string.
     *
     * @param iA - invertedAbstract object provided by microsoft api
     * @returns abstract of a paper in a single string.
     */
    private static _convertInvertedAbstract(iA: any): string {
        let abstract: string = "";
        try {
            for (let key in iA.InvertedIndex) {
            abstract = `${abstract} ${key}`;
            }
            return abstract.trim();
        } catch (error) {
            logger.error("Couldn't convert InvertedAbstract from Microsoft API to a single string.");
            return "";
        }
    }

    /**
     * Parse all microsoft-id references from a single paper and query for all the ids to return a paperObject for each.
     *
     * @param microsoftIds - list of microsoft-ids provided by the source-paper in key RId
     * @returns list of paperObjects containing the references. Promise.
     */
    private _getReferences(microsoftIds: number[]): Promise<IApiPaper[]> {
        let convertedIds: string[] = microsoftIds.map(String);
        convertedIds = convertedIds.map(i => 'Id=' + i);
        let queryPattern = convertedIds.join(',');

        let response = fetch(this.url, {
            method: 'POST',
            headers: this._headers,
            body: JSON.stringify({
                expr: `Or(${queryPattern})`,
                attributes: this._attributes
            })
        })
            .then(data => {
                return data.json();
            })
            .then(data => {
                let citations: Array<IApiPaper> = [];
                for (let value in data.entities) {
                    let cit = this._parseResponse(data.entities[value]);
                    citations.push(cit);
                }
                return citations;
            })
        return response;
    }

    /**
     * Query for all papers containing the original-papers microsoft id in their references
     *
     * @param microsoftId - original-paper microsoft id. Returned by another fetch call.
     * @returns list of paperObjects containing the citations. Promise.
     */
    private _getCitations(microsoftId: string): Promise<IApiPaper[]> {
        let response = fetch(this.url, {
            method: 'POST',
            headers: this._headers,
            body: JSON.stringify({
                expr: `RId=${microsoftId}`,
                attributes: this._attributes
            })
        })
            .then(data => {
                return data.json();
            })
            .then(data => {
                var citations: Array<IApiPaper> = [];
                for (let value in data.entities) {
                    let cit = this._parseResponse(data.entities[value]);
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
     * Parse a queryObject to a string readable by the microsoftApi
     *
     * @param query - queryObject for the originalPaper.
     * @returns string appendable to the api call via body-key "expr".
     */
    private _parseQuery(query: IApiQuery): string {
        let baseExpr: string = `Or(DOI='${query.id}', And(Composite(AA.AuN='${query.rawName.toLowerCase()}'), Ti='${query.title.toLowerCase()}'))`

        return baseExpr;
    }

    /**
     * Cast the response of a single paper return by the microsoftApi to a ApiPaper object
     * Used to get normalized result of all apis.
     *
     * @param response - single object return for a single paper by the microsoftApi
     * @returns normalized ApiPaper object.
     */
    private _parseResponse(response: any): IApiPaper {
        logger.debug(response);
        let refLength: number = (response.RId === undefined ? 0 : response.RId.length);

        let parsedAuthors: IApiAuthor[] = [];
        for(let a of response.AA) {
            let parsedAuthor: IApiAuthor = {
                id: undefined,
                orcid: undefined,
                rawString: a.AuN,
                lastName: undefined,
                firstName: undefined,
            }
            parsedAuthors.push(parsedAuthor);
        }

        let parsedUniqueIds: IApiUniqueId[] = [];
        parsedUniqueIds.push(
            {
                id: undefined,
                type: idType.DOI,
                value: response.DOI ? response.DOI : undefined,
            } as IApiUniqueId
        )
        parsedUniqueIds.push(
            {
                id: undefined,
                type: idType.MicrosoftAcademic,
                value: response.Id ? response.Id : undefined,
            } as IApiUniqueId
        )

        let parsedResponse: IApiPaper = {
            id: undefined,
            title: response.Ti ? response.Ti : undefined,
            author: parsedAuthors,
            abstract: MicrosoftResearchApi._convertInvertedAbstract(response.IA),
            numberOfReferences: refLength,
            numberOfCitations: response.CC ? response.CC : undefined,
            year: response.Y ? response.Y : undefined,
            publisher: response.PB ? response.PB : undefined,
            type: response.Pt ? this._paperTypeMapper[response.Pt] : undefined,
            scope: "",
            scopeName: "",
            pdf: response.S ? response.S : undefined,
            uniqueId: parsedUniqueIds
        };
        return parsedResponse;
    }
}