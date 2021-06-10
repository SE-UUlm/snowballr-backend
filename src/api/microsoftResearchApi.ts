import {IApiQuery} from "./iApiQuery.ts";
import {IApiResponse} from "./iApiResponse.ts";
import {IApiFetcher} from "./iApiFetcher.ts";
import {IApiPaper} from './iApiPaper.ts'

export class MicrosoftResearchApi implements IApiFetcher {
    url: string;
    private _authToken: string;
    private _headers: {};
    private _attributes: string;
    private _paperTypeMapper: string[];
    private _queryAttributeMapper: {};

    public constructor(url: string, authToken: string) {
        this.url = url;
        this._authToken = authToken;
        this._headers = {"Ocp-Apim-Subscription-Key": this._authToken, "Content-Type": "application/json"};
        this._attributes = "Id,IA,RId,CC,Y,PB,Pt,S,Ti,AA.AuN,DOI";
        this._paperTypeMapper = [ "Unknown", "Journal article", "Patent", "Conference paper", "Book chapter", "Book", "Book reference entry", "Dataset", "Repository" ];
        this._queryAttributeMapper = { "author": "AA.AuN", "title": "Ti", "id": "DOI", "year": "Y", "publisher": "PB", "type": "Pt" };
    }

    public async fetch(query: IApiQuery): Promise<IApiResponse> {
        var citations: IApiPaper[];
        var paper: IApiPaper;
        var RIds: number[];

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
            return this._getCitations(data.entities[0].Id);;
        })
        .then(data => {
            citations = data
            return this._getReferences(RIds);
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
            //console.log("test");
            //console.log(data);
            var citations: Array<IApiPaper> = [];
            for (let value in data.entities) {
                let cit = this._parseResponse(data.entities[value]);
                //console.log(data.entities[value]);
                citations.push(cit);
            }
            return citations;
        })
        return response;
    }

    private _getCitations(microsoftId: string): Promise<IApiPaper[]> {
        //var citations: Array<IApiPaper> = [];
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
            //console.log("test");
            //console.log(data);
            var citations: Array<IApiPaper> = [];
            for (let value in data.entities) {
                let cit = this._parseResponse(data.entities[value]);
                //console.log(data.entities[value]);
                citations.push(cit);
            }
            return citations;
        })
        .then(
            (result) => {
                //console.log(result); // This returns an object
                return result; // This returns undefined
            },
            (error) => {
                return error;
            }
        );
        return response;
    }

    private _parseQuery(query: IApiQuery): string {
        let expr: string = "";
        let baseExpr: string = `Or(DOI='${query.id}', And(Composite(AA.AuN='${query.rawName.toLowerCase()}'), Ti='${query.title.toLowerCase()}'))`

        return baseExpr;
    }

    private _parseResponse(response: any): IApiPaper  {



        //console.log("____________________________" + response);

        let refLength: number = ( response.RId != undefined ? response.RId.length : 0);

        let parsedResponse: IApiPaper = {
            title: response.Ti,
            abstract: MicrosoftResearchApi._convertInvertedAbstract(response.IA),
            numberOfReferences: refLength,
            numberOfCitations: response.CC,
            year: response.Y,
            publisher: response.PB,
            type: this._paperTypeMapper[response.Pt],
            scope: "",
            scopeName: "",
            pdf: response.S,
            id: response.DOI
        };
        //console.log(promiseObject);
        return parsedResponse;
    }

    private static _convertInvertedAbstract(iA: any): string {
        let abstract: string = "";
        try {
            for (let key in iA.InvertedIndex) {
                abstract =`${abstract} ${key}`;
            }
            return abstract.trim();
        }
        catch (error) {
            return "";
        }
    }
}