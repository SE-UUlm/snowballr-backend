import {Context} from 'https://deno.land/x/oak/mod.ts';
import {checkAdmin, checkPO, createJWT, getPayloadFromJWT, getUserID, getUserName} from "./validation.ts";
import {insertUserForRegistration, returnUserByEmail} from "./databaseFetcher/user.ts";
import {User} from "../model/db/user.ts";
import {convertCtxBodyToUser, convertUserToUserProfile} from "../helper/converter/userConverter.ts";
import {EMailClient} from "../model/eMailClient.ts";
import {makeErrorMessage} from "../helper/error.ts";
import {urlSanitizer} from "../helper/url.ts";
import {jsonBodyToObject} from "../helper/body.ts";
import {getInvitation, insertInvitation} from "./databaseFetcher/invitation.ts";
import {getResetToken, insertResetToken} from "./databaseFetcher/resetToken.ts";
import {getAllMembersOfProject, getAllProjectsByUser} from "./databaseFetcher/userProject.ts";
import {hashPassword} from "../helper/passwordHasher.ts";
import {UserParameters} from "../model/userProfile.ts";
import {convertProjectToProjectMessage} from "../helper/converter/projectConverter.ts";
import {UsersMessage} from "../model/messages/user.message.ts";
import {Project} from "../model/db/project.ts";
import {UserIsPartOfProject} from "../model/db/userIsPartOfProject.ts";
import {ProjectMembersMessage} from "../model/messages/projectMembers.message.ts";

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

        let project = await Project.create({name: requestParameter.name,minCountReviewers: requestParameter.minCountReviewers, countDecisiveReviewers: requestParameter.countDecisiveReviewers})
        if(requestParameter.evaluationFormula){
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
            isOwner: requestParameter.isOwner? requestParameter.isOwner: false,
            userId: requestParameter.id,
            projectId: id
        })
        ctx.response.status = 200;
    }else {
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
    }else {
            makeErrorMessage(ctx, 401, "not authorized");
        }
    }