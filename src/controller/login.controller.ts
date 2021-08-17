import { returnUserByEmailAndPassword } from "./databaseFetcher/user.ts";
import { Context } from 'https://deno.land/x/oak/mod.ts';
import { convertUserToUserProfile } from "../helper/converter/userConverter.ts";
import { startSession } from "./session.controller.ts";
import { makeErrorMessage } from "../helper/error.ts";
import { jsonBodyToObject } from "../helper/body.ts";
import { checkActive, UserStatus, validateUserEntry } from "./validation.controller.ts";
import { LoginMessage } from "../model/messages/login.message.ts";

/**
 * Login of a user by email and password.
 * @param ctx 
 * @returns login successfull or not
 */
export const login = async (ctx: Context): Promise<boolean> => {
    let validate = await validateUserEntry(ctx, [], UserStatus.none, -1, { needed: true, params: ["email", "password"] })
    if (validate) {

        let user = await returnUserByEmailAndPassword(validate.email, validate.password);
        if (user) {
            if (checkActive(String(user.status))) {
                let token = await startSession(user);
                let loginMessage: LoginMessage = { token: token, user: convertUserToUserProfile(user) }
                ctx.response.body = JSON.stringify(loginMessage)

                return true;
            } else {
                makeErrorMessage(ctx, 401, "not an active profile")
                return false;
            }
        } else {
            makeErrorMessage(ctx, 400, "wrong username or password provided")
            return false;
        }

    }

    return false

}