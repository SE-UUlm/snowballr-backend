import { Context } from 'https://deno.land/x/oak/mod.ts';
import { makeErrorMessage } from "../helper/error.ts";
import { jsonBodyToObject } from "../helper/body.ts";
import { Project } from "../model/db/project.ts";
import { UserIsPartOfProject } from "../model/db/userIsPartOfProject.ts";
import { ProjectMembersMessage } from "../model/messages/projectMembers.message.ts";
import { getAllMembersOfProject } from "./databaseFetcher/userProject.ts";
import { convertProjectToProjectMessage } from "../helper/converter/projectConverter.ts";
import { Stage } from "../model/db/stage.ts";
import { Paper } from "../model/db/paper.ts";
import { getAllStagesFromProject } from "./databaseFetcher/stage.ts";
import { getAllPapersFromStage, getPaperByDoi } from "./databaseFetcher/paper.ts";
import { PapersMessage } from "../model/messages/papersMessage.ts";
import { PaperScopeForStage } from "../model/db/paperScopeForStage.ts";
import { assignOnlyIfUnassignedPaper, checkIApiPaper, convertIApiPaperToDBPaper, convertPapersToPaperMessage, convertPaperToPaperMessage } from "../helper/converter/paperConverter.ts";
import { assign } from "../helper/assign.ts"
import { IApiPaper } from "../api/iApiPaper.ts";
import { getDOI } from "../api/apiMerger.ts";
import { Cache, CacheType } from "../api/cache.ts";
import { logger } from "../api/logger.ts";
import { IApiAuthor } from "../api/iApiAuthor.ts";
import { checkAdmin, checkMemberOfProject, checkPO, checkPOofProject, getPayloadFromJWT, UserStatus, validateUserEntry } from "./validation.controller.ts";
import { makeFetching } from "./fetch.controller.ts";
import { saveChildren } from "./database.controller.ts";

export const paperCache = new Cache<IApiPaper>(CacheType.F, 0, "paperCache")
export const authorCache = new Cache<IApiAuthor>(CacheType.F, 0, "authorCache")
/**
 * Creates a project
 *
 * @param ctx
 */
export const createProject = async (ctx: Context) => {
    let validate = await validateUserEntry(ctx, [], UserStatus.needsPO, -1, { needed: true, params: ["name", "minCountReviewers", "countDecisiveReviewers"] })
    if (!validate) {
        return
    }

    let project = await Project.create({
        name: validate.name,
        minCountReviewers: validate.minCountReviewers,
        countDecisiveReviewers: validate.countDecisiveReviewers
    })
    if (validate.evaluationFormula) {
        project.evaluationFormula = validate.evaluationFormula;
        await project.update();
    }

    ctx.response.status = 201;
    ctx.response.body = JSON.stringify(project)
}



/**
 * Adds a person to a project
 *
 * @param ctx
 * @param id id of project
 */
export const addMemberToProject = async (ctx: Context, id: number) => {
    let validate: any = await validateUserEntry(ctx, [id], UserStatus.needsPOOfProject, id, { needed: true, params: ["id"] })
    if (!validate) {
        return
    }
    const params = await jsonBodyToObject(ctx)
    await UserIsPartOfProject.create({
        isOwner: params.isOwner ? params.isOwner : false,
        userId: validate.id,
        projectId: id
    })

    ctx.response.status = 201;

}

export const getMembersOfProject = async (ctx: Context, id: number) => {
    let validate = await validateUserEntry(ctx, [id], UserStatus.needsMemberOfProject, id, { needed: false, params: [] })
    if (!validate) {
        return
    }
    ctx.response.status = 200;
    let message: ProjectMembersMessage = { members: await getAllMembersOfProject(id) }
    ctx.response.body = JSON.stringify(message)

}

export const getProjects = async (ctx: Context) => {
    const payloadJson = await getPayloadFromJWT(ctx);
    if (await checkAdmin(payloadJson)) {
        let projects = await Project.all();
        let projectMessage = await convertProjectToProjectMessage(projects);
        ctx.response.status = 200;
        ctx.response.body = JSON.stringify(projectMessage)
    } else {
        makeErrorMessage(ctx, 401, "not authorized");
    }
}

/**
 * Adds a nextStage to a project
 *
 * @param ctx
 * @param id id of project
 */
export const addStageToProject = async (ctx: Context, id: number) => {
    let validate = await validateUserEntry(ctx, [id], UserStatus.needsPOOfProject, id, { needed: false, params: [] })
    if (!validate) {
        return
    }
    const requestParameter = await jsonBodyToObject(ctx)
    const stages = await getAllStagesFromProject(id);

    let stage = await Stage.create({
        name: requestParameter.name ? requestParameter.name : `Stage ${stages.length}`,
        projectId: id,
        number: stages.length
    })

    ctx.response.status = 201;
    ctx.response.body = JSON.stringify(stage)

}

/**
 * Adds a paper to a project nextStage
 *
 * @param ctx
 * @param projectID id of project
 * @param stageID id of nextStage
 */
export const addPaperToProjectStage = async (ctx: Context, projectID: number, stageID: number, awaitFetch?: boolean) => {
    let validate = await validateUserEntry(ctx, [projectID, stageID], UserStatus.needsMemberOfProject, projectID, { needed: false, params: [] })
    if (!validate) {
        return
    }

    const requestParameter = await jsonBodyToObject(ctx)
    if (!requestParameter.doi && !requestParameter.title) {
        makeErrorMessage(ctx, 422, "to add a paper to a nextStage, at least a DOI or a title is needed")
        return;
    }

    if (awaitFetch) {
        await fetchToDB(stageID, projectID, requestParameter.doi, requestParameter.title, requestParameter.author)
    } else {
        fetchToDB(stageID, projectID, requestParameter.doi, requestParameter.title, requestParameter.author)
    }
    ctx.response.status = 201;

}

const fetchToDB = async (stageID: number, projectID: number, doi?: string, title?: string, authorName?: string) => {

    let fetch = await makeFetching(doi, title, authorName);
    let response = (await fetch.response)

    for (let element of response) {
        if (element) {
            let parent = await savePaper(element.paper)

            PaperScopeForStage.create({ stageId: stageID, paperId: Number(parent.id) })
            let currentStage = await Stage.find(stageID)
            let stages = (await getAllStagesFromProject(projectID))
            let nextStage: Stage = stages.filter((item: Stage) => Number(item.number) == Number(currentStage.number) + 1)[0];
            if (!nextStage) {
                nextStage = await Stage.create({
                    name: `Stage ${stages.length}`,
                    projectId: projectID,
                    number: stages.length
                })
            }

            let allChildren: Promise<Paper>[] = []
            for (let item of element.citations!) {
                allChildren.push(createChildren(item, "citedBy", "papercitedid", "papercitingid", Number(parent.id), Number(nextStage.id)))
            }
            for (let item of element.references!) {
                allChildren.push(createChildren(item, "referencedby", "paperreferencedid", "paperreferencingid", Number(parent.id), Number(nextStage.id)))
            }
            await Promise.all(allChildren)
        }

    }

}

const createChildren = async (item: IApiPaper, into: string, column1: string, column2: string, firstId: number, nextStageId: number) => {
    let child = await savePaper(item)
    await saveChildren(into, column1, column2, firstId, Number(child.id))
    await PaperScopeForStage.create({ stageId: Number(nextStageId), paperId: Number(child.id) })
    return child;
}



const savePaper = async (apiPaper: IApiPaper): Promise<Paper> => {
    let doi = getDOI(apiPaper)

    if (doi[0]) {
        let dbPaper = await getPaperByDoi(doi[0])

        if (dbPaper) {
            await assignOnlyIfUnassignedPaper(dbPaper, apiPaper)
            return dbPaper.update()
        }

    }
    let paper = await convertIApiPaperToDBPaper(apiPaper)

    if (!checkIApiPaper(apiPaper)) {
        paperCache.add(String(paper.id), (apiPaper))
    }
    return paper;

}
/**
 *
 * @param ctx
 * @param projectID
 * @param stageID
 */
export const getPapersOfProjectStage = async (ctx: Context, projectID: number, stageID: number) => {
    let validate = await validateUserEntry(ctx, [projectID, stageID], UserStatus.needsMemberOfProject, projectID, { needed: false, params: [] })
    if (!validate) {
        return
    }

    ctx.response.status = 200;
    let message: PapersMessage = { papers: await convertPapersToPaperMessage(await getAllPapersFromStage(stageID), stageID) }
    ctx.response.body = JSON.stringify(message)

}

/**
 * Gets a single paper by using the project specific paper id (not the normal id of a paper!)
 *
 * @param ctx
 * @param projectID
 * @param stageID
 * @param ppID project specific paper id
 */
export const getPaperOfProjectStage = async (ctx: Context, projectID: number, stageID: number, ppID: number) => {
    let validate = await validateUserEntry(ctx, [projectID, stageID], UserStatus.needsMemberOfProject, projectID, { needed: false, params: [] })
    if (!validate) {
        return
    }

    try {
        let paper = await PaperScopeForStage.where("id", ppID).paper();
        if (paper) {
            ctx.response.status = 200;
            ctx.response.body = JSON.stringify(await convertPaperToPaperMessage(paper, stageID));
        }
    } catch (e) {
        makeErrorMessage(ctx, 404, "paper does not exist")
    }

}

export const patchPaperOfProjectStage = async (ctx: Context, projectID: number, stageID: number, ppID: number) => {
    let validate = await validateUserEntry(ctx, [projectID, stageID], UserStatus.needsMemberOfProject, projectID, { needed: false, params: [] })
    if (!validate) {
        return
    }

    try {
        let paper = await PaperScopeForStage.where("id", ppID).paper();
        if (paper) {
            let bodyJson = await jsonBodyToObject(ctx);
            if (!bodyJson) {
                return
            }
            assign(paper, bodyJson);
            await paper.update()
            ctx.response.status = 200;
            ctx.response.body = JSON.stringify(await convertPaperToPaperMessage(paper, stageID))
        }
    } catch (e) {
        makeErrorMessage(ctx, 404, "paper does not exist")
    }


}
