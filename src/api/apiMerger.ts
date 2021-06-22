import {IApiMerger} from "./iApiMerger.ts";
import {logger} from "./logger.ts";
import {IApiResponse} from "./iApiResponse.ts";
import {idType} from "./iApiUniqueId.ts";
import {IApiPaper} from "./iApiPaper.ts";
import {Levenshtein} from "./levenshtein.ts";
import {IComparisonWeight} from "./iComparisonWeight.ts";


export class ApiMerger implements IApiMerger {
    public comparisonWeight = {
        titleWeight: 10,
        titleLevenshtein: 10,
        abstractWeight: 7,
        abstractLevenshtein: 0,
        authorWeight: 8,
        overallWeight: 0.7
    } as IComparisonWeight;

    public constructor(comparisonWeight?: IComparisonWeight) {
        this.comparisonWeight = comparisonWeight ? comparisonWeight : this.comparisonWeight;
        logger.info("Merger initialized");
    }

    public async compare(firstResponse: Promise<IApiResponse>[], secondResponse: Promise<IApiResponse>[]): Promise<IApiResponse[]> {
        //TODO: async for fastness
        //logger.debug(`\n*************************************`);
        let finished: IApiResponse[] = [];
        for (let i: number = 0; i < firstResponse.length - 1; i++) {
            logger.debug(`\n*************************************`);
            let element = await this.comparePaperWithPapers(firstResponse[i], secondResponse)
            if(element){
                finished.push(element);
            } else{
                finished.push(await firstResponse[i])
            }
        }

        for (let i: number = 0; i < secondResponse.length; i++) {
            if(secondResponse[i]){
                finished.push(await secondResponse[i])
            }
        }
        return finished;
    }

    public async comparePaperWithPapers(paper: Promise<IApiResponse>,others: Promise<IApiResponse>[]): Promise<IApiResponse | undefined> {
        for (let j: number = 0; j < others.length; j++) {
            let isEqual: boolean = this._isEqual(await paper, await others[j]);
            if (isEqual) {
                logger.debug("Others before: " + others[j]);
                logger.debug("Objects merged");
                delete others[j];
                logger.debug("Others after: " + others[j]);
                return this.merge(await paper, await paper);
            }
            logger.debug("Different objects");
        }
    }

    public merge(firstResponse: IApiResponse, secondResponse: IApiResponse): IApiResponse {
        return secondResponse;
    }

    /*
    1 2 3
    Start iteration i: 1
    1 + 2 => merge into 2 delete 1
    continue
    2 3
    Start iteration i:2
    2 + 3 => 3 => merge into 3 delete 2
    break iteration i:2
    no more iterations

     */
    private _isEqual(firstResponse: IApiResponse, secondResponse: IApiResponse): boolean {
        let firstDOI: string | undefined = this._getDOI(firstResponse.paper);
        let secondDOI: string | undefined = this._getDOI(secondResponse.paper);

        if (firstDOI && secondDOI && firstDOI === secondDOI) {
            return true;
        }
        let sameTitle: number = 0;
        let sameAbstract: number = 0;
        let sameAuthor: number = 0;
        if (firstResponse.paper.title && secondResponse.paper.title) {
            var levTitle = Levenshtein(firstResponse.paper.title, secondResponse.paper.title);
            sameTitle = this.comparisonWeight.titleWeight * ((firstResponse.paper.title.length - levTitle)/firstResponse.paper.title.length); // 0.9  -> 9
        }
        if (firstResponse.paper.abstract && secondResponse.paper.abstract) {
            var levAbstract = Levenshtein(firstResponse.paper.abstract, secondResponse.paper.abstract);
            sameAbstract = this.comparisonWeight.abstractWeight * ((firstResponse.paper.abstract.length - levAbstract)/firstResponse.paper.abstract.length); // 0.9 -> 6.3
        }
        if (firstResponse.paper.author && secondResponse.paper.author) { // 0.7 ->
            sameAuthor = 0.7 * this.comparisonWeight.authorWeight;
        }
        if (((sameTitle + sameAbstract + sameAuthor)/(this.comparisonWeight.titleWeight + this.comparisonWeight.abstractWeight + this.comparisonWeight.authorWeight)) > this.comparisonWeight.overallWeight ) {
            return true;
        }
        return false;
    }

    private _getDOI(paper: IApiPaper): string | undefined {
        let DOI: string;


        if (paper.uniqueId) {
            for (let i: number = 0; i < paper.uniqueId.length; i++) {
                if (paper.uniqueId[i].type == idType.DOI) {
                    return paper.uniqueId[i].value;
                }
            }
        }
        return undefined;
    }


}