import {Context} from 'https://deno.land/x/oak/mod.ts';
import {checkAdmin, checkMemberOfProject, checkPO, checkPOofProject, getPayloadFromJWT} from "./validation.ts";
import {makeErrorMessage} from "../helper/error.ts";
import {jsonBodyToObject} from "../helper/body.ts";
import {Project} from "../model/db/project.ts";
import {UserIsPartOfProject} from "../model/db/userIsPartOfProject.ts";
import {ProjectMembersMessage} from "../model/messages/projectMembers.message.ts";
import {getAllMembersOfProject} from "./databaseFetcher/userProject.ts";
import {convertProjectToProjectMessage} from "../helper/converter/projectConverter.ts";
import {Stage} from "../model/db/stage.ts";
import {Paper} from "../model/db/paper.ts";

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
        let message: ProjectMembersMessage = {members: await getAllMembersOfProject(id)}
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
        let stage = await Stage.create({
            name: requestParameter.name,
            projectId: id
        })
        ctx.response.status = 201;
        ctx.response.body = JSON.stringify(stage)
    } else {
        makeErrorMessage(ctx, 401, "not authorized");
    }
}

/**
 * Adds a person to a project
 *
 * @param ctx
 * @param projectId id of project
 */
export const addPaperToProjectStage = async (ctx: Context, projectId: number | undefined, stageID: number | undefined) => {
    if (!projectId || !stageID) {
        makeErrorMessage(ctx, 422, "no project and/or paper id included")
        return
    }
    const payloadJson = await getPayloadFromJWT(ctx);
    if (await checkAdmin(payloadJson) || await checkPOofProject(projectId, payloadJson)) {
        const requestParameter = await jsonBodyToObject(ctx)
        if (!requestParameter) {
            return
        }
        if (!requestParameter.doi || !requestParameter.title) {
            makeErrorMessage(ctx, 422, "to add a paper to a stage, at least a DOI or a title is needed")
            return;
        }
        await Paper.create({
            isOwner: requestParameter.isOwner ? requestParameter.isOwner : false,
            userId: requestParameter.id,
            projectId: projectId
        })
        ctx.response.status = 201;
    } else {
        makeErrorMessage(ctx, 401, "not authorized");
    }
}

export const getPapersOfProjectStage = async (ctx: Context, projectID: number | undefined, stageID: number | undefined) => {
    if (!projectID || !stageID) {
        makeErrorMessage(ctx, 422, "no project and/or paper id included")
        return
    }

    const payloadJson = await getPayloadFromJWT(ctx);
    if (await checkAdmin(payloadJson) || await checkMemberOfProject(projectID, payloadJson)) {
        ctx.response.status = 200;
        let message: ProjectMembersMessage = {members: await getAllMembersOfProject(projectID)}
        ctx.response.body = JSON.stringify(message)
    } else {
        makeErrorMessage(ctx, 401, "not authorized");
    }
}