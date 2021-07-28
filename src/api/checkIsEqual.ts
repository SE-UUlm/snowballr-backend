import { ApiMerger } from "./apiMerger.ts";
import { IApiAuthor } from "./iApiAuthor.ts";
import { logger } from "./logger.ts";

export const regexLetterFollowedByPoint = /^[a-zA-Z]\..*/g
export const isEqualAuthor = (firstAuthor: IApiAuthor, secondAuthor: IApiAuthor) => {
    let fFirstName = firstAuthor.firstName!.map((item: string) => ApiMerger.normalizeString(item))
    let sFirstName = secondAuthor.firstName!.map((item: string) => ApiMerger.normalizeString(item))
    let fLastName = firstAuthor.lastName!.map((item: string) => ApiMerger.normalizeString(item))
    let sLastName = secondAuthor.lastName!.map((item: string) => ApiMerger.normalizeString(item))

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
    let firstNormalizedItems: string[] = [];
    let secondNormalizedItems: string[] = [];
    try {
        firstNormalizedItems = ApiMerger.normalizeString(firstRawString).split(" ");
        secondNormalizedItems = ApiMerger.normalizeString(secondRawString).split(" ");
    }
    catch (e) {

        logger.critical(e);
        logger.error(`${firstRawString}, ${secondRawString}`);
    }

    //Special case a name is given like M. Muster
    if (firstRawString.match(regexLetterFollowedByPoint) || secondRawString.match(regexLetterFollowedByPoint)) {
        //Check if last name is same, and if yes, check if at least the first name is same
        if (firstNormalizedItems[firstNormalizedItems.length - 1] == secondNormalizedItems[secondNormalizedItems.length - 1]) {
            if (firstNormalizedItems[0].startsWith(secondNormalizedItems[0]) || secondNormalizedItems[0].startsWith(firstNormalizedItems[0])) {
                // TODO hardcoded
                return 0.9;
            }
        }
    } else {
        for (let i in firstNormalizedItems) {
            if (secondNormalizedItems.includes(firstNormalizedItems[i])) {
                equalParts++;
            }
        }
    }
    return equalParts / (firstNormalizedItems.length >= secondNormalizedItems.length ? firstNormalizedItems.length : secondNormalizedItems.length)
}
