import { PaperScopeForStage } from "../../model/db/paperScopeForStage.ts";
import { Paper } from "../../model/db/paper.ts";
import { PaperID } from "../../model/db/paperID.ts";
import { IApiUniqueId } from "../../api/iApiUniqueId.ts";
import { getAllStagesFromProject } from "./stage.ts";
import { Stage } from "../../model/db/stage.ts";
import { Author } from "../../model/db/author.ts";
import { getAllAuthorsFromPaper } from "./author.ts";

export const getAllPapersFromStage = async (id: number):Promise<{paper : Promise<Paper>, scope: PaperScopeForStage, authors: Promise<Author[]>}[]> => {
    let paperScopes = await PaperScopeForStage.where("stageId", id).get()

    if (Array.isArray(paperScopes)) {
        let paperPromises: {paper : Promise<Paper>, scope: PaperScopeForStage, authors: Promise<Author[]>}[] = [];
        paperScopes.forEach((item: PaperScopeForStage) => {
            paperPromises.push({paper: (Paper.find(Number(item.paperId)) as Promise<Paper>), scope: item, authors:  getAllAuthorsFromPaper(Number(item.paperId))})
        })
        return Promise.all(paperPromises)
    }
    return new Array<{paper : Promise<Paper>, scope: PaperScopeForStage, authors: Promise<Author[]>}>();
}

export const getAllPapersFromProject = async (id: number): Promise<{ papers: { paper: Promise<Paper>, scope: PaperScopeForStage , authors: Promise<Author[]>}[], stage: Stage }[]> => {
    let stages = await getAllStagesFromProject(id)
    let allPapers: { papers: { paper: Promise<Paper>, scope: PaperScopeForStage , authors: Promise<Author[]>}[], stage: Stage }[] = []
    for(let stage of stages){
        let papers = {papers: await getAllPapersFromStage(Number(stage.id)), stage: stage}
        
        allPapers.push(papers)
    }
    return allPapers
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
    let paper = await Paper.where({ doi: doi }).get()
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