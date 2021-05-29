import {Context} from 'https://deno.land/x/oak/mod.ts';
import {verify, create, decode} from "https://deno.land/x/djwt@/mod.ts"
import {User} from "../model/db/user.ts";
import {createNumericTerminationDate} from "../helper/dateHelper.ts";

const SECRET = String(Deno.env.get('SECRET'));

export const validateContentType = async (ctx: Context, next: () => Promise<unknown>) => {
    if(await emptyContent(ctx)){
        await next();
    }
    else if (ctx.request.headers.get("Content-Type") === "application/json") {
        ctx.response.type = "application/json";
        if(await validateContent(ctx)){
            await next();
        }
    } else {
        ctx.response.status = 415;
        ctx.response.body = {error: "Only application/json is allowed"}
    }

}
const emptyContent = async (ctx:Context): Promise<boolean> =>{
    try{
        await ctx.request.body({type: "undefined"}).value
        return true;
    } catch (error){
        return false;
    }
}
const validateContent = async (ctx:Context): Promise<boolean> =>{
    try{
        await ctx.request.body({type: "json"}).value
    } catch (error){
        ctx.response.status = 415;
        ctx.response.body = {error: "The requestBody is not in a correct JSON format"}
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
        eMail: user.email,
        isAdmin: user.isAdmin,
        lastName: user.lastName,
        firstName: user.firstName,
        exp: createNumericTerminationDate()
    }, SECRET);
}

export const checkAdmin = async (ctx: Context) => {
    const payloadJson = await getPayloadFromJWT(ctx);
        if(payloadJson.isAdmin) {
            return payloadJson.isAdmin;
        }

    return false;
}

export const getPayloadFromJWT = async (ctx: Context) => {
    let token = await ctx.cookies.get("token");
    if(token) {
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
        ctx.response.redirect("/login");
    });
}

const allowLogin = async (ctx: Context, next: () => Promise<unknown>) => {
    if (ctx.request.url.pathname === "/login") {
        await next();
    } else{
        ctx.response.status = 401
    }
}