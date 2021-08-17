import { Context } from "https://deno.land/x/oak@v8.0.0/mod.ts";

/**
 * Adds an error message to the oak context
 *
 * @param ctx
 * @param httpStatusCode
 * @param message
 */
export const makeErrorMessage = (ctx: Context, httpStatusCode: number, message?: string) => {
    ctx.response.status = httpStatusCode;
    if (message) {
        ctx.response.body = `{"error": "${message}"}`
    }
}