import {Paper} from "../../model/db/paper.ts";
import {PaperMessage} from "../../model/messages/papersMessage.ts";
import {getProjectPaperID} from "../../controller/databaseFetcher/paper.ts";

export const convertPaperToPaperMessage = async (papers: Paper[], stageId?: number) => {
    let paperMessages: PaperMessage[] = [];
    for (const item of papers) {
        let paperMessage: PaperMessage = {id: Number(item.id)}
        if (stageId) {
            paperMessage.ppid = await getProjectPaperID(stageId, Number(item.id))
        }
        if (item.doi) {
            paperMessage.doi = String(item.doi)
        }
        if (item.title) {
            paperMessage.title = String(item.title)
        }
        if (item.abstract) {
            paperMessage.abstract = String(item.abstract)
        }
        if (item.year) {
            paperMessage.year = item.year as Date
        }
        if (item.publisher) {
            paperMessage.publisher = String(item.publisher)
        }
        if (item.type) {
            paperMessage.type = String(item.type)
        }
        if (item.scope) {
            paperMessage.scope = String(item.scope)
        }
        if (item.scopeName) {
            paperMessage.scopeName = String(item.scopeName)
        }
        if (item.createdAt) {
            paperMessage.createdAt = item.createdAt as Date
        }
        if (item.updatedAt) {
            paperMessage.updatedAt = item.updatedAt as Date
        }

        paperMessages.push(paperMessage)
    }

    return paperMessages;
}