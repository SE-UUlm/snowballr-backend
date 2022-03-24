import { returnUserByEmailAndPassword } from "./databaseFetcher/user.ts";
import { Context } from "https://deno.land/x/oak/mod.ts";
import { convertUserToUserProfile } from "../helper/converter/userConverter.ts";
import { authToken } from "./session.controller.ts";
import { makeErrorMessage } from "../helper/error.ts";
import { jsonBodyToObject } from "../helper/body.ts";
import { checkActive, createRefreshJWT, getPayloadFromJWTCookie, getPayloadFromJWTHeader, UserStatus, validateRefreshJWT, validateUserEntry } from "./validation.controller.ts";
import { LoginMessage } from "../model/messages/login.message.ts";
import { User } from "../model/db/user.ts";

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
				let token = await authToken(user);
				let refreshToken = await createRefreshJWT(Number(user.id));
				ctx.cookies.set("refreshToken", refreshToken)
				let loginMessage: LoginMessage = { token: token, user: convertUserToUserProfile(user) }
				ctx.response.body = JSON.stringify(loginMessage)
				//ctx.response.headers.set("content-encoding", "");

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

/**
 * Gives a user with a valid refresh cookie a new authentification token
 * @param ctx 
 */
export const refresh = async (ctx: Context) => {
	let token = await validateRefreshJWT(ctx);
	if (token.valid && token.payload) {
		let user = await User.find(token.payload.id)
		let authenticationToken = await authToken(user);
		let loginMessage: LoginMessage = { token: authenticationToken, user: convertUserToUserProfile(user) }
		ctx.response.body = JSON.stringify(loginMessage)
		ctx.response.status = 200;
	} else {
		makeErrorMessage(ctx, 401, "not authorized")
	}
}