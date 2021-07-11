import { IApiMerger } from "./iApiMerger.ts";
import { logger } from "./logger.ts";
import { IApiResponse } from "./iApiResponse.ts";
import { idType } from "./iApiUniqueId.ts";
import { IApiPaper } from "./iApiPaper.ts";
import { Levenshtein } from "./levenshtein.ts";
import { IComparisonWeight } from "./iComparisonWeight.ts";
import { IApiAuthor } from "./iApiAuthor.ts";


export class ApiMerger implements IApiMerger {
	public comparisonWeight = {
		titleWeight: 15,
		titleLevenshtein: 10,
		abstractWeight: 7,
		abstractLevenshtein: 0,
		authorWeight: 8,
		overallWeight: 0.8,
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
			finalPaper.position != -1 ? response[finalPaper.position] = this.makePromise<IApiResponse>({
				paper: finalPaper.item.paper,
				citations: finalPaper.item.citations,
				references: finalPaper.item.references!
			} as IApiResponse) : finished.push(finalPaper.item);
		}
		if (response[0]) {
			finished.push(await response[0]);
		}

		/** Merge dublicates coming from the same apis */
		for (let res in finished) {
			finished[res].citations = this.reviewPaper(finished[res].citations!);
			finished[res].references = this.reviewPaper(finished[res].references!);
		}

		//logger.debug("FINISHED LENGTH " + finished.length)
		return finished;
	}

	/**
	 * This function just makes an object to a promise.
	 * Usefull if, for example, the used function works with async types.
	 * 
	 * @param item item that should be promise 
	 * @returns item wrapped in a promise
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
	public async comparePaperWithPapers(response: Promise<IApiResponse>, others: Promise<IApiResponse>[]): Promise<{ position: number, item: IApiResponse }> {

		/** Iterate over all other apiresponses expecpt for the first one to compare them each */
		for (let i: number = 0; i < others.length; i++) {
			//logger.debug("OTHERS: " + (await others[i]).paper)
			let isEqual: boolean = this._isEqual((await response).paper, (await others[i]).paper);
			if (isEqual) {
				let otherResponses = others[i];

				/** Comparison for each citation with the citations of the other paper*/
				/** Comparison for each citation with the citations of the other paper*/
				let response1Citations = this._getCiteOrRefList((await response).citations);
				let response2Citations = this._getCiteOrRefList((await others[i]).citations);

				/** Same for references*/
				let response1References = this._getCiteOrRefList((await response).references);
				let response2References = this._getCiteOrRefList((await others[i]).references);

				//logger.info("NEXT");

				return {
					position: i,
					item: {
						paper: this.merge((await response).paper, (await otherResponses).paper),
						citations: this._compareChildren(response1Citations, response2Citations),
						references: this._compareChildren(response1References, response2References)
					}
				};
			}

		}
		return { position: -1, item: await response };
	}

	private _getCiteOrRefList(paper?: IApiPaper[]) {
		return paper ? paper : []
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
		if (!firstResponse || !secondResponse) {
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

		let sameAuthor: number = 0;
		let sameYear: number = 0;

		/** Get the levenshtein distance for both titles and compare the in comparison to their length
		 * Weight is used to control the importance of the whole equality formula */
		const [sameTitle, worked1] = this.compareStringArrays(comparison.titleWeight, firstResponse.title, secondResponse.title);
		if (!worked1) {
			comparison.titleWeight = 0;
		}
		/** Get the levenshtein distance for both abstracts and compare the in comparison to their length
		 * Weight is used to control the importance of the whole equality formula */
		const [sameAbstract, worked2] = this.compareStringArrays(comparison.abstractWeight, firstResponse.abstract, secondResponse.abstract)
		if (!worked2) {
			comparison.abstractWeight = 0;
		}

		/** Compare if papers have equal publication year. Year might differ by one for papers published on different platform at the end of the year */
		if (firstResponse.year && secondResponse.year) {
			sameYear = ApiMerger.compareYears(firstResponse.year, secondResponse.year) ? comparison.yearWeight : -comparison.yearWeight;
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

		/*
		if (title1 && title2) {

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

	public static compareYears(firstYear: number[], secondYear: number[]): boolean {
		for (let i in firstYear) {
			for (let j in secondYear) {
				if (firstYear[i] - secondYear[i] in [-1, 0, 1]) {
					return true;
				}
			}
		}
		return false;
	}

	private compareStringArrays(weight: number, first?: string[], second?: string[]): [number, boolean] {
		if (first && second && first[0] && second[0]) {
			let title = second[0];

			let lev = Math.max.apply(null, first.map((item) => {
				return Levenshtein(ApiMerger.normalizeString(item), ApiMerger.normalizeString(title));
			}));

			return [weight * ((second[0].length - lev) / second[0].length), true]; // 0.9  -> 9

		}
		return [0, false];
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
		let mergingAuthors: IApiAuthor[] = [];

		for (let f1 in firstAuthors) {
			for (let s1 in secondAuthors) {

				let val = this._isEqualAuthor(firstAuthors[f1], secondAuthors[s1]);
				if (val) {
					mergingAuthors.push(this._mergeAuthor(firstAuthors[f1], secondAuthors[s1]));
					delete secondAuthors[s1];
					break;
				}

			}
			secondAuthors = secondAuthors.filter(item => item)
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
			/** Check if a value of an author property is existing in both AuthorObjects. If so merge, else append. */
			mergedAuthor[key] = [];//first[key].push.append(second[key]);
			FIRSTLOOP: for (let i in first[key]) {
				for (let j in second[key]) {
					if (first[key][i] && second[key][j] && ApiMerger.normalizeString(first[key][i]) === ApiMerger.normalizeString(second[key][j])) {
						mergedAuthor[key].push(this._deriveGenericProperty(first[key][i], second[key][j]));
						delete second[key][j];
						continue FIRSTLOOP;
					}
				}
				mergedAuthor[key].push(first[key][i]);
			}
		}

		return this._deriveRawStringAuthor(mergedAuthor);
	}

	/**
	 * Returns the value that has a bigger Levenshtein distance to itself after normalizing it.
	 * The idea is that string with capitalization and special characters are more
	 * @param first 
	 * @param second 
	 * @returns 
	 */
	private _deriveGenericProperty(first: string, second: string): any {
		let distance = Levenshtein(first, ApiMerger.normalizeString(first))
		let distance2 = Levenshtein(second, ApiMerger.normalizeString(second))
		return distance > distance2 ? first : second;
	}

	private _deriveRawStringAuthor(mergedAuthor: IApiAuthor): IApiAuthor {
		//TODO: Check if if clauses are necessary
		if (mergedAuthor.rawString) {
			for (let i in mergedAuthor.rawString) {
				if (!Array.isArray(mergedAuthor.lastName) && !Array.isArray(mergedAuthor.firstName) && mergedAuthor.rawString[i] == `${mergedAuthor.lastName}, ${mergedAuthor.firstName}`) {
					mergedAuthor.rawString = [mergedAuthor.rawString[i]];
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
			if ((firstAuthor.firstName.some(item => secondAuthor.firstName!.includes(item))) && (firstAuthor.lastName.some(item => secondAuthor.lastName!.includes(item)))) {
				return 1;
			}
		}
		let s1 = firstAuthor.rawString;
		let s2 = secondAuthor.rawString;
		if (s1 && s2 && s1.length > 0 && s2.length > 0) {

			let equal = 0;
			for (let i = 0; i < s1.length; i++) {
				for (let j = 0; j < s1.length; j++) {
					equal += this._isEqualRawAuthorString(s1[i], s2[j]);
				}
			}

			return equal;

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
		for (const key in Object.assign({}, firstPaper, secondPaper)) {
			if (!first[key] && !second[key]) {
				continue;
			} else if (!first[key]) {
				resultingPaper[key] = second[key]
				continue;
			} else if (!second[key]) {
				resultingPaper[key] = first[key]
				continue;
			}

			if (key == "pdf") {
				resultingPaper.pdf = first.pdf.concat(second.pdf);
				continue;
			}

			if (typeof first[key] === "string") {
				resultingPaper[key] = this._deriveGenericProperty(first[key], second[key]);
			} else if (Array.isArray(first[key])) {

				if (first[key].length === 0) {
					resultingPaper[key] = second[key]
				} else if (second[key].length === 0) {
					resultingPaper[key] = first[key]

				} else if (typeof first[key][0] == "number" || typeof second[key][0] == "number") {
					resultingPaper[key] = first[key].concat(second[key]);
				} else if (key == "author") {
					resultingPaper.author = this._mergeAuthors(first.author, second.author);
				} else {
					resultingPaper[key] = [];
					for (let i = 0; i < first[key].length; i++) {
						for (let j = 0; j < second[key].length; j++) {
							if (ApiMerger.normalizeString(first[key][i]) == ApiMerger.normalizeString(second[key][j])) {
								resultingPaper[key].push(this._deriveGenericProperty(first[key][i], second[key][j]));
								delete first[key][i];
								delete second[key][j];
								break;
							}
						}
						second[key] = second[key].filter((item: string) => item);
					}
					first[key] = first[key].filter((item: string) => item)
					resultingPaper[key] = resultingPaper[key].concat(first[key], second[key])

				}
			} else {
				//TODO needed?
				resultingPaper[key] = first[key];
			}


		}
		return resultingPaper as IApiPaper;
	}
}