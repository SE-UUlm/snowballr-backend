import { getPayloadFromJWT } from "./validation.controller.ts";
import { getToken } from "./databaseFetcher/token.ts";
import { Context } from 'https://deno.land/x/oak/mod.ts';

/**
 * Logs out a user by deleting the token in the database and from the cookies.
 *
 * @param ctx
 */
export const logout = async (ctx: Context) => {
    let payload = await getPayloadFromJWT(ctx);
    let token = ctx.request.headers.get("authenticationToken");
    if (token && payload) {
        await getToken(payload.id, token).then(async loginToken => loginToken ? loginToken.delete() : undefined);
    }
    ctx.response.status = 200;
}