import { Context } from "https://deno.land/x/oak@v11.1.0/mod.ts";
import { makePromise } from "./assign.ts";
import { makeErrorMessage } from "./error.ts";

/**
 * Converts the Body to a json object, if possible.
 * If not possible, an error is added to the context.
 * @param ctx oak context with request/response
 */
export const jsonBodyToObject = (ctx: Context) => {
  try {
    return ctx.request.body({ type: "json" }).value;
  } catch (_) {
    makeErrorMessage(ctx, 422, "noBodyProvided");
  }
  return makePromise(undefined);
};
