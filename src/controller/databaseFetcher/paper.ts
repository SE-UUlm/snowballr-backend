import { PaperScopeForStage } from "../../model/db/paperScopeForStage.ts";
import { Paper } from "../../model/db/paper.ts";
import { PaperID } from "../../model/db/paperID.ts";
import { IApiUniqueId } from "../../api/iApiUniqueId.ts";
import { getAllStagesFromProject } from "./stage.ts";
import { Stage } from "../../model/db/stage.ts";
import { Author } from "../../model/db/author.ts";
import { getAllAuthorsFromPaper } from "./author.ts";
import { Wrote } from "../../model/db/wrote.ts";
import { PaperMessage, Status } from "../../model/messages/papersMessage.ts";
import { AuthorMessage } from "../../model/messages/author.message.ts";
import { Pdf } from "../../model/db/pdf.ts";
import { paperCache } from "../project.controller.ts";
import { checkUserReviewOfProjectPaper, getAllReviewsFromProjectPaper } from "./review.ts";
import { Review } from "../../model/db/review.ts";
import { getUserID } from "../validation.controller.ts";
import { getPayloadFromJWTHeader } from "../validation.controller.ts";
import { Context } from "https://deno.land/x/oak/mod.ts";
import { concatWithoutDuplicates } from "../../helper/assign.ts";
import { getProjectStageStuff } from "../database.controller.ts";

export const getAllPapersFromStage = async (id: number): Promise<{ paper: Paper, scope: PaperScopeForStage, authors: Author[] }[]> => {
    let paperScopes = await PaperScopeForStage.where("stageId", id).get()

    console.log("new item: " + JSON.stringify((await getProjectStageStuff(id))))
    if (Array.isArray(paperScopes)) {
        let paperPromises: { paper: Paper, scope: PaperScopeForStage, authors: Author[] }[] = [];
        for (let item of paperScopes) {
            paperPromises.push({ paper: (await Paper.find(Number(item.paperId))) as Paper, scope: item, authors: await getAllAuthorsFromPaper(Number(item.paperId)) })
        }
        return paperPromises
    }
    return new Array<{ paper: Paper, scope: PaperScopeForStage, authors: Author[] }>();
}

export const getAllPaperMessagesJoin = async (id: number, ctx: Context) => {
    let userID = await getUserID(await getPayloadFromJWTHeader(ctx))
    let object = await PaperScopeForStage.where(PaperScopeForStage.field('stage_id'), id)
        .leftOuterJoin(Paper, Paper.field('id'), PaperScopeForStage.field('paper_id'))
        .leftOuterJoin(Pdf, Pdf.field('paper_id'), Paper.field('id'))
        .leftOuterJoin(Wrote, Wrote.field('paper_id'), Paper.field('id'))
        .leftOuterJoin(Author, Author.field('id'), Wrote.field('author_id'))
        .get();

    let paperMessage: PaperMessage[] = []
    let lastId = 0;
    if (Array.isArray(object)) {
        for (let item of object) {
            if (lastId == Number(item.paperId)) {
                let paper = paperMessage[paperMessage.length - 1]
                if (item.pdf) {
                    paper.pdf = concatWithoutDuplicates(paper.pdf, [String(item.pdf)])
                }
                if (item.authorId) {
                    if (!paper.authors.some(author => author.id == Number(item.authorId))) {
                        paper.authors.push({
                            id: item.authorId ? Number(item.authorId) : undefined,
                            firstName: item.firstName ? String(item.firstName) : undefined,
                            lastName: item.lastName ? String(item.lastName) : undefined,
                            rawString: item.rawString ? String(item.rawString) : undefined,
                            orcid: item.orcid ? String(item.orcid) : undefined,
                        })
                    }
                }

            } else {
                lastId = Number(item.paperId)
                let pp = await PaperScopeForStage.where({ paperId: Number(item.paperId) }).get()

                if (Array.isArray(pp)) {
                    let review = await getAllReviewsFromProjectPaper(Number(pp[0].id))
                    let author: AuthorMessage = {
                        id: item.authorId ? Number(item.authorId) : undefined,
                        firstName: item.firstName ? String(item.firstName) : undefined,
                        lastName: item.lastName ? String(item.lastName) : undefined,
                        rawString: item.rawString ? String(item.rawString) : undefined,
                        orcid: item.orcid ? String(item.orcid) : undefined,

                    }

                    let paper: PaperMessage = {
                        id: Number(item.paperId),
                        doi: item.doi ? String(item.doi) : undefined,
                        title: item.title ? String() : undefined,
                        abstract: item.abstract ? String() : undefined,
                        year: item.year ? Number(item.year) : undefined,
                        publisher: item.publisher ? String() : undefined,
                        type: item.type ? String() : undefined,
                        scope: item.scope ? String() : undefined,
                        scopeName: item.scopeName ? String() : undefined,
                        ppid: Number(pp[0].id),
                        finalDecision: pp[0].finalDecision ? String() : undefined,
                        authors: author.id ? [author] : [],
                        pdf: item.url ? [String(item.url)] : []
                    }

                    if (paperCache.has(String(paper.id))) {
                        paper.status = Status.unfinished
                    } else if (pp[0].finalDecision) {
                        paper.status = Status.completelyEvaluated
                    } else if (userID && review.some(item => Number(item.userId) == userID)) {
                        paper.status = Status.evaluatedByMyself
                    } else if (paper.ppid && pp[0].overallEvaluation) {
                        paper.status = Status.partiallyEvaluated
                    } else {
                        paper.status = Status.ready
                    }

                    paperMessage.push(paper)
                }
            }

        }
    }
    return paperMessage
}

export const getAllPapersFromProject = async (id: number): Promise<{ papers: { paper: Paper, scope: PaperScopeForStage, authors: Author[] }[], stage: Stage }[]> => {
    let stages = await getAllStagesFromProject(id)
    let allPapers: { papers: { paper: Paper, scope: PaperScopeForStage, authors: Author[] }[], stage: Stage }[] = []
    for (let stage of stages) {
        let papers = { papers: await getAllPapersFromStage(Number(stage.id)), stage: stage }

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