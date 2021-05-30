import {returnUserByEmailAndPassword} from "./databaseFetcher/user.ts";
import {Context} from 'https://deno.land/x/oak/mod.ts';
import {convertUserToUserProfile} from "../helper/userConverter.ts";
import {startSession} from "./session.ts";

export const login = async (ctx: Context): Promise<boolean> => {
    const requestParameter = await ctx.request.body({type: "json"}).value;

    if (!requestParameter.email || !requestParameter.password) {
        ctx.response.status = 401;
        ctx.response.body = {error: "no email or password provided"}
        return false;
    }

    let user = await loginFromDatabase(requestParameter.email, requestParameter.password)
    if (user) {
        ctx.response.body = JSON.stringify(convertUserToUserProfile(user));
        await startSession(user, ctx);
        return true;
    } else {
        ctx.response.status = 401;
        ctx.response.body = {error: "wrong username or password provided"}
        return false;
    }

}

const loginFromDatabase = async (email: string, password: string) => {
    return await returnUserByEmailAndPassword(email, password);
}