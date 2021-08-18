import { Context } from "https://deno.land/x/mod.ts";
import { create, decode, verify } from "https://deno.land/x/djwt/mod.ts"
import { User } from "../model/db/user.ts";
import { createNumericTerminationDate } from "../helper/dateHelper.ts";
import { makeErrorMessage } from "../helper/error.ts";
import { PayloadJson } from "../model/payloadJson.ts";
import { UserIsPartOfProject } from "../model/db/userIsPartOfProject.ts";
import { jsonBodyToObject } from "../helper/body.ts";

const SECRET = String(Deno.env.get('SECRET'));
const KEY = await crypto.subtle.generateKey(
    { name: "HMAC", hash: "SHA-512" },
    true,
    ["sign", "verify"],
);
/**
 * Validates, whether the provided request has either an empty content or a right set content type with valid json.
 *
 * @param ctx
 * @param next function to start next, if the validation is successful
 */
export const validateContentType = async (ctx: Context, next: () => Promise<unknown>) => {
    ctx.response.type = "application/json";
    let contentType = ctx.request.headers.get("Content-Type");
    if (await emptyContent(ctx)) {
        await next();
    } else if (contentType && contentType.startsWith("application/json")) {
        if (await validateContent(ctx)) {
            await next();
        } else {
            makeErrorMessage(ctx, 415, "The requestBody is not in a correct JSON format")
        }
    } else {
        makeErrorMessage(ctx, 415, "Only application/json is allowed")
    }

}

/**
 * Checks whether the body of a function is empty or not
 * @param ctx
 */
const emptyContent = async (ctx: Context): Promise<boolean> => {
    try {
        await ctx.request.body({ type: "undefined" }).value
        return true;
    } catch (error) {
        return false;
    }
}

/**
 * Validates, if the content can be parsed to json
 * @param ctx
 * @param next
 */
const validateContent = async (ctx: Context): Promise<boolean> => {
    try {
        await ctx.request.body({ type: "json" }).value
    } catch (error) {
        return false;
    }
    return true;
}

/**
 * Checks whether a JWT has been set and if yes, forwards it to verify it's correctness.
 * Otherwise it checks whether the provided url is allowed
 *
 * @param ctx
 * @param next
 */
export const validateJWTIfExists = async (ctx: Context, next: () => Promise<unknown>) => {
    let token = ctx.request.headers.get("authenticationToken");
    if (token) {
        return verifyJWT(ctx, next, token)
    } else {
        return allowedAddressesUnauthorized(ctx, next)
    }
}

/**
 * Creates a JWT and adds the userprofile in it
 * @param user
 */
export const createJWT = async (user: User) => {
    return create({ alg: "HS512", typ: "JWT" }, {
        id: user.id,
        eMail: user.email,
        isAdmin: user.isAdmin,
        lastName: user.lastName,
        firstName: user.firstName,
        status: user.status,
        exp: createNumericTerminationDate()
    }, KEY);
}

/**
 * Extracts the userName from a JWT payload
 * @param payloadJson
 */
export const getUserName = async (payloadJson?: PayloadJson) => {
    if (payloadJson) {
        let name = payloadJson.firstName;
        if (payloadJson.lastName) {
            name += " " + payloadJson.lastName;
        }
        return name;
    }
}

/**
 * Checks the admin status from a JWT payload
 * @param payloadJson
 */
export const checkAdmin = async (payloadJson?: PayloadJson) => {
    if (payloadJson) {
        return payloadJson.isAdmin;
    }
}

/**
 * Checks the PO status from a JWT payload
 * @param payloadJson
 */
export const checkPO = async (payloadJson?: PayloadJson) => {
    if (payloadJson) {
        let projects = await User.where('id', payloadJson.id).project();
        if (Array.isArray(projects)) {
            return !(projects.every((userIsPartOfProject) => {
                return !userIsPartOfProject.isOwner;
            }))
        }

    }
    return false;
}

/**
 * Checks if the user making the request is the PO of a project
 * @param projectID 
 * @param payloadJson 
 * @returns 
 */
export const checkPOofProject = async (projectID: number, payloadJson?: PayloadJson) => {
    if (payloadJson) {
        let userProject = await UserIsPartOfProject.where({ userId: payloadJson.id, projectId: projectID }).get()
        if (Array.isArray(userProject)) {
            let value: boolean = Boolean(userProject[0].isOwner)
            return value
        }

    }
    return false;
}

/**
 * Checks if the user making the request is a member of the corresponding project
 * @param projectID 
 * @param payloadJson 
 * @returns 
 */
export const checkMemberOfProject = async (projectID: number, payloadJson?: PayloadJson) => {
    if (payloadJson) {
        let userProject = await UserIsPartOfProject.where({ userId: payloadJson.id, projectId: projectID }).get()


        if (Array.isArray(userProject) && userProject[0]) {
            return true;
        }

    }
    return false;
}

/**
 * Checks whether a user is active.
 * @param status
 */
export const checkActive = (status: string) => {
    if (status === "active") {
        return true;
    }
    return false;
}

/**
 * Extracts the userID from a JWT payload
 * @param payloadJson
 */
export const getUserID = async (payloadJson?: PayloadJson) => {
    if (payloadJson) {
        return payloadJson.id
    }
}

/**
 * Returns the payload from a JWT, if it exists
 * @param ctx
 */
export const getPayloadFromJWT = async (ctx: Context): Promise<PayloadJson | undefined> => {
    let token = await ctx.request.headers.get("authenticationToken");
    if (token) {
        let [, payload,] = await decode(token)
        return <PayloadJson>payload;
    }
}

/**
 * Verifies a token if it exists and isn't expired yet
 * @param ctx
 * @param next function to trigger if the token is valid
 * @param token
 */
const verifyJWT = async (ctx: Context, next: () => Promise<unknown>, token: string) => {
    let goingForward = true;
    await verify(token, KEY).catch((err) => {
        goingForward = false;
        makeErrorMessage(ctx, 401, "token expired")
    });
    if (goingForward) {
        await next();
    }
}

/**
 * Checks whether an unverified user is allowed to enter that path
 * @param ctx
 * @param next function to trigger if the path is allowed for unverified users
 */
const allowedAddressesUnauthorized = async (ctx: Context, next: () => Promise<unknown>) => {
    if (ctx.request.url.pathname === "/login/" || ctx.request.url.pathname === "/reset-password/" || (ctx.request.url.pathname.match(/\/users\/[0-9]+\//g) && ctx.request.method.toString() === "PATCH")) {
        ctx.response.status = 200;
        await next();
    } else {
        makeErrorMessage(ctx, 401, "not authorized")
    }
}

export const validateUserEntry = async (ctx: Context, id: (number | undefined)[], needed: UserStatus, projectID: number, requestParameter: { needed: boolean, params: string[] }, userID?: number) => {
    for (let element in id) {
        if (!Number(element) && Number(element) !== 0) {
            console.error("path id wrong for" + element)
            makeErrorMessage(ctx, 422, "path ids must be numbers");
            return;
        }
    }

    if (! await checkAuthorization(ctx, needed, projectID, userID)) {
        return
    }

    if (requestParameter.needed) {
        const params = await jsonBodyToObject(ctx)
        if (!params) {
            return
        }
        for (let param of requestParameter.params) {
            if (!params[param]) {
                console.error(`Request doesn't include parameter ${param}`)
                makeErrorMessage(ctx, 422, `Request doesn't include parameter ${param}`)
                return;
            }
        }

        return params
    }
    return true;

}

const checkAuthorization = async (ctx: Context, needed: UserStatus, projectID: number, userID?: number) => {
    const payloadJson = await getPayloadFromJWT(ctx);
    let isAdmin = await checkAdmin(payloadJson)
    if (needed === UserStatus.needsAdmin) {
        if (!isAdmin) {
            makeErrorMessage(ctx, 401, "not authorized");
            return
        }
    }
    if (needed === UserStatus.needsPO) {
        if (!isAdmin && !await checkPO(payloadJson)) {
            makeErrorMessage(ctx, 401, "not authorized");
            return
        }
    }
    if (needed === UserStatus.needsPOOfProject) {
        if (!isAdmin && !await checkPOofProject(projectID, payloadJson)) {
            makeErrorMessage(ctx, 401, "not authorized");
            return
        }
    }
    if (needed === UserStatus.needsMemberOfProject) {
        if (!isAdmin && !await checkMemberOfProject(projectID, payloadJson)) {
            makeErrorMessage(ctx, 401, "not authorized");
            return
        }
    }

    if (needed === UserStatus.needsSameMemberOfProject) {
        if (!isAdmin && !(await checkMemberOfProject(projectID, payloadJson) && (await getUserID(payloadJson) === userID))) {
            makeErrorMessage(ctx, 401, "not authorized");
            return
        }
    }

    if (needed === UserStatus.needsSameUserOrPO) {
        if (!isAdmin && !(await getUserID(payloadJson) === userID) && !(await checkPO(payloadJson))) {
            makeErrorMessage(ctx, 401, "not authorized");
            return
        }
    }
    if (needed === UserStatus.needsSameUser) {
        if (!isAdmin && !(await getUserID(payloadJson) === userID)) {
            makeErrorMessage(ctx, 401, "not authorized");
            return
        }
    }
    return true;
}

export enum UserStatus {
    needsAdmin,
    needsPO,
    needsPOOfProject,
    needsMemberOfProject,
    needsSameMemberOfProject,
    needsSameUserOrPO,
    needsSameUser,
    none

}
