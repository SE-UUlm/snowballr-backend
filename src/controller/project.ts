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
import { getAllPapersFromStage } from "./databaseFetcher/paper.ts";
import { PapersMessage } from "../model/messages/papersMessage.ts";
import { PaperScopeForStage } from "../model/db/paperScopeForStage.ts";
import { convertIApiPaperToDBPaper, convertPapersToPaperMessage, convertPaperToPaperMessage } from "../helper/converter/paperConverter.ts";
import { assign } from "../helper/assign.ts"
import { startDoiFetch } from './fetch.ts';
import { IApiPaper } from "../api/iApiPaper.ts";

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
 * Adds a stage to a project
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
        if (!requestParameter.name) {
            makeErrorMessage(ctx, 422, "to add a stage, a name is needed")
            return;
        }


        const stages = await getAllStagesFromProject(id);
        let number = requestParameter.number ? requestParameter.number : 1
        if (!requestParameter.number) {
            for (const item of stages) {
                if (Number(item.number) >= number) {
                    number = Number(item.number) + 1;
                }
            }
        }
        let stage = await Stage.create({
            name: requestParameter.name,
            projectId: id,
            number: number
        })

        ctx.response.status = 201;
        ctx.response.body = JSON.stringify(stage)
    } else {
        makeErrorMessage(ctx, 401, "not authorized");
    }
}

/**
 * Adds a paper to a project stage
 *
 * @param ctx
 * @param projectId id of project
 * @param stageID id of stage
 */
export const addPaperToProjectStage = async (ctx: Context, projectId: number | undefined, stageID: number | undefined) => {
    if (!projectId || !stageID) {
        makeErrorMessage(ctx, 422, "no project and/or stage id included")
        return
    }
    const payloadJson = await getPayloadFromJWT(ctx);
    if (await checkAdmin(payloadJson) || await checkMemberOfProject(projectId, payloadJson)) {
        const requestParameter = await jsonBodyToObject(ctx)
        if (!requestParameter) {
            return
        }
        if (!requestParameter.doi && !requestParameter.title) {
            makeErrorMessage(ctx, 422, "to add a paper to a stage, at least a DOI or a title is needed")
            return;
        }
        if (requestParameter.doi) {
            await doiFetchToDB(requestParameter.doi)
        }
        //TODO check paper already exists
        /*
        let paper = await Paper.create({})

        assign(paper, requestParameter)
        paper.save()
        PaperScopeForStage.create({ paperId: Number(paper.id), stageId: stageID })
        */
        ctx.response.status = 201;
        //ctx.response.body = JSON.stringify(paper);
    } else {
        makeErrorMessage(ctx, 401, "not authorized");
    }
}

const doiFetchToDB = async (doi: string) => {
    let fetch = await startDoiFetch(doi);

    (await fetch.response).forEach(element => {
        if (element) {
            savePapers(element.paper)
            element.citations!.forEach(element => {
                savePapers(element)
            })
            element.references!.forEach(element => {
                savePapers(element)
            })
        }
    });


}

const savePapers = async (apiPaper: IApiPaper) => {

    let paper = await convertIApiPaperToDBPaper(apiPaper)
    if (paper) {
        console.error(`paper: ${paper.title}`)
        return paper;
    } else {
        console.error(`iapi: ${apiPaper.title}`)
    }
}
/**
 *
 * @param ctx
 * @param projectID
 * @param stageID
 */
export const getPapersOfProjectStage = async (ctx: Context, projectID: number | undefined, stageID: number | undefined) => {
    if (!projectID || !stageID) {
        makeErrorMessage(ctx, 422, "no project and/or stage id included")
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
        makeErrorMessage(ctx, 422, "no project and/or stage and/or no project paper id included")
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
        makeErrorMessage(ctx, 422, "no project and/or stage and/or no project paper id included")
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