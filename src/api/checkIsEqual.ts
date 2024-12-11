import { ApiMerger, getDOI } from "./apiMerger.ts";
import { IApiAuthor } from "./iApiAuthor.ts";
import { IApiPaper } from "./iApiPaper.ts";
import { IComparisonWeight } from "./iComparisonWeight.ts";
import { Levenshtein } from "./levenshtein.ts";
import { logger } from "./logger.ts";

export const regexLetterFollowedByPoint = /^[a-zA-Z][\. ].*/g

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
export const isEqualPaper = (firstResponse: IApiPaper, secondResponse: IApiPaper, comparisonWeight: IComparisonWeight): boolean => {
	if (!firstResponse || !secondResponse) {
		return false
	}
	const comparison: IComparisonWeight = {} as IComparisonWeight;
	Object.assign(comparison, comparisonWeight)
	const firstDOI: string[] = getDOI(firstResponse);
	const secondDOI: string[] = getDOI(secondResponse);


	/** if DOI of 2 paper is equal we can assume that its the same paper */
	for (let i = 0; i < firstDOI.length; i++) {
		for (let j = 0; j < secondDOI.length; j++) {
			if (firstDOI[i] && secondDOI[j]) {
				if (firstDOI[i] == secondDOI[j]) {
					return true
				} else {
					return false
				}
			}
		}
	}

	let sameAuthor: number = 0;
	let sameYear: number = 0;

	/** Get the levenshtein distance for both titles and compare the in comparison to their length
	 * Weight is used to control the importance of the whole equality formula */
	const [sameTitle, worked1] = compareStringArrays(comparison.titleWeight, firstResponse.title, secondResponse.title);
	if (!worked1) {
		comparison.titleWeight = 0;
	}
	/** Get the levenshtein distance for both abstracts and compare the in comparison to their length
	 * Weight is used to control the importance of the whole equality formula */
	const [sameAbstract, worked2] = compareStringArrays(comparison.abstractWeight, firstResponse.abstract, secondResponse.abstract)
	if (!worked2) {
		comparison.abstractWeight = 0;
	}

	/** Compare if papers have equal publication year. Year might differ by one for papers published on different platform at the end of the year */
	if (firstResponse.year && secondResponse.year && firstResponse.year.length > 0 && secondResponse.year.length > 0) {
		sameYear = compareYears(firstResponse.year, secondResponse.year) ? comparison.yearWeight : -comparison.yearWeight;
	} else {
		comparison.yearWeight = 0;
	}

	/** Compare of each of the authors is the same by normalizing them or using the orchid.
	 * Weight is used to control the importance of the whole equality formula */
	if (firstResponse.author && secondResponse.author && firstResponse.author.length > 0 && secondResponse.author.length > 0) { // 0.7 ->
		sameAuthor = isEqualAuthors(firstResponse.author, secondResponse.author) * comparison.authorWeight;

		//TODO: if title and year are equal likely to be equal --> year might be shifted by one year
		//TODO: Take Publisher into Account.
	} else {
		comparison.authorWeight = 0;
	}
	if ((comparison.titleWeight + comparison.abstractWeight) === 0) {
		return false
	}

	/** Calculate the complete equality of 2 papers. OverallWeight is used to kinda control the aggressiveness of the algorithm */
	if (((sameTitle + sameAbstract + sameAuthor + sameYear) / (comparison.titleWeight + comparison.abstractWeight + comparison.authorWeight + comparison.yearWeight)) > comparison.overallWeight) {
		return true;
	}
	return false;

}

const isEqualAuthors = (firstAuthors: IApiAuthor[], secondAuthors: IApiAuthor[]): number => {


	let equalAuthors: number = 0;

	for (const a1 in firstAuthors) {
		for (const a2 in secondAuthors) {
			equalAuthors += isEqualAuthor(firstAuthors[a1], secondAuthors[a2])
		}
	}
	return (equalAuthors / (firstAuthors.length >= secondAuthors.length ? firstAuthors.length : secondAuthors.length))

}

export const isEqualAuthor = (firstAuthor: IApiAuthor, secondAuthor: IApiAuthor) => {
	if (Object.keys(firstAuthor).length === 0 || Object.keys(secondAuthor).length === 0) {
		logger.error("one author value was not set")
		return 0;
	}
	const fFirstName = firstAuthor.firstName!.map((item: string) => ApiMerger.normalizeString(item))
	const sFirstName = secondAuthor.firstName!.map((item: string) => ApiMerger.normalizeString(item))
	const fLastName = firstAuthor.lastName!.map((item: string) => ApiMerger.normalizeString(item))
	const sLastName = secondAuthor.lastName!.map((item: string) => ApiMerger.normalizeString(item))

	if (fFirstName.some((item: string) => sFirstName.includes(item)) && fLastName.some((item: string) => sLastName.includes(item))) {
		return 1;
	}

	let s1 = firstAuthor.rawString!
	let s2 = secondAuthor.rawString!

	if (s1.length === 0 && firstAuthor.firstName!.length > 0 && firstAuthor.lastName!.length > 0) {
		s1 = [`${firstAuthor.firstName![0]} ${firstAuthor.lastName![0]}`]
	}
	if (s2.length === 0 && secondAuthor.firstName!.length > 0 && secondAuthor.lastName!.length > 0) {
		s2 = [`${secondAuthor.firstName![0]} ${secondAuthor.lastName![0]}`]
	}

	let equal = 0;
	for (let i = 0; i < s1.length; i++) {
		for (let j = 0; j < s2.length; j++) {
			equal += isEqualRawAuthorString(s1[i], s2[j]);
		}
	}

	return equal;
}

export const isEqualRawAuthorString = (firstRawString: string, secondRawString: string): number => {
	let equalParts: number = 0;
	const firstNormalizedItems: string[] = ApiMerger.normalizeString(firstRawString).split(" ");
	const secondNormalizedItems: string[] = ApiMerger.normalizeString(secondRawString).split(" ");


	//Special case a name is given like M. Muster or M Muster
	if (firstRawString.match(regexLetterFollowedByPoint) || secondRawString.match(regexLetterFollowedByPoint)) {
		//Check if last name is same, and if yes, check if at least the beginning of the first name is same
		if (firstNormalizedItems[0].charAt(0) == secondNormalizedItems[0].charAt(0)) {

			// TODO hardcoded
			const lev = Levenshtein(firstNormalizedItems[firstNormalizedItems.length - 1], secondNormalizedItems[secondNormalizedItems.length - 1]);
			return ((secondNormalizedItems[secondNormalizedItems.length - 1].length - lev) / secondNormalizedItems[secondNormalizedItems.length - 1].length)

		}
	} else {
		for (const i in firstNormalizedItems) {
			if (secondNormalizedItems.includes(firstNormalizedItems[i])) {
				equalParts++;
			}
		}
	}
	return equalParts / (firstNormalizedItems.length >= secondNormalizedItems.length ? firstNormalizedItems.length : secondNormalizedItems.length)
}

const compareYears = (firstYear: number[], secondYear: number[]): boolean => {
	for (const i in firstYear) {
		for (const j in secondYear) {
			if (firstYear[i] - secondYear[j] in [-1, 0, 1]) {
				return true;
			}
		}
	}
	return false;
}


const compareStringArrays = (weight: number, first?: string[], second?: string[]): [number, boolean] => {
	if (first && second && first[0] && second[0]) {
		const title = second[0];

		const lev = Math.max.apply(null, first.map((item) => {
			return Levenshtein(ApiMerger.normalizeString(item), ApiMerger.normalizeString(title));
		}));

		return [weight * ((second[0].length - lev) / second[0].length), true]; // 0.9  -> 9

	}
	return [0, false];
}