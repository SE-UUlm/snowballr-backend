import {Context} from 'https://deno.land/x/oak/mod.ts';
import {checkAdmin, checkPO, getPayloadFromJWT} from "./validation.ts";
import {makeErrorMessage} from "../helper/error.ts";
import {jsonBodyToObject} from "../helper/body.ts";
import {Project} from "../model/db/project.ts";
import {UserIsPartOfProject} from "../model/db/userIsPartOfProject.ts";
import {ProjectMembersMessage} from "../model/messages/projectMembers.message.ts";
import {getAllMembersOfProject} from "./databaseFetcher/userProject.ts";
import {convertProjectToProjectMessage} from "../helper/converter/projectConverter.ts";

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

        ctx.response.status = 200;
        ctx.response.body = JSON.stringify(project)
    } else {
        makeErrorMessage(ctx, 401, "not authorized")
    }
}


//TODO isPO check
/**
 * Adds a person to a project
 *
 * @param ctx
 * @id id of project
 */
export const addPersonToProject = async (ctx: Context, id: number | undefined) => {
    if (!id) {
        makeErrorMessage(ctx, 400, "no project id included")
        return
    }

    const payloadJson = await getPayloadFromJWT(ctx);
    if (await checkAdmin(payloadJson) || await checkPO(payloadJson)) {
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
        ctx.response.status = 200;
    } else {
        makeErrorMessage(ctx, 401, "not authorized");
    }
}

//TODO same group && is Po raelly PO of project
export const getMembersOfProject = async (ctx: Context, id: number | undefined) => {
    if (!id) {
        makeErrorMessage(ctx, 400, "no project id included")
        return
    }

    const payloadJson = await getPayloadFromJWT(ctx);
    if (await checkAdmin(payloadJson) || await checkPO(payloadJson)) {
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