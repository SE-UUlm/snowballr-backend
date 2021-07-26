import { PaperScopeForStage } from "../../model/db/paperScopeForStage.ts";
import { Paper } from "../../model/db/paper.ts";

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

export const getProjectPaperID = async (stageId: number, paperId: number) => {
    let paperScope = await PaperScopeForStage.where({
        stageId: stageId,
        paperId: paperId
    }).get()
    if (Array.isArray(paperScope)) {
        return Number(paperScope[0].id)
    }
    return -1;
}

export const getPaperByDoi = async (doi: string): Promise<Paper> => {
    let paper = await Paper.where({ doi: doi[0] }).get()
    return Array.isArray(paper) ? paper[0] : paper
}