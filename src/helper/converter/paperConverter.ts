import {Paper} from "../../model/db/paper.ts";
import {PaperMessage} from "../../model/messages/papersMessage.ts";
import {getProjectPaperID} from "../../controller/databaseFetcher/paper.ts";
import {assign} from "../assign.ts"

export const convertPapersToPaperMessage = async (papers: Paper[], stageId: number) => {
    let paperMessages: PaperMessage[] = [];
    for (const item of papers) {
        let paperMessage: PaperMessage = await convertPaperToPaperMessage(item, stageId)
        paperMessages.push(paperMessage)
    }
    return paperMessages;
}

export const convertPaperToPaperMessage = async (paper: Paper, stageId: number) => {
    let paperMessage: PaperMessage = {id: Number(paper.id), ppid: await getProjectPaperID(stageId, Number(paper.id))}
    assign(paperMessage, paper)
    return paperMessage;
}

