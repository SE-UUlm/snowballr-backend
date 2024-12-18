import { getPayloadFromJWTHeader } from "./validation.controller.ts";
import { getToken } from "./databaseFetcher/token.ts";
import { Context } from "https://deno.land/x/oak@v11.1.0/mod.ts";

/**
 * Logs out a user by deleting the token in the database and from the cookies.
 *
 * @param ctx
 */
export const logout = async (ctx: Context) => {
  const payload = await getPayloadFromJWTHeader(ctx);
  const token = ctx.request.headers.get("authenticationToken");
  if (token && payload) {
    await getToken(payload.id, token).then(async (loginToken) =>
      loginToken ? loginToken.delete() : undefined
    );
  }
  ctx.response.status = 200;
};
