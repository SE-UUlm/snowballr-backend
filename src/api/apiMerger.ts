import {IApiMerger} from "./iApiMerger.ts";
import {logger} from "./logger.ts";
import {IApiResponse} from "./iApiResponse.ts";
import {idType} from "./iApiUniqueId.ts";
import {IApiPaper} from "./iApiPaper.ts";
import {Levenshtein} from "./levenshtein.ts";
import {IComparisonWeight} from "./iComparisonWeight.ts";
import {IApiAuthor} from "./iApiAuthor.ts";


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

    /**
     * Removes special signs from string and casts it to lower case so strings get compareable for equality
     *
     * @param pattern - string to normalize
     * @returns string without special signs and all lower case
     */
    static normalizeString(pattern: any): any {
        if (typeof pattern === "string") {
            return pattern.toLowerCase().replace(/[äüö,]/g, "").trim();
        }
        return pattern;
    }

    /**
     * Compares multiple ApiResponses with each other for all their properties.
     *
     * @param response - list of api responses each from a one api
     * @returns list of api responses without dublicates either by merging them or declaring them different
     */
    public async compare(response: Promise<IApiResponse>[]): Promise<IApiResponse[]> {
        //TODO: async for fastness
        //logger.debug(`\n*************************************`);
        let finished: IApiResponse[] = [];
        while (response.length > 1) {
            logger.debug(`\n*************************************`);
            let element = await this.comparePaperWithPapers(response.shift()!, response)
            if (element) {
                finished.push(element);
            } else {
                finished.push(await response[0]);
            }
        }

        for (let i: number = 0; i < response.length; i++) {
            if (response[i]) {
                finished.push(await response[i])
            }
        }
        return finished;
    }

    /**
     * Compare a paper only with papers that arent compared before
     *
     * @param paper - paper which will be compared with all other papers
     * @param others - other papers
     * @returns list list of merged papers
     */
    public async comparePaperWithPapers(paper: Promise<IApiResponse>, others: Promise<IApiResponse>[]): Promise<IApiResponse | undefined> {
        //logger.debug(await paper);
        //logger.debug(`\n*************************************`);
        //logger.debug(await others[0]);
        //logger.debug(`\n*************************************`);

        for (let j: number = 0; j < others.length; j++) {
            let isEqual: boolean = this._isEqual(await paper, await others[j]);
            if (isEqual) {
                logger.debug("Others before: " + await others[j]);
                logger.debug("Objects merged");
                let otherPaper = others[j];
                delete others[j];
                logger.debug("Others after: " + others[j]);
                return this.merge(await paper, await otherPaper);
            }
            logger.debug("Different objects");
        }
    }

    /**
     * Merge two papers if they are defined as euqal.
     * Resulting paper gets all unique properties.
     * Not unique properties are provided as a list which's final value is to be selected by the user in the frontend.
     *
     * @param firstResponse - paper similar to secondResponse. Most likely provided by another api
     * @param secondResponse - paper similar to firstResponse. Most likely provided by another api
     * @returns IApiResponse that has all unique values and decidable values.
     */
    public merge(firstResponse: IApiResponse, secondResponse: IApiResponse): IApiResponse {
        logger.debug(firstResponse.paper);
        logger.debug(secondResponse.paper);
        let uniqueProperties = this._selectUniqueKey(firstResponse.paper, secondResponse.paper);

        logger.info(uniqueProperties);
        return uniqueProperties as IApiResponse;
    }

    /**
     * Compare two papers for equality
     * Papers are equal if:
     * - the DOI of each paper is equal
     * - or the weighted levinshtein distance of title, abstract and a similarity of the author are greater than a defined threshold
     *    - these parameters are settable by an IComparisonWeight object
     *
     * @param firstResponse - paper similar to secondResponse. Most likely provided by another api
     * @param secondResponse - paper similar to firstResponse. Most likely provided by another api
     * @returns boolean whether the papers are found out to be equal or not
     */

    _isEqual(firstResponse: IApiResponse, secondResponse: IApiResponse): boolean {
        let comparison: IComparisonWeight = {} as IComparisonWeight;
        Object.assign(comparison, this.comparisonWeight)
        let firstDOI: string | undefined = this._getDOI(firstResponse.paper);
        let secondDOI: string | undefined = this._getDOI(secondResponse.paper);

        let sameTitle: number = 0;
        let sameAbstract: number = 0;
        let sameAuthor: number = 0;
        if (firstResponse.paper.title && secondResponse.paper.title) {
            var levTitle = Levenshtein(firstResponse.paper.title.toLowerCase(), secondResponse.paper.title.toLowerCase());
            sameTitle = comparison.titleWeight * ((firstResponse.paper.title.length - levTitle) / firstResponse.paper.title.length); // 0.9  -> 9
        } else {
            comparison.titleWeight = 0;
        }
        if (firstResponse.paper.abstract && secondResponse.paper.abstract) {
            var levAbstract = Levenshtein(firstResponse.paper.abstract.toLowerCase(), secondResponse.paper.abstract.toLowerCase());
            sameAbstract = comparison.abstractWeight * ((firstResponse.paper.abstract.length - levAbstract) / firstResponse.paper.abstract.length); // 0.9 -> 6.3
        } else {
            comparison.abstractWeight = 0;
        }
        //TODO: compare rawstring by splitting them and check that all parts without special signs are equal at some position
        if (firstResponse.paper.author && secondResponse.paper.author) { // 0.7 ->
            sameAuthor = this._isEqualAuthors(firstResponse.paper.author, secondResponse.paper.author) * comparison.authorWeight;
            logger.info("SameAuthor Equality: " + sameAuthor);
        } else {
            comparison.authorWeight = 0;
        }
        if ((comparison.titleWeight + comparison.abstractWeight) === 0) {
            return false;
        }
        logger.info(`weight: ${this.comparisonWeight.abstractWeight}`)
        logger.info(`Title match: ${sameTitle}`);
        logger.info(`Abstract match: ${sameAbstract}`);
        logger.info(`Calculated match of papers: ${((sameTitle + sameAbstract + sameAuthor) / (comparison.titleWeight + comparison.abstractWeight + comparison.authorWeight))}`);
        if (((sameTitle + sameAbstract + sameAuthor) / (comparison.titleWeight + comparison.abstractWeight + comparison.authorWeight)) > comparison.overallWeight) {
            return true;
        }
        return false;
    }

    private _mergeAuthors(firstAuthors: IApiAuthor[], secondAuthors: IApiAuthor[]): IApiAuthor[] {
        if (firstAuthors.length === 0) {
            return secondAuthors as IApiAuthor[];
        } else if (secondAuthors.length === 0) {
            return firstAuthors as IApiAuthor[];
        }

        let mergingAuthors: IApiAuthor[] = [];

        for (let a1 in firstAuthors) {
            for (let a2 in secondAuthors) {
                let val = this._isEqualAuthor(firstAuthors[a1], secondAuthors[a2])
                if (val) {
                    mergingAuthors.push(this._mergeAuthor(firstAuthors[a1], secondAuthors[a2]))
                    delete firstAuthors[a1];
                    delete firstAuthors[a2];
                    break;
                }
            }
        }

        mergingAuthors.push.apply(firstAuthors, secondAuthors);
        return mergingAuthors;
    }

    private _mergeAuthor(firstAuthor: IApiAuthor, secondAuthor: IApiAuthor): IApiAuthor {

        //TODO: if equal compare raw string with lastname and firstname if given and sort rawstring
        let mergedAuthor: any = {};
        let first = <any>firstAuthor;
        let second = <any>secondAuthor;

        for (const key in firstAuthor) {
            if (first[key] && !second[key]) {
                mergedAuthor[key] = first[key]
            } else if (!first[key] && second[key]) {
                mergedAuthor[key] = second[key];
            } else if (ApiMerger.normalizeString(first[key]) == ApiMerger.normalizeString(second[key])) {
                mergedAuthor[key] = first[key];
            } else if (ApiMerger.normalizeString(first[key]) != ApiMerger.normalizeString(second[key])) {
                mergedAuthor[key] = [first[key], second[key]];
            }
        }

        return mergedAuthor as IApiAuthor;
    }

    private _isEqualAuthors(firstAuthors: IApiAuthor[], secondAuthors: IApiAuthor[]): number {
        let equalAuthors: number = 0;

        for (let a1 in firstAuthors) {
            for (let a2 in secondAuthors) {
                equalAuthors += this._isEqualAuthor(firstAuthors[a1], secondAuthors[a2])
            }
        }
        return equalAuthors / (firstAuthors.length >= secondAuthors.length ? firstAuthors.length : secondAuthors.length);
        //return 0.7
    }

    private _isEqualAuthor(firstAuthor: IApiAuthor, secondAuthor: IApiAuthor) {
        if (firstAuthor.firstName && secondAuthor.firstName && firstAuthor.lastName && secondAuthor.lastName) {
            if ((firstAuthor.firstName === secondAuthor.firstName) && (firstAuthor.lastName === secondAuthor.lastName)) {
                return 1;
            }
        }
        // ugly due to compiler error in ts
        let s1 = firstAuthor.rawString;
        let s2 = secondAuthor.rawString;
        if (s1 && s2) {
            return this._isEqualRawAuthorString(s1, s2);
        }
        return 0;
    }

    private _isEqualRawAuthorString(firstRawString: string, secondRawString: string): number {
        let equalParts: number = 0;

        logger.info(`Comparing following: ${firstRawString} <-> ${secondRawString}`);
        let firstNormalizedItems = ApiMerger.normalizeString(firstRawString).split(" ");
        let secondNormalizedItems = ApiMerger.normalizeString(secondRawString).split(" ");
        logger.info(`Comparing following rawString: ${firstNormalizedItems} <-> ${secondNormalizedItems}`);
        for (let i in firstNormalizedItems) {
            if (secondNormalizedItems.includes(firstNormalizedItems[i])) {
                equalParts++;
            }
        }
        return equalParts / (firstNormalizedItems.length >= secondNormalizedItems.length ? firstNormalizedItems.length : secondNormalizedItems.length)
    }

    /**
     * Return DOI of Paper if it has one in it's unique IDs
     *
     * @param paper which's DOI to be returned
     * @param secondResponse - paper similar to firstResponse. Most likely provided by another api
     * @returns DOI or undefined.
     */
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

    /**
     * Iterate over a object and compare its keys.
     * If the values are different add both to a list.
     * If a property is only set in one of the papers take this one.
     * If the values are equal take one of them
     *
     * @param firstPaper object to check properties of
     * @param secondPaper object to check properties of
     * @returns final paper resulting
     */
    private _selectUniqueKey(firstPaper: Object, secondPaper: Object): IApiPaper {
        let resultingPaper: any = {};
        let first = <any>firstPaper;
        let second = <any>secondPaper;
        // for (let firstPaperKey of firstPaper) {
        //     logger.debug(firstPaperKey);
        // }
        for (const key in firstPaper) {
            logger.debug(key);
            logger.debug(`values: ${first[key]} <-> ${second[key]}`)
            if (key == "author") {
                if (first.author && second.author) {
                    logger.info(`!!!!!!!\n Entering author merge\n !!!!!!`);
                    resultingPaper.author = this._mergeAuthors(first.author, second.author);
                } else if (first.author) {
                    resultingPaper.author = first.author
                } else if (second.author) {
                    resultingPaper.author = second.author
                }
                continue;
            }

            if (key == "pdf") {
                logger.info(typeof first.pdf + ' ... ' + typeof second.pdf)
                resultingPaper.pdf = first.pdf.concat(second.pdf)
                continue;
            }

            if (first[key] && !second[key]) {

                logger.info(`Found unique value fo key in first paper: ${key}`);
                resultingPaper[key] = first[key];
            } else if (!first[key] && second[key]) {
                logger.info(`Found unique value for key in second paper: ${key}`);
                resultingPaper[key] = second[key];
            } else if (ApiMerger.normalizeString(first[key]) == ApiMerger.normalizeString(second[key])) {
                logger.info(`Equal values - taking one : ${key}`);
                resultingPaper[key] = first[key];
            } else if (ApiMerger.normalizeString(first[key]) != ApiMerger.normalizeString(second[key])) {
                logger.info(`Different values - can't decide: ${key}`);
                resultingPaper[key] = [first[key], second[key]];
            }

        }
        return resultingPaper as IApiPaper;

    }

}