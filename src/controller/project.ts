import { Context } from 'https://deno.land/x/oak/mod.ts';
import { checkAdmin, checkMemberOfProject, checkPO, checkPOofProject, getPayloadFromJWT } from "./validation.ts";
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
import { Batcher, makeFetching } from './fetch.ts';
import { IApiPaper } from "../api/iApiPaper.ts";
import { getDOI } from "../api/apiMerger.ts";
import { client } from "./database.ts";
import { Cache } from "../api/cache.ts";
import { logger } from "../api/logger.ts";

export const paperCache = new Cache<IApiPaper>(false, true, undefined, undefined, "paperCache")
/**
 * Creates a project
 *
 * @param ctx
 */
export const createProject = async (ctx: Context) => {
    const payloadJson = await getPayloadFromJWT(ctx);
    if (await checkAdmin(payloadJson) || await checkPO(payloadJson)) {
        const requestParameter = await jsonBodyToObject(ctx)
        if (!requestParameter) {
            return
        }

        if (!requestParameter.name || !requestParameter.minCountReviewers || !requestParameter.countDecisiveReviewers) {
            makeErrorMessage(ctx, 422, "A project must have a name, minCountReviewers and countDecisiveReviewers")
            return;
        }

        let project = await Project.create({
            name: requestParameter.name,
            minCountReviewers: requestParameter.minCountReviewers,
            countDecisiveReviewers: requestParameter.countDecisiveReviewers
        })
        if (requestParameter.evaluationFormula) {
            project.evaluationFormula = requestParameter.evaluationFormula;
            await project.update();
        }

        ctx.response.status = 201;
        ctx.response.body = JSON.stringify(project)
    } else {
        makeErrorMessage(ctx, 401, "not authorized")
    }
}


/**
 * Adds a person to a project
 *
 * @param ctx
 * @param id id of project
 */
export const addMemberToProject = async (ctx: Context, id: number | undefined) => {
    if (!id) {
        makeErrorMessage(ctx, 422, "no project id included")
        return
    }

    const payloadJson = await getPayloadFromJWT(ctx);
    if (await checkAdmin(payloadJson) || await checkPOofProject(id, payloadJson)) {
        const requestParameter = await jsonBodyToObject(ctx)
        if (!requestParameter) {
            return
        }
        if (!requestParameter.id) {
            makeErrorMessage(ctx, 422, "to add a member, a userid is needed")
            return;
        }
        await UserIsPartOfProject.create({
            isOwner: requestParameter.isOwner ? requestParameter.isOwner : false,
            userId: requestParameter.id,
            projectId: id
        })
        ctx.response.status = 201;
    } else {
        makeErrorMessage(ctx, 401, "not authorized");
    }
}

export const getMembersOfProject = async (ctx: Context, id: number | undefined) => {
    if (!id) {
        makeErrorMessage(ctx, 422, "no project id included")
        return
    }

    const payloadJson = await getPayloadFromJWT(ctx);
    if (await checkAdmin(payloadJson) || await checkMemberOfProject(id, payloadJson)) {
        ctx.response.status = 200;
        let message: ProjectMembersMessage = { members: await getAllMembersOfProject(id) }
        ctx.response.body = JSON.stringify(message)
    } else {
        makeErrorMessage(ctx, 401, "not authorized");
    }
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
export const addStageToProject = async (ctx: Context, id: number | undefined) => {
    if (!id) {
        makeErrorMessage(ctx, 422, "no project id included")
        return
    }

    const payloadJson = await getPayloadFromJWT(ctx);
    if (await checkAdmin(payloadJson) || await checkPOofProject(id, payloadJson)) {
        const requestParameter = await jsonBodyToObject(ctx)
        if (!requestParameter) {
            return
        }

        const stages = await getAllStagesFromProject(id);

        let stage = await Stage.create({
            name: requestParameter.name ? requestParameter.name : `Stage ${stages.length}`,
            projectId: id,
            number: stages.length
        })

        ctx.response.status = 201;
        ctx.response.body = JSON.stringify(stage)
    } else {
        makeErrorMessage(ctx, 401, "not authorized");
    }
}

/**
 * Adds a paper to a project nextStage
 *
 * @param ctx
 * @param projectId id of project
 * @param stageID id of nextStage
 */
export const addPaperToProjectStage = async (ctx: Context, projectId: number | undefined, stageID: number | undefined, awaitFetch?: boolean) => {
    if (!projectId || !stageID) {
        makeErrorMessage(ctx, 422, "no project and/or nextStage id included")
        return
    }
    const payloadJson = await getPayloadFromJWT(ctx);
    if (await checkAdmin(payloadJson) || await checkMemberOfProject(projectId, payloadJson)) {
        const requestParameter = await jsonBodyToObject(ctx)
        if (!requestParameter) {
            return
        }
        if (!requestParameter.doi && !requestParameter.title) {
            makeErrorMessage(ctx, 422, "to add a paper to a nextStage, at least a DOI or a title is needed")
            return;
        }

        if (awaitFetch) {
            await fetchToDB(stageID, projectId, requestParameter.doi, requestParameter.title, requestParameter.author)
        } else {
            fetchToDB(stageID, projectId, requestParameter.doi, requestParameter.title, requestParameter.author)
        }


        ctx.response.status = 200;

    } else {
        makeErrorMessage(ctx, 401, "not authorized");
    }
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
            let nextStage: Stage = stages.filter((item: Stage) => item.number === currentStage.number)[0];
            if (!nextStage) {
                nextStage = await Stage.create({
                    name: `Stage ${stages.length}`,
                    projectId: projectID,
                    number: stages.length
                })
            }

            //TODO async for fastness
            for (let item of element.citations!) {
                let child = await savePaper(item)
                await saveChildren("citedBy", "papercitedid", "papercitingid", Number(parent.id), Number(child.id))
                await PaperScopeForStage.create({ stageId: Number(nextStage.id), paperId: Number(child.id) })
            }
            for (let item of element.references!) {
                let child = await savePaper(item)
                await saveChildren("referencedby", "paperreferencedid", "paperreferencingid", Number(parent.id), Number(child.id))
                await PaperScopeForStage.create({ stageId: Number(nextStage.id), paperId: Number(child.id) })
            }

        }

    }

}

const saveChildren = async (into: string, column1: string, column2: string, firstId: number, secondId: number) => {
    await client.queryArray(`insert into ${into}(${column1}, ${column2})
                        VALUES (${firstId}, ${secondId})`);
}

const savePaper = async (apiPaper: IApiPaper): Promise<Paper> => {
    let doi = getDOI(apiPaper)

    if (doi[0]) {
        let dbPaper = await getPaperByDoi(doi[0])

        if (dbPaper) {
            assignOnlyIfUnassignedPaper(dbPaper, apiPaper)
            return dbPaper.update()
        }

    }
    let paper = await convertIApiPaperToDBPaper(apiPaper)

    if (!checkIApiPaper(apiPaper)) {
        let id = Number(paper.id);
        paperCache.add(String(id), apiPaper)
    }
    return paper;

}
/**
 *
 * @param ctx
 * @param projectID
 * @param stageID
 */
export const getPapersOfProjectStage = async (ctx: Context, projectID: number | undefined, stageID: number | undefined) => {
    if (!projectID || !stageID) {
        makeErrorMessage(ctx, 422, "no project and/or nextStage id included")
        return
    }

    const payloadJson = await getPayloadFromJWT(ctx);
    if (await checkAdmin(payloadJson) || await checkMemberOfProject(projectID, payloadJson)) {
        ctx.response.status = 200;
        let message: PapersMessage = { papers: await convertPapersToPaperMessage(await getAllPapersFromStage(stageID), stageID) }
        ctx.response.body = JSON.stringify(message)
    } else {
        makeErrorMessage(ctx, 401, "not authorized");
    }
}

/**
 * Gets a single paper by using the project specific paper id (not the normal id of a paper!)
 *
 * @param ctx
 * @param projectID
 * @param stageID
 * @param ppID project specific paper id
 */
export const getPaperOfProjectStage = async (ctx: Context, projectID: number | undefined, stageID: number | undefined, ppID: number | undefined) => {
    if (!projectID || !stageID || !ppID) {
        makeErrorMessage(ctx, 422, "no project and/or nextStage and/or no project paper id included")
        return
    }

    const payloadJson = await getPayloadFromJWT(ctx);
    if (await checkAdmin(payloadJson) || await checkMemberOfProject(projectID, payloadJson)) {
        let paper = await PaperScopeForStage.where("id", ppID).paper();
        if (paper) {
            ctx.response.status = 200;
            ctx.response.body = JSON.stringify(await convertPaperToPaperMessage(paper, stageID));
        } else {
            makeErrorMessage(ctx, 404, "paper does not exist")
        }
    } else {
        makeErrorMessage(ctx, 401, "not authorized");
    }
}

export const patchPaperOfProjectStage = async (ctx: Context, projectID: number | undefined, stageID: number | undefined, ppID: number | undefined) => {
    if (!projectID || !stageID || !ppID) {
        makeErrorMessage(ctx, 422, "no project and/or nextStage and/or no project paper id included")
        return
    }

    const payloadJson = await getPayloadFromJWT(ctx);
    if (await checkAdmin(payloadJson) || await checkMemberOfProject(projectID, payloadJson)) {
        let paper = await PaperScopeForStage.where("id", ppID).paper();
        if (paper) {
            let bodyJson = jsonBodyToObject(ctx);
            if (!bodyJson) {
                return
            }
            assign(paper, bodyJson);
            await paper.update()
            ctx.response.status = 200;
            ctx.response.body = JSON.stringify(await convertPaperToPaperMessage(paper, stageID))
        } else {
            makeErrorMessage(ctx, 404, "paper does not exist")
        }

    } else {
        makeErrorMessage(ctx, 401, "not authorized");
    }
}
