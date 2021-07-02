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
        overallWeight: 0.7,
        yearWeight: 2
    } as IComparisonWeight;

    public constructor(comparisonWeight?: IComparisonWeight) {
        this.comparisonWeight = comparisonWeight ? comparisonWeight : this.comparisonWeight;
    }

    /**
     * Removes special signs from string and casts it to lower case so strings get compareable for equality
     *
     * @param pattern - string to normalize
     * @returns string without special signs and all lower case
     */
    static normalizeString(pattern: any): any {
        if (typeof pattern === "string") {
            return pattern.toLowerCase().replace(/[äüö,\\\-/]/g, " ").trim();
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
        let finished: IApiResponse[] = [];
        while (response.length > 1) {
            logger.debug("new RUN");
            let stuff = await this.comparePaperWithPapers(response.shift()!, response);
            logger.debug(JSON.stringify(stuff, null, 2))
            stuff.forEach(item => finished.push(item))
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
    public async comparePaperWithPapers(response: Promise<IApiResponse>, others: Promise<IApiResponse>[]): Promise<IApiResponse[]> {

        //   logger.debug(JSON.stringify(await response, null, 2) + "\n")
        logger.debug("OTHERS LENGTH: " + others.length)
        logger.debug("RESPONSE: " + (await response).paper)

        //   Promise.all(others).then((item: any) => {
        //       logger.debug(JSON.stringify(item, null, 2) + "\n")
        //   })
        for (let i: number = 0; i < others.length; i++) {
            logger.debug("OTHERS: " + (await others[i]).paper)
            let isEqual: boolean = this._isEqual((await response).paper, (await others[i]).paper);
            if (isEqual) {
                logger.debug("Others before: " + await others[j]);
                logger.debug("Objects merged");
                let otherResponses = others[i];

                let response1Citations = (await response).citations;
                response1Citations = response1Citations ? response1Citations : [];
                let response2Citations = (await others[i]).citations;
                response2Citations = response2Citations ? response2Citations : [];

                let response1References = (await response).references;
                response1References = response1References ? response1References : [];
                let response2References = (await others[i]).references;
                response2References = response2References ? response2References : [];

                delete others[i];
                return {
                    paper: this.merge((await response).paper, (await otherResponses).paper),
                    citations: this._compareChildren(response1Citations, response2Citations),
                    references: this._compareChildren(response1References, response2References)
                };
            }

        }
        return [await response, await others[0]];
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
    public merge(firstResponse: IApiPaper, secondResponse: IApiPaper): IApiPaper {
        let uniqueProperties = this._selectUniqueKey(firstResponse, secondResponse);

        return uniqueProperties as IApiPaper;
    }

    private _compareChildren(response1Citations: IApiPaper[], response2Citations: IApiPaper[]) {
        logger.debug("Comparing Refs or Cites");

        for (let j: number = 0; j < response1Citations.length; j++) {
            for (let k: number = 0; k < response2Citations.length; k++) {
                let isEqual: boolean = this._isEqual(response1Citations[j], response2Citations[k]);
                if (isEqual) {
                    response1Citations[j] = this.merge(response1Citations[j], response2Citations[k]);
                    delete response2Citations[k];
                    //TODO make possible for 2+ api
                    break;
                }
            }
            response2Citations = response2Citations.filter(item => item);
        }
        return response1Citations.concat(response2Citations)
    }

    /**
     * Compare two papers for equality
     * Papers are equal if:
     * - the DOI of each paper is equal
     * - or the weighted levenshtein distance of title, abstract and a similarity of the author are greater than a defined threshold
     *    - these parameters are settable by an IComparisonWeight object
     *
     * @param firstResponse - paper similar to secondResponse. Most likely provided by another api
     * @param secondResponse - paper similar to firstResponse. Most likely provided by another api
     * @returns boolean whether the papers are found out to be equal or not
     */

    private _isEqual(firstResponse: IApiPaper, secondResponse: IApiPaper): boolean {
        let comparison: IComparisonWeight = {} as IComparisonWeight;
        Object.assign(comparison, this.comparisonWeight)
        let firstDOI: string | undefined = this._getDOI(firstResponse);
        let secondDOI: string | undefined = this._getDOI(secondResponse);

        let sameTitle: number = 0;
        let sameAbstract: number = 0;
        let sameAuthor: number = 0;
        let sameYear: number = 0;
        if (firstResponse.title && secondResponse.title) {
            try {
                var levTitle = Levenshtein(firstResponse.title.toLowerCase(), secondResponse.title.toLowerCase());
                sameTitle = comparison.titleWeight * ((firstResponse.title.length - levTitle) / firstResponse.title.length); // 0.9  -> 9
            } catch (err) {
                err.print()
            }

        } else {
            comparison.titleWeight = 0;
        }
        if (firstResponse.abstract && secondResponse.abstract) {
            var levAbstract = Levenshtein(firstResponse.abstract.toLowerCase(), secondResponse.abstract.toLowerCase());
            sameAbstract = comparison.abstractWeight * ((firstResponse.abstract.length - levAbstract) / firstResponse.abstract.length); // 0.9 -> 6.3
        } else {
            comparison.abstractWeight = 0;
        }
        //TODO: compare rawstring by splitting them and check that all parts without special signs are equal at some position

        if (firstResponse.year && secondResponse.year) {
            sameYear = firstResponse.year - secondResponse.year in [-1, 0, 1] ? comparison.yearWeight : -comparison.yearWeight;
        } else {
            comparison.yearWeight = 0;
        }

        if (firstResponse.author && secondResponse.author) { // 0.7 ->
            sameAuthor = this._isEqualAuthors(firstResponse.author, secondResponse.author) * comparison.authorWeight;

            //TODO: if title and year are equal likely to be equal --> year might be shifted by one year
            //TODO: Take Publisher into Account.
        } else {
            comparison.authorWeight = 0;
        }
        if ((comparison.titleWeight + comparison.abstractWeight) === 0) {
            return false;
        }

        if (((sameTitle + sameAbstract + sameAuthor + sameYear) / (comparison.titleWeight + comparison.abstractWeight + comparison.authorWeight + comparison.yearWeight)) > comparison.overallWeight) {
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

    // wenn normalized(rawName) = normalized(lastName),normalized(firstName)
    // wenn normalized beide gleich, wenn mehr großgeschrieben nimm den und vllt "," drin


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
                mergedAuthor[key] = this._deriveGenericPropertyAuthor(first[key], second[key]);
            } else if (ApiMerger.normalizeString(first[key]) != ApiMerger.normalizeString(second[key])) {
                mergedAuthor[key] = [first[key], second[key]];
            }
        }

        return this._deriveRawStringAuthor(mergedAuthor);
    }

    private _deriveGenericPropertyAuthor(first: string, second: string): any {
        let distance = Levenshtein(first, ApiMerger.normalizeString(first))
        let distance2 = Levenshtein(second, ApiMerger.normalizeString(second))
        return distance > distance2 ? first : second;
    }

    private _deriveRawStringAuthor(mergedAuthor: IApiAuthor): IApiAuthor {
        if (Array.isArray(mergedAuthor.rawString)) {
            for (let i in mergedAuthor.rawString) {
                if (!Array.isArray(mergedAuthor.lastName) && !Array.isArray(mergedAuthor.firstName) && mergedAuthor.rawString[i] == `${mergedAuthor.lastName}, ${mergedAuthor.firstName}`) {
                    mergedAuthor.rawString = mergedAuthor.rawString[i];
                    break;
                }
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

// TODO: first name might be shortened while last name is most likely not shortened. handle this via a heuristic
    private _isEqualAuthor(firstAuthor: IApiAuthor, secondAuthor: IApiAuthor) {
        if (firstAuthor.firstName && secondAuthor.firstName && firstAuthor.lastName && secondAuthor.lastName) {
            if ((firstAuthor.firstName === secondAuthor.firstName) && (firstAuthor.lastName === secondAuthor.lastName)) {
                return 1;
            }
        }
        let s1 = firstAuthor.rawString;
        let s2 = secondAuthor.rawString;
        if (s1 && s2) {
            return this._isEqualRawAuthorString(s1, s2);
        }
        return 0;
    }

    private _isEqualRawAuthorString(firstRawString: string, secondRawString: string): number {
        let equalParts: number = 0;

        let firstNormalizedItems = ApiMerger.normalizeString(firstRawString).split(" ");
        let secondNormalizedItems = ApiMerger.normalizeString(secondRawString).split(" ");

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

        if (paper && paper.uniqueId) {
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

        for (const key in firstPaper) {

            if (key == "author") {
                if (first.author && second.author) {
                    resultingPaper.author = this._mergeAuthors(first.author, second.author);
                } else if (first.author) {
                    resultingPaper.author = first.author
                } else if (second.author) {
                    resultingPaper.author = second.author
                }
                continue;
            }

            if (key == "pdf") {
                if (first.pdf && second.pdf) {
                    resultingPaper.pdf = first.pdf.concat(second.pdf);
                } else if (first.pdf) {
                    resultingPaper.pdf = first.pdf;
                } else if (second.pdf) {
                    resultingPaper.pdf = second.pdf;
                }
                continue;
            }

            if (first[key] && !second[key]) {
                resultingPaper[key] = first[key];
            } else if (!first[key] && second[key]) {
                resultingPaper[key] = second[key];
            } else if (ApiMerger.normalizeString(first[key]) == ApiMerger.normalizeString(second[key])) {
                if (typeof first[key] === "string") {
                    resultingPaper[key] = this._deriveGenericPropertyAuthor(first[key], second[key]);
                } else {
                    resultingPaper[key] = first[key];
                }
            } else if (ApiMerger.normalizeString(first[key]) != ApiMerger.normalizeString(second[key])) {
                resultingPaper[key] = [first[key], second[key]];
            }

        }
        return resultingPaper as IApiPaper;

    }

}