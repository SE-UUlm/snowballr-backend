import { Paper } from "../../model/db/paper.ts";
import { PaperMessage } from "../../model/messages/papersMessage.ts";
import { getProjectPaperID } from "../../controller/databaseFetcher/paper.ts";
import { assign } from "../assign.ts"
import { IApiPaper } from "../../api/iApiPaper.ts"
import { getDOI } from "../../api/apiMerger.ts";
import { logger, fileLogger } from "../../api/logger.ts";
import { PaperID } from "../../model/db/paperID.ts";
import { PaperHasID } from "../../model/db/paperHasID.ts";
export const convertPapersToPaperMessage = async (papers: Paper[], stageId: number) => {
    let paperMessages: PaperMessage[] = [];
    for (const item of papers) {
        let paperMessage: PaperMessage = await convertPaperToPaperMessage(item, stageId)
        paperMessages.push(paperMessage)
    }
    return paperMessages;
}

export const convertPaperToPaperMessage = async (paper: Paper, stageId: number) => {
    let paperMessage: PaperMessage = { id: Number(paper.id), ppid: await getProjectPaperID(stageId, Number(paper.id)) }
    assign(paperMessage, paper)
    return paperMessage;
}


export const convertIApiPaperToDBPaper = async (paper: { [index: string]: any }): Promise<Paper | undefined> => {
    if (checkIApiPaper(paper)) {
        let paperId: PaperID | PaperID[] = await PaperID.where({ value: getDOI(paper)[0], type: "DOI" }).get()
        let newPaper: Paper;
        if (Array.isArray(paperId) && paperId.length > 0) {
            //TODO JOIN & MERGE
            let paperHasId = PaperHasID.where({ idId: Number(paperId[0].id) }).get()
            newPaper = await Paper.create({})
        } else {
            newPaper = await Paper.create({})
            for (let i in paper) {
                if (["title", "abstract", "publisher", "type", "scopeName", "year"].includes(i)) {
                    newPaper[i] = paper[i][0]
                } else if (i == "uniqueId") {
                    //TODO rest unique keys
                    newPaper.doi = getDOI(paper)[0]
                } else if (i == "author") {
                    //TODO author
                } else if (i == "pdf") {
                    //TODO pdf
                }
            }
            await newPaper.update()
        }

        return newPaper;
    }


}

const checkIApiPaper = (paper: { [index: string]: any }): boolean => {
    for (let i in paper) {
        if (!["id", "uniqueId", "source", "author", "pdf"].includes(i)) {

            if (paper[i] && paper[i].length > 1) {
                logger.critical(`${i} wasn't single`)
                return false;
            }
        }
    }
    return true;
}