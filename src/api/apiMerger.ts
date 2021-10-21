import { IApiMerger } from "./iApiMerger.ts";
import { logger, fileLogger } from "./logger.ts";
import { IApiResponse } from "./iApiResponse.ts";
import { idType } from "./iApiUniqueId.ts";
import { IApiPaper } from "./iApiPaper.ts";
import { Levenshtein } from "./levenshtein.ts";
import { IComparisonWeight } from "./iComparisonWeight.ts";
import { IApiAuthor } from "./iApiAuthor.ts";
import { isEqualAuthor, regexLetterFollowedByPoint } from "./checkIsEqual.ts";
import { concatWithoutDuplicates } from "../helper/assign.ts";
import { isEqualPaper } from "./checkIsEqual.ts"


export class ApiMerger implements IApiMerger {
	public comparisonWeight;

	public constructor(comparisonWeight: IComparisonWeight) {
		this.comparisonWeight = comparisonWeight
	}

	/**
	 * Removes special signs from string and casts it to lower case so strings get compareable for equality
	 *
	 * @param pattern - string to normalize
	 * @returns string without special signs and all lower case
	 */
	static normalizeString(pattern: string): string {
		if (typeof pattern === "string") {
			return pattern.toLowerCase().replace(/[äüö,\\\-:/\.]/g, " ").replace(/ +/g, " ").trim();
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
		let finished: IApiResponse[] = [];
		while (response.length > 1) {

			logger.debug("next round!")
			let finalPaper = await this.comparePaperWithPapers(response.shift()!, response)
			if (finalPaper.position != -1) {
				response[finalPaper.position] = this.makePromise<IApiResponse>({
					paper: finalPaper.item.paper,
					citations: finalPaper.item.citations,
					references: finalPaper.item.references!
				} as IApiResponse)
			} else { finished.push(finalPaper.item) }
		}
		//console.error(JSON.stringify((await response[0]).references!.map((item) => item.author), null, 2))
		if (await response[0]) {
			finished.push(await response[0]);
		}
		/** Merge dublicates coming from the same apis */
		for (let res in finished) {
			finished[res].citations = await this.reviewPaper(finished[res].citations!);
			finished[res].references = await this.reviewPaper(finished[res].references!);
		}

		return finished.filter(item => Array.isArray(item.paper.title));
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
			let isEqual: boolean = await isEqualPaper((await response).paper, (await others[i]).paper, this.comparisonWeight);
			if (isEqual) {
				let otherResponses = others[i];

				/** Comparison for each citation with the citations of the other paper*/
				/** Comparison for each citation with the citations of the other paper*/
				let response1Citations = this._getCiteOrRefList((await response).citations);
				let response2Citations = this._getCiteOrRefList((await others[i]).citations);

				/** Same for references*/
				let response1References = this._getCiteOrRefList((await response).references);
				let response2References = this._getCiteOrRefList((await others[i]).references);


				return {
					position: i,
					item: {
						paper: this.merge((await response).paper, (await otherResponses).paper),
						citations: await this._compareChildren(response1Citations, response2Citations),
						references: await this._compareChildren(response1References, response2References)
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

	private async reviewPaper(finalChildren: IApiPaper[]) {
		finalChildren = finalChildren.filter(item => item);
		for (let i: number = 0; i < finalChildren.length; i++) {
			/** Check if paper is in childpapers of the same api. Since the same paper could return with a different DOI */
			for (let j: number = i + 1; j < finalChildren.length; j++) {

				let isEqual: boolean = await isEqualPaper(finalChildren[i], finalChildren[j], this.comparisonWeight);
				if (isEqual) {
					logger.info(`CONCLUSIVE API paper merging: ${finalChildren[i].uniqueId!.map(item => item.type == idType.DOI ? item.value : undefined)} // ${finalChildren[i].title} <-> ${finalChildren[j].uniqueId!.map(item => item.type == idType.DOI ? item.value : undefined)} // ${finalChildren[j].title}`);
					finalChildren[j] = this.merge(finalChildren[i], finalChildren[j]);
					delete finalChildren[i]
					break;
				}
			}
		}
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
	private async _compareChildren(response1Citations: IApiPaper[], response2Citations: IApiPaper[]) {
		for (let i: number = 0; i < response1Citations.length; i++) {
			/** Check for the same paper in other apis */
			for (let j: number = 0; j < response2Citations.length; j++) {
				let isEqual: boolean = await isEqualPaper(response1Citations[i], response2Citations[j], this.comparisonWeight);
				if (isEqual) {
					logger.info(`DIFFERENT API paper merging: ${response1Citations[i].uniqueId!.map(item => item.type == idType.DOI ? item.value : undefined)} // ${response1Citations[i].title} <-> ${response2Citations[j].uniqueId!.map(item => item.type == idType.DOI ? item.value : undefined)} // ${response2Citations[j].title}`);
					response2Citations[j] = this.merge(response1Citations[i], response2Citations[j]);
					response1Citations = response1Citations.slice(0, i).concat(response1Citations.slice(i + 1))
					break;
				}
			}
		}
		return response1Citations.concat(response2Citations);


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

		for (let f1 = 0; f1 < firstAuthors.length; f1++) {
			for (let s1 = 0; s1 < secondAuthors.length; s1++) {

				let val = isEqualAuthor(firstAuthors[f1], secondAuthors[s1]);
				if (val > this.comparisonWeight.overallWeight) {
					mergingAuthors.push(this._mergeAuthor(firstAuthors[f1], secondAuthors[s1]));
					delete firstAuthors[f1]
					secondAuthors = secondAuthors.slice(0, s1).concat(secondAuthors.slice(s1 + 1))
					break;
				}

			}
		}
		mergingAuthors = mergingAuthors.concat(firstAuthors.filter(item => item), secondAuthors);
		return mergingAuthors.filter(value => Object.keys(value).length !== 0);
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
		let mergedAuthor: any = {};
		let first = <any>firstAuthor;
		let second = <any>secondAuthor;

		/** take the value which is more normalized if the key are equal*/
		for (const key in Object.assign({}, firstAuthor, secondAuthor)) {
			/** Id isnt a list. Since we are only able to merge lists we skip the database id if even given */
			if (key === "id" || first[key] === undefined || second[key] === undefined) {
				continue;
			}

			/** Check if a value of an author property is existing in both AuthorObjects. If so merge, else append. */
			mergedAuthor[key] = [];
			FIRSTLOOP: for (let i in first[key]) {
				second[key] = second[key].filter((item: string) => item)
				for (let j in second[key]) {
					if (first[key][i] && second[key][j]) {
						if (ApiMerger.normalizeString(first[key][i]) === ApiMerger.normalizeString(second[key][j])) {
							mergedAuthor[key].push(this._deriveGenericProperty(first[key][i], second[key][j]));
							second[key] = second[key].slice(0, j).concat(second[key].slice(j + 1))
							continue FIRSTLOOP;
						}
					}
				}
				mergedAuthor[key].push(first[key][i]);
			}
			mergedAuthor[key] = mergedAuthor[key].concat(second[key])
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
	private _deriveGenericProperty(first: string, second: string): string {
		let distance = Levenshtein(first, ApiMerger.normalizeString(first))
		let distance2 = Levenshtein(second, ApiMerger.normalizeString(second))
		return distance > distance2 ? first : second;
	}

	private _deriveRawStringAuthor(mergedAuthor: IApiAuthor): IApiAuthor {

		if (mergedAuthor.firstName!.length === 1 && mergedAuthor.lastName!.length === 1) {
			mergedAuthor.rawString = [`${mergedAuthor.firstName![0]} ${mergedAuthor.lastName![0]}`]
		} else if (mergedAuthor.rawString!.length > 1) {
			//only leaves rawstrings that have a full name, not names like M. Muster
			let rawstring = mergedAuthor.rawString!.filter((item: string) => !item.match(regexLetterFollowedByPoint))
			if (rawstring.length > 0) {
				mergedAuthor.rawString = rawstring;
			}
		}

		if (!mergedAuthor.firstName[0] && mergedAuthor.rawString[0]) {
			if (mergedAuthor.rawString[0].includes(",")) {
				mergedAuthor.firstName.push(mergedAuthor.rawString[0].split(",")[1].trim())
			} else if (mergedAuthor.rawString[0].includes(" ")) {
				mergedAuthor.firstName.push(mergedAuthor.rawString[0].split(" ")[0].trim())
			}
		}

		if (!mergedAuthor.lastName[0] && mergedAuthor.rawString[0]) {
			if (mergedAuthor.rawString[0].includes(",")) {
				mergedAuthor.lastName.push(mergedAuthor.rawString[0].split(",")[0].trim())
			} else if (mergedAuthor.rawString[0].includes(" ")) {
				mergedAuthor.lastName.push(mergedAuthor.rawString[0].split(" ")[1].trim())
			} else {
				mergedAuthor.lastName.push(mergedAuthor.rawString[0])
			}
		}

		return mergedAuthor as IApiAuthor;
	}



	/**
	 * Return DOI of Paper if it has one in it's unique IDs
	 *
	 * @param paper which's DOI to be returned
	 * @param secondResponse - paper similar to firstResponse. Most likely provided by another api
	 * @returns DOI or undefined.
	 */


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
			if (key == "id" || (!first[key] && !second[key]) || key.includes("Source")) {
				continue;
			}
			if (key == "source") {
				resultingPaper[key] = concatWithoutDuplicates(first[key], second[key])
				continue
			}
			if (!first[key + "Source"]) {
				first[key + "Source"] = []
				for (let i = 0; i < first[key].length; i++) {
					first[key + "Source"].push(first.source)
				}
			}
			if (!second[key + "Source"]) {
				second[key + "Source"] = []
				for (let i = 0; i < second[key].length; i++) {
					second[key + "Source"].push(second.source)
				}
			}
			if (first[key].length === 0) {
				resultingPaper[key] = second[key]
				resultingPaper[key + "Source"] = second[key + "Source"]
			} else if (second[key].length === 0) {
				resultingPaper[key] = first[key]
				resultingPaper[key + "Source"] = first[key + "Source"]
			} else if (key == "pdf" || typeof first[key][0] == "number" || typeof second[key][0] == "number") {
				resultingPaper[key] = concatWithoutDuplicates(first[key], second[key])
				resultingPaper[key + "Source"] = []
				for (let i = 0; i < first[key].length; i++) {
					let position = resultingPaper[key].indexOf(first[key][i])
					if (Array.isArray(resultingPaper[key + "Source"][position])) {
						resultingPaper[key + "Source"][position].push(first[key + "Source"][i][0])
					} else {
						resultingPaper[key + "Source"][position] = first[key + "Source"][i]
					}
				}
				for (let i = 0; i < second[key].length; i++) {
					let position = resultingPaper[key].indexOf(second[key][i])
					if (Array.isArray(resultingPaper[key + "Source"][position])) {
						resultingPaper[key + "Source"][position].push(second[key + "Source"][i][0])
					} else {
						resultingPaper[key + "Source"][position] = second[key + "Source"][i]
					}
				}
			} else if (key == "author") {
				resultingPaper.author = this._mergeAuthors(first.author, second.author);
			} else if (key == "uniqueId") {
				resultingPaper[key] = [];
				for (let i = 0; i < first[key].length; i++) {
					for (let j = 0; j < second[key].length; j++) {
						if (ApiMerger.normalizeString(first[key][i].value) == ApiMerger.normalizeString(second[key][j].value)) {
							resultingPaper[key].push(first[key][i]);
							delete first[key][i]
							second[key] = second[key].slice(0, j).concat(second[key].slice(j + 1))
							break;
						}
					}
				}
				resultingPaper[key] = resultingPaper[key].concat(first[key].filter((item: string) => item)).concat(second[key])

			} else {
				resultingPaper[key] = [];
				resultingPaper[key + "Source"] = [];
				for (let i = 0; i < first[key].length; i++) {
					for (let j = 0; j < second[key].length; j++) {
						if (ApiMerger.normalizeString(first[key][i]) == ApiMerger.normalizeString(second[key][j])) {
							resultingPaper[key].push(this._deriveGenericProperty(first[key][i], second[key][j]));
							resultingPaper[key + "Source"].push([first[key + "Source"][i], second[key + "Source"][j]].flat(2))
							delete first[key][i]
							delete first[key + "Source"][i]
							second[key] = second[key].slice(0, j).concat(second[key].slice(j + 1))
							second[key + "Source"] = second[key + "Source"].slice(0, j).concat(second[key + "Source"].slice(j + 1))
							break;
						}
					}
				}
				first[key] = first[key].filter((item: string) => item)
				//console.log(first[key + "Source"])
				first[key + "Source"] = first[key + "Source"].filter((item: string) => item)
				//first[key + "Source"].forEach((item: any, index: number) => first[key + "Source"][index] = item.filter((i: string) => i))
				//first = first.filter(String);
				//second = second.filter(String)
				// console.log(key)
				// console.log('.')
				// console.log(first[key])
				// console.log(first[key + "Source"].filter((item: any) => { return item.filter((item2: string) => { return item2 }) }))
				// console.log(first[key + "Source"]) //.filter((item: string) => console.log(item))
				// console.log('.')
				//console.log(tst.length > 0 ? tst[0].filter((item: string) => item) : "empty")
				//first[k]
				for (let i = 0; i < first[key].length; i++) {
					//console.log(i)
					// console.log(resultingPaper[key + "Source"]);
					// console.log("What are you doin?" + first[key + "Source"][i])
					resultingPaper[key + "Source"].push(first[key + "Source"][i].flat())

				}
				for (let i = 0; i < second[key].length; i++) {
					resultingPaper[key + "Source"].push(second[key + "Source"][i].flat())
				}

				resultingPaper[key] = resultingPaper[key].concat(first[key]).concat(second[key])

			}

		}
		return resultingPaper as IApiPaper;
	}
}

export const getDOI = (paper: { [index: string]: any }): string[] => {
	let DOI: string[] = [];

	if (paper && paper.uniqueId) {
		for (let i: number = 0; i < paper.uniqueId.length; i++) {
			if (paper.uniqueId[i].type == idType.DOI) {
				let s = paper.uniqueId[i].value;
				if (s) {
					DOI.push(s.toLowerCase());
				}
			}
		}
	}
	return DOI;
}
