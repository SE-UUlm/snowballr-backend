import { PaperScopeForStage } from "../../model/db/paperScopeForStage.ts";
import { Paper } from "../../model/db/paper.ts";
import { PaperID } from "../../model/db/paperID.ts";
import { IApiUniqueId } from "../../api/iApiUniqueId.ts";

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
    if (Array.isArray(paperScope) && paperScope[0]) {
        return Number(paperScope[0].id)
    }
    return -1;
}

export const getProjectPaperScope = async (stageId: number, paperId: number) => {
    let paperScope = await PaperScopeForStage.where({
        stageId: stageId,
        paperId: paperId
    }).get()
    if (Array.isArray(paperScope) && paperScope[0]) {
        return paperScope[0]
    }
}

export const getPaperByDoi = async (doi: string): Promise<Paper> => {
    let paper = await Paper.where({ doi: doi[0] }).get()
    return Array.isArray(paper) ? paper[0] : paper
}

export const checkUniqueVal = async (type: string, value: string) => {

    let paperId = await PaperID.where({ type: type, value: value }).get()
    if (Array.isArray(paperId)) {
        return paperId.length > 0
    }

    return false;
}

export const checkPaperInProjectStage = async (paper: Paper, stageID: number) => {
    let pp = await PaperScopeForStage.where({ paperId: Number(paper.id), stageId: stageID }).get()
    if (Array.isArray(pp) && pp.length > 0) {
        return true
    } else {
        return false
    }
}