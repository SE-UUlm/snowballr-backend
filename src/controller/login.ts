import {returnUserByEmailAndPassword} from "./databaseFetcher/user.ts";
import {Context} from 'https://deno.land/x/oak/mod.ts';
import {convertUserToUserProfile} from "../helper/userConverter.ts";
import {startSession} from "./session.ts";
import {makeErrorMessage} from "../helper/error.ts";
import {jsonBodyToObject} from "../helper/body.ts";
import {checkActive} from "./validation.ts";

export const login = async (ctx: Context): Promise<boolean> => {
    const requestParameter = await jsonBodyToObject(ctx)
    if (!requestParameter) {
        return false
    }

    if (!requestParameter.email || !requestParameter.password) {
        makeErrorMessage(ctx, 401, "no email or password provided")
        return false;
    }

    let user = await returnUserByEmailAndPassword(requestParameter.email, requestParameter.password);
    if (user) {
        if (checkActive(String(user.status))) {
            let token = await startSession(user);
            let userprofile = JSON.stringify(convertUserToUserProfile(user))
            ctx.response.body = `{
                                    "token": ${token}, 
                                    "user": ${userprofile}
                                 }`

            return true;
        } else {
            makeErrorMessage(ctx, 401, "not an active profile")
            return false;
        }
    } else {
        makeErrorMessage(ctx, 401, "wrong username or password provided")
        return false;
    }

}