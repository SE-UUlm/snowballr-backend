import {Context} from 'https://deno.land/x/oak/mod.ts';
import {create, decode, verify} from "https://deno.land/x/djwt@/mod.ts"
import {User} from "../model/db/user.ts";
import {createNumericTerminationDate} from "../helper/dateHelper.ts";
import {makeErrorMessage} from "../helper/error.ts";

const SECRET = String(Deno.env.get('SECRET'));

export const validateContentType = async (ctx: Context, next: () => Promise<unknown>) => {
    ctx.response.type = "application/json";
    if (await emptyContent(ctx)) {
        await next();
    } else if (ctx.request.headers.get("Content-Type") === "application/json") {
        if (await validateContent(ctx, next)) {
            await next();
        } else {
            makeErrorMessage(ctx, 415, "The requestBody is not in a correct JSON format")
        }
    } else {
        makeErrorMessage(ctx, 415, "Only application/json is allowed")
    }

}
const emptyContent = async (ctx: Context): Promise<boolean> => {
    try {
        await ctx.request.body({type: "undefined"}).value
        return true;
    } catch (error) {
        return false;
    }
}
const validateContent = async (ctx: Context, next: () => Promise<unknown>): Promise<boolean> => {
    try {
        await ctx.request.body({type: "json"}).value
    } catch (error) {
        return false;
    }
    return true;
}

export const validateJWTIfExists = async (ctx: Context, next: () => Promise<unknown>) => {
    let token = ctx.cookies.get("token");
    if (token) {
        return verifyJWT(ctx, next, token)
    } else {
        return allowLogin(ctx, next)
    }
}

export const createJWT = async (user: User) => {
    return await create({alg: "HS512", typ: "JWT"}, {
        id: user.id,
        eMail: user.email,
        isAdmin: user.isAdmin,
        lastName: user.lastName,
        firstName: user.firstName,
        status: user.status,
        exp: createNumericTerminationDate()
    }, SECRET);
}

export const checkAdmin = async (ctx: Context) => {
    const payloadJson = await getPayloadFromJWT(ctx);
    if (payloadJson && payloadJson.isAdmin && payloadJson.status) {
        if (payloadJson.status === "unregistered" || payloadJson.status === "deleted") {
            return false;
        }
        return payloadJson.isAdmin;
    }

    return false;
}

export const getUserID = async (ctx: Context) => {
    const payloadJson = await getPayloadFromJWT(ctx);
    if (payloadJson && payloadJson.id) {
        return payloadJson.id
    }
    return undefined;
}

export const getPayloadFromJWT = async (ctx: Context) => {
    let token = await ctx.cookies.get("token");
    if (token) {
        const [signature, payload, header] = await decode(token)
        return payload as any;
    }

    return undefined;
}

const verifyJWT = async (ctx: Context, next: () => Promise<unknown>, token: string) => {
    await verify(token, SECRET, "HS512").then(async () => {
        await next();
    }).catch(() => {
        ctx.cookies.delete("token");
        makeErrorMessage(ctx, 401, "token expired")
    });
}

const allowLogin = async (ctx: Context, next: () => Promise<unknown>) => {
    if (ctx.request.url.pathname === "/login/" || (ctx.request.url.pathname.match(/\/users\/[0-9]+\//g) && ctx.request.method.toString() === "PATCH")) {
        await next();
    } else {
        makeErrorMessage(ctx, 401, "not authorized")
    }
}