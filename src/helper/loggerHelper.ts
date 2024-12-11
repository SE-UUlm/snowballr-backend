import { IApiPaper } from "../api/iApiPaper.ts";
import { IApiResponse } from "../api/iApiResponse.ts";
import { fileLogger } from "../api/logger.ts";
import { Paper } from "../model/db/paper.ts";

export const logResponse = (response: IApiResponse[]): void => {
    for (let i = 0; i < response.length; i++) {

        fileLogger.info(`PAPER${i}:`);
        fileLogger.info(response[i].paper);
        fileLogger.info("CITATIONS:");

        let citeOriginal = response[i].citations;
        if (citeOriginal) {
            citeOriginal = citeOriginal.sort(sortIApiPapersByName)
        }
        for (const cite in citeOriginal) {
            fileLogger.info((citeOriginal as any)[cite])
        }


        let refOriginal = response[i].references;
        if (refOriginal) {
            refOriginal = refOriginal.sort(sortIApiPapersByName)
        }
        fileLogger.info("REFERENCES:");
        for (const ref in refOriginal) {
            fileLogger.info((refOriginal as any)[ref]);
        }
    }
}

export const sortIApiPapersByName = (item1: IApiPaper, item2: IApiPaper) => {
    if (item1.title && item2.title && item1.title[0] && item2.title[0]) {
        if (item1.title[0].toLowerCase() < item2.title[0].toLowerCase()) {
            return -1
        } else {
            return 1
        }
    }
    return 0
}

export const sortPapersByName = (item1: Paper, item2: Paper) => {
    if (String(item1.title).toLowerCase() < String(item2.title).toLowerCase()) {
        return -1
    } else {
        return 1
    }
}