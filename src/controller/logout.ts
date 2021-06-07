import {getPayloadFromJWT} from "./validation.ts";
import {getToken} from "./databaseFetcher/token.ts";
import {Context} from 'https://deno.land/x/oak/mod.ts';

export const logout = async (ctx: Context) => {
    let payload = await getPayloadFromJWT(ctx);
    let token = await ctx.cookies.get("token");
    if(token) {
        await getToken(payload.id, token).then(async token => token?await token.delete() : undefined);
    }
    ctx.cookies.delete("token");
    ctx.response.status = 200;
}