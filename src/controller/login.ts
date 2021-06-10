import {returnUserByEmailAndPassword} from "./databaseFetcher/user.ts";
import {Context} from 'https://deno.land/x/oak/mod.ts';
import {convertUserToUserProfile} from "../helper/userConverter.ts";
import {startSession} from "./session.ts";
import {makeErrorMessage} from "../helper/error.ts";
import {jsonBodyToObject} from "../helper/body.ts";

export const login = async (ctx: Context): Promise<boolean> => {
    const requestParameter = await jsonBodyToObject(ctx)
    if (!requestParameter) {
        return false
    }

    if (!requestParameter.email || !requestParameter.password) {
        makeErrorMessage(ctx, 401, "no email or password provided")
        return false;
    }

    let user = await loginFromDatabase(requestParameter.email, requestParameter.password)
    if (user) {
        ctx.response.body = JSON.stringify(convertUserToUserProfile(user));
        await startSession(user, ctx);
        return true;
    } else {
        makeErrorMessage(ctx, 401, "wrong username or password provided")
        return false;
    }

}

const loginFromDatabase = async (email: string, password: string) => {
    return await returnUserByEmailAndPassword(email, password);
}