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
        titleWeight: 15,
        titleLevenshtein: 10,
        abstractWeight: 7,
        abstractLevenshtein: 0,
        authorWeight: 8,
        overallWeight: 0.85,
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
            return pattern.toLowerCase().replace(/[äüö,\\\-:/]/g, " ").replace(/  /g, " ").trim();
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
            logger.debug("next round!")
            let finalPaper = await this.comparePaperWithPapers(response.shift()!, response)
            finalPaper.items.length === 1 ? response[finalPaper.position] = this.makePromise<IApiResponse>({
                paper: finalPaper.items[0].paper,
                citations: this.reviewPaper(finalPaper.items[0].citations!),
                references: this.reviewPaper(finalPaper.items[0].references!)
            } as IApiResponse) : finished.push(finalPaper.items[0]);
            logger.debug("length: " + finalPaper.items.length)
        }
        if (response[0]) {
            finished.push(await response[0]);
        }

        //logger.debug("FINISHED LENGTH " + finished.length)
        return finished;
    }

    /* finished: item 1 und item 2 kein merge => kein weiterarbeiten mit item 1 (kommt auf finish) -> was wenn item1 richtig, aber item2 das falsche?
    item1 / item2 / item3
      finished: item1
      item 2 / item 3
       finished: item1 /merged(2&3)
     */
    async makePromise<T>(item: T) {
        return item
    }

    /**
     * Compare a paper only with papers that arent compared before
     *
     * @param paper - paper which will be compared with all other papers
     * @param others - other papers
     * @returns list list of merged papers
     */
    public async comparePaperWithPapers(response: Promise<IApiResponse>, others: Promise<IApiResponse>[]): Promise<{ position: number, items: IApiResponse[] }> {

        //logger.debug("OTHERS LENGTH: " + others.length)
        //logger.debug("RESPONSE: " + (await response).paper)
        /** Iterate over all other apiresponses expecpt for the first one to compare them each */
        for (let i: number = 0; i < others.length; i++) {
            //logger.debug("OTHERS: " + (await others[i]).paper)
            let isEqual: boolean = this._isEqual((await response).paper, (await others[i]).paper);
            if (isEqual) {
                let otherResponses = others[i];

                /** Comparison for each citation with the citations of the other paper*/
                let response1Citations = (await response).citations;
                response1Citations = response1Citations ? response1Citations : [];
                let response2Citations = (await others[i]).citations;
                response2Citations = response2Citations ? response2Citations : [];

                /** Same for references*/
                let response1References = (await response).references;
                //TODO weird filters
                response1References = response1References ? response1References : [];
                let response2References = (await others[i]).references;
                response2References = response2References ? response2References : [];

                //logger.info("NEXT");

                return {
                    position: i,
                    items: [{
                        paper: this.merge((await response).paper, (await otherResponses).paper),
                        citations: this._compareChildren(response1Citations, response2Citations),
                        references: this._compareChildren(response1References, response2References)
                    }]
                };
            }

        }
        return {position: -1, items: [await response, await others[0]]};
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
        // if (firstResponse.title && secondResponse.title && firstResponse.title[0] && secondResponse.title[0] && firstResponse.title[0].toLowerCase().includes("metar") && secondResponse.title[0].toLowerCase().includes("metar")) {
        //     logger.info(`MERGE metar ---------------------`)
        //     //logger.info(firstResponse)
        //     //logger.info(secondResponse)
        // }
        let uniqueProperties = this._selectUniqueKey(firstResponse, secondResponse);
        return uniqueProperties as IApiPaper;
    }

    private reviewPaper(finalChildren: IApiPaper[]) {
        finalChildren = finalChildren.filter(item => item);
        for (let i: number = 0; i < finalChildren.length; i++) {
            /** Check if paper is in childpapers of the same api. Since the same paper could return with a different DOI */
            for (let j: number = i + 1; j < finalChildren.length; j++) {

                let isEqual: boolean = this._isEqual(finalChildren[i], finalChildren[j]);
                if (isEqual) {
                    logger.info(`CONCLUSIVE API paper merging: ${finalChildren[i].uniqueId!.map(item => item.type == idType.DOI ? item.value : undefined)} // ${finalChildren[i].title} <-> ${finalChildren[j].uniqueId!.map(item => item.type == idType.DOI ? item.value : undefined)} // ${finalChildren[j].title}`);
                    finalChildren[j] = this.merge(finalChildren[i], finalChildren[j]);
                    delete finalChildren[i];
                    break;
                }
            }
        }
        //finalChildren = finalChildren.filter(item => item);
        return finalChildren.filter(item => item);
    }

    /**
     * Compare list of childobject from a reponse object with a list of childobjects from another response
     * EG: citations with citations or references with references
     * delete the citaiton in the second paper so it wont be compared once more
     *
     * @param response1Citations - child paper of an api response like reference or citation
     * @param response2Citations - child paper of an api response like reference or citation
     * @returns IApiResponse only unique paper child objkects
     */
    private _compareChildren(response1Citations: IApiPaper[], response2Citations: IApiPaper[]) {
        //logger.debug("Comparing Refs or Cites");

        response1Citations = response1Citations.filter(item => item);
        response2Citations = response2Citations.filter(item => item);
        for (let i: number = 0; i < response1Citations.length; i++) {
            /** Check for the same paper in other apis */
            for (let k: number = 0; k < response2Citations.length; k++) {
                let isEqual: boolean = this._isEqual(response1Citations[i], response2Citations[k]);
                if (isEqual) {
                    logger.info(`DIFFERENT API paper merging: ${response1Citations[i].uniqueId!.map(item => item.type == idType.DOI ? item.value : undefined)} // ${response1Citations[i].title} <-> ${response2Citations[k].uniqueId!.map(item => item.type == idType.DOI ? item.value : undefined)} // ${response2Citations[k].title}`);
                    response2Citations[k] = this.merge(response1Citations[i], response2Citations[k]);
                    delete response1Citations[i];
                    break;
                }
            }
            response1Citations = response1Citations.filter(item => item);
        }
        // response2Citations.forEach(item => {
        //     if (item.title && item.title[0] && item.title[0].toLowerCase().includes("metar")) {
        //         logger.info(`item: ${JSON.stringify(item)}`)
        //     }
        // })
        try {
            return response1Citations.concat(response2Citations.filter(item => item));
        } catch (e) {
            logger.error(`Cannot delete undefined: ${response2Citations}`);
            logger.error(`Error: ${e}`);
            return response1Citations.concat(response2Citations);
        }

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
        if(!firstResponse || !secondResponse){
            return false
        }
        let comparison: IComparisonWeight = {} as IComparisonWeight;
        Object.assign(comparison, this.comparisonWeight)
        let firstDOI: string[] = this._getDOI(firstResponse);
        let secondDOI: string[] = this._getDOI(secondResponse);


        /** if DOI of 2 paper is equal we can assume that its the same paper */
        for (let i = 0; i < firstDOI.length; i++) {
            for (let j = 0; j < secondDOI.length; j++) {
                if (firstDOI[i] == secondDOI[j]) {
                    //TODO test for rest, commented out
                    return true;
                }

            }
        }

        let sameTitle: number = 0;
        let sameAbstract: number = 0;
        let sameAuthor: number = 0;
        let sameYear: number = 0;
        let title1 = firstResponse.title;
        let title2 = secondResponse.title;

        /** Get the levenshtein distance for both titles and compare the in comparison to their length
         * Weight is used to control the importance of the whole equality formula */

        if (firstResponse.title && secondResponse.title && secondResponse.title[0] && firstResponse.title[0]) {
            let title = secondResponse.title[0];
            if (title1 && title2 && title1[0].toLowerCase().includes("metar") && title2[0].toLowerCase().includes("metar")) {
                firstResponse.title.map((item: string) => {
                    logger.info(`${item.toLowerCase()} <=> ${title.toLowerCase()}`)
                    logger.info(Levenshtein(item.toLowerCase(), title.toLowerCase()));
                    logger.info(Levenshtein(ApiMerger.normalizeString(item), ApiMerger.normalizeString(title.toLowerCase())));

                    // @ts-ignore
                    logger.info(`MAX: ${Math.max.apply(null, firstResponse.title.map((item: string) => {
                        return Levenshtein(item.toLowerCase(), title.toLowerCase());
                    }))}`)

                    // @ts-ignore
                    logger.info(`MAX2: ${Math.max.apply(null, firstResponse.title.map((item: string) => {
                        return Levenshtein(ApiMerger.normalizeString(item), ApiMerger.normalizeString(title.toLowerCase()));
                    }))}`)
                })

            }

            let levTitle = Math.max.apply(null, firstResponse.title.map((item: string) => {
                return Levenshtein(ApiMerger.normalizeString(item), ApiMerger.normalizeString(title));
            }));

            sameTitle = comparison.titleWeight * ((secondResponse.title[0].length - levTitle) / secondResponse.title[0].length); // 0.9  -> 9

        } else {
            comparison.titleWeight = 0;
        }

        /** Get the levenshtein distance for both abstracts and compare the in comparison to their length
         * Weight is used to control the importance of the whole equality formula */
        if (firstResponse.abstract && secondResponse.abstract && firstResponse.abstract[0] && secondResponse.abstract[0]) {
            let abstract = secondResponse.abstract[0];

            let levAbstract = Math.max.apply(null, firstResponse.abstract.map((item: any) => Levenshtein(ApiMerger.normalizeString(item), ApiMerger.normalizeString(abstract))));
            sameAbstract = comparison.abstractWeight * ((secondResponse.abstract[0].length - levAbstract) / secondResponse.abstract[0].length); // 0.9  -> 9
        } else {
            comparison.abstractWeight = 0;
        }
        //TODO: compare rawstring by splitting them and check that all parts without special signs are equal at some position

        if (firstResponse.year && secondResponse.year) {
            sameYear = firstResponse.year - secondResponse.year in [-1, 0, 1] ? comparison.yearWeight : -comparison.yearWeight;
        } else {
            comparison.yearWeight = 0;
        }

        /** Compare of each of the authors is the same by normalizing them or using the orchid.
         * Weight is used to control the importance of the whole equality formula */
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


       /* if (title1 && title2) {

            console.error(`another isequal with ${title1[0]} and ${title2[0]} => ${sameTitle} +  ${sameAbstract} + ${sameAuthor} + ${sameYear}`)
            console.error(`${comparison.titleWeight} + ${comparison.abstractWeight} + ${comparison.authorWeight} + ${comparison.yearWeight}`)
            console.error(`${((sameTitle + sameAbstract + sameAuthor + sameYear) / (comparison.titleWeight + comparison.abstractWeight + comparison.authorWeight + comparison.yearWeight))}`)
        }
        */
        /** Calculate the complete equality of 2 papers. OverallWeight is used to kinda control the aggressiveness of the algorithm */

        if (((sameTitle + sameAbstract + sameAuthor + sameYear) / (comparison.titleWeight + comparison.abstractWeight + comparison.authorWeight + comparison.yearWeight)) > comparison.overallWeight) {
            return true;
        }
        return false;
    }

    /**
     * Compares all author objects of 2 papers and merge dublicates into a single list of authors.
     *
     *
     * @param firstResponse - paper similar to secondResponse. Most likely provided by another api
     * @param secondResponse - paper similar to firstResponse. Most likely provided by another api
     * @returns boolean whether the papers are found out to be equal or not
     */
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

    /**
     * If an author object is merged decide which subproperties of a single dublicated author is to be taken
     *
     *
     * @param firstAuthor - single author object dublicated
     * @param secondAuthor - single author object dublicated
     * @returns IApiAuthor which contains optionals or the most fitting values
     */
    private _mergeAuthor(firstAuthor: IApiAuthor, secondAuthor: IApiAuthor): IApiAuthor {
        //TODO: if equal compare raw string with lastname and firstname if given and sort rawstring
        let mergedAuthor: any = {};
        let first = <any>firstAuthor;
        let second = <any>secondAuthor;

        /** take the value which is more normalized if the key are equal*/
        for (const key in firstAuthor) {
            if (!first[key] && !second[key]) {
                continue;
            }
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
            if (Array.isArray(s1) && Array.isArray((s2))) {
                let equal = 0;
                for (let i = 0; i < s1.length; i++) {
                    for (let j = 0; j < s1.length; j++) {
                        equal += this._isEqualRawAuthorString(s1[i], s2[j]);
                    }
                }
                //logger.debug("equal weird: " + equal)
                return equal;
            } else if (Array.isArray(s1)) {
                let equal = 0;
                for (let i = 0; i < s1.length; i++) {
                    equal += this._isEqualRawAuthorString(s1[i], s2);
                }
                //logger.debug("equal 1 weird: " + equal)
                return equal;
            } else if (Array.isArray(s2)) {
                let equal = 0;
                for (let i = 0; i < s2.length; i++) {
                    equal += this._isEqualRawAuthorString(s1, s2[i])
                }
                //logger.debug("equal 2 weird: " + equal)
                return equal;
            }
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
    private _getDOI(paper: IApiPaper): string[] {
        let DOI: string[] = [];

        if (paper && paper.uniqueId) {
            for (let i: number = 0; i < paper.uniqueId.length; i++) {
                if (paper.uniqueId[i].type == idType.DOI) {
                    let s = paper.uniqueId[i].value;
                    if (s) {
                        DOI.push(s);
                    }
                }
            }
        }
        return DOI;
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
        let keys = {}
        for (const key in Object.assign({}, firstPaper, secondPaper)) {
            if (!first[key] && !second[key]) {
                continue;
            }
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
            } else {
                if (typeof first[key] === "string") {
                    resultingPaper[key] = this._deriveGenericPropertyAuthor(first[key], second[key]);
                } else if (Array.isArray(first[key])) {
                    if (first[key].length === 0) {
                        resultingPaper[key] = second[key]
                        continue;
                    }
                    if (second[key].length === 0) {
                        resultingPaper[key] = first[key]
                        continue;
                    }
                    if (typeof first[key][0] == "number" || typeof second[key][0] == "number") {
                        resultingPaper[key] = first[key].concat(second[key]);
                        continue;
                    }
                    let normalized = first[key].map((item: string) => ApiMerger.normalizeString(item));
                    if (first[key].includes(second[key])) {
                        resultingPaper[key] = first[key];
                    } else if (normalized.includes(ApiMerger.normalizeString(second[key][0]))) {
                        let index = normalized.indexOf(ApiMerger.normalizeString(second[key][0]));
                        resultingPaper[key] = [this._deriveGenericPropertyAuthor(first[key][index], second[key][0])];
                        delete first[key][index];
                        first[key] = first[key].filter((item: any) => item);
                        if (first[key].length > 0) {
                            resultingPaper[key] = resultingPaper[key].concat(first[key]);
                        }
                    } else {
                        resultingPaper[key] = (first[key].concat(second[key]));
                    }
                    /*
                    arr1: [MegaTitle, mega title of doom]
                    arr2: [mega Title of Doom]
                    => [MegaTitle, mega Title of Doom]
                     */
                } else {
                    //TODO needed?
                    resultingPaper[key] = first[key];
                }
            }

        }
        return resultingPaper as IApiPaper;
    }

}