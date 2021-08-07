import { ApiMerger } from "../../src/api/apiMerger.ts";
import { IApiPaper } from "../../src/api/iApiPaper.ts";
import { IApiResponse } from "../../src/api/iApiResponse.ts";
import { fileLogger } from "../../src/api/logger.ts";

export const logResponse = (response: IApiResponse[]): void => {
    for (let i = 0; i < response.length; i++) {

        fileLogger.info(`PAPER${i}:`);
        fileLogger.info(response[i].paper);
        fileLogger.info("CITATIONS:");

        let citeOriginal = response[i].citations;
        if (citeOriginal) {
            citeOriginal = citeOriginal.sort(sortPapersByName)
        }
        for (let cite in citeOriginal) {
            fileLogger.info((citeOriginal as any)[cite])
        }


        let refOriginal = response[i].references;
        if (refOriginal) {
            refOriginal = refOriginal.sort(sortPapersByName)
        }
        fileLogger.info("REFERENCES:");
        for (let ref in refOriginal) {
            fileLogger.info((refOriginal as any)[ref]);
        }
    }
}

export const sortPapersByName = (item1: IApiPaper, item2: IApiPaper) => {
    if (item1.title && item2.title && item1.title[0] && item2.title[0]) {
        if (item1.title[0].toLowerCase() < item2.title[0].toLowerCase()) {
            return -1
        } else {
            return 1
        }
    }
    return 0
}