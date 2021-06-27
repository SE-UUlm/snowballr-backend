import {PaperScopeForStage} from "../../model/db/paperScopeForStage.ts";
import {Paper} from "../../model/db/paper.ts";

export const getAllPapersFromStage = async (id: number) => {
    let paperScope = await PaperScopeForStage.where("stageId", id).get()

    if (Array.isArray(paperScope)) {
        let paperPromises: Promise<Paper>[] = [];
        paperScope.forEach((item: PaperScopeForStage) => {
            paperPromises.push(Paper.find(Number(item.paperId)))
        })
        return Promise.all(paperPromises)
    }
    return new Array<Paper>();
}