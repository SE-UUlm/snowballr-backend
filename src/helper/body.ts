import {Context} from 'https://deno.land/x/oak/mod.ts';
import {makeErrorMessage} from "./error.ts";

export const jsonBodyToObject = (ctx: Context) => {
    try {
        return ctx.request.body({type: "json"}).value;
    } catch (err) {
        makeErrorMessage(ctx, 401, "noBodyProvided")
    }
    return undefined
}