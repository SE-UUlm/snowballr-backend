import { PaperScopeForStage } from "../../model/db/paperScopeForStage.ts";
import { Paper } from "../../model/db/paper.ts";
import { PaperID } from "../../model/db/paperID.ts";
import { getAllStagesFromProject } from "./stage.ts";
import { Stage } from "../../model/db/stage.ts";
import { Author } from "../../model/db/author.ts";
import { getAllAuthorsFromPaper } from "./author.ts";

export const getAllPapersFromStage = async (id: number): Promise<{ paper: Paper, scope: PaperScopeForStage, authors: Author[] }[]> => {
    const paperScopes = await PaperScopeForStage.where("stageId", id).get()

    if (Array.isArray(paperScopes)) {
        const paperPromises: { paper: Paper, scope: PaperScopeForStage, authors: Author[] }[] = [];
        for (const item of paperScopes) {
            paperPromises.push({ paper: (await Paper.find(Number(item.paperId))) as Paper, scope: item, authors: await getAllAuthorsFromPaper(Number(item.paperId)) })
        }
        return paperPromises
    }
    return new Array<{ paper: Paper, scope: PaperScopeForStage, authors: Author[] }>();
}

export const getPaperSizeOfStage = (id: number): Promise<number> => {
    return PaperScopeForStage.where("stageId", id).count()
}

export const getAllPapersFromProject = async (id: number): Promise<{ papers: { paper: Paper, scope: PaperScopeForStage, authors: Author[] }[], stage: Stage }[]> => {
    const stages = await getAllStagesFromProject(id)
    const allPapers: { papers: { paper: Paper, scope: PaperScopeForStage, authors: Author[] }[], stage: Stage }[] = []
    for (const stage of stages) {
        const papers = { papers: await getAllPapersFromStage(Number(stage.id)), stage: stage }

        allPapers.push(papers)
    }
    return allPapers
}

export const getProjectPaperID = async (stageId: number, paperId: number) => {
    const paperScope = await PaperScopeForStage.where({
        stageId: stageId,
        paperId: paperId
    }).get()
    if (Array.isArray(paperScope) && paperScope[0]) {
        return Number(paperScope[0].id)
    }
    return -1;
}

export const getProjectPaperScope = async (stageId: number, paperId: number) => {
    const paperScope = await PaperScopeForStage.where({
        stageId: stageId,
        paperId: paperId
    }).get()
    if (Array.isArray(paperScope) && paperScope[0]) {
        return paperScope[0]
    }
}

export const getPaperByDoi = async (doi: string): Promise<Paper> => {
    const paper = await Paper.where({ doi: doi }).get()
    return Array.isArray(paper) ? paper[0] : paper
}

export const checkUniqueVal = async (type: string, value: string) => {

    const paperId = await PaperID.where({ type: type, value: value }).get()
    if (Array.isArray(paperId)) {
        return paperId.length > 0
    }

    return false;
}

export const checkPaperInProjectStage = async (paper: Paper, stageID: number) => {
    const pp = await PaperScopeForStage.where({ paperId: Number(paper.id), stageId: stageID }).get()
    if (Array.isArray(pp) && pp.length > 0) {
        return true
    } else {
        return false
    }
}