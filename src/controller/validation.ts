import {Context} from 'https://deno.land/x/oak/mod.ts';
import { verify, create } from "https://deno.land/x/djwt@/mod.ts"
import {User} from "../model/user.ts";
import {createNumericTerminationDate} from "../helper/dateHelper.ts";

const SECRET = String(Deno.env.get('SECRET'));

export const validate = async ( ctx: Context, next: () => Promise<unknown>) => {

    let token = ctx.cookies.get("token");
    if(token) {
        return  verifyToken(ctx, next, token)
    } else {
        return  allowLogin(ctx, next)
    }
}

export const createToken = async (user: User) => {
    return await create({alg: "HS512", typ: "JWT"}, {eMail: user.email, isAdmin: user.isAdmin, lastName: user.lastName, firstName: user.firstName, exp: createNumericTerminationDate()}, SECRET);
}

const verifyToken = async (ctx: Context, next: () => Promise<unknown>, token: string) => {
    await verify(token, SECRET, "HS512").then(() => {
        next();
    }).catch(() => {
        ctx.cookies.delete("token");
        ctx.response.redirect("/login");
    });
}

const allowLogin = async (ctx: Context, next: () => Promise<unknown>) => {
    if (ctx.request.url.pathname === "/login") {
        await next();
    } else {
        ctx.response.redirect("/login");
    }
}