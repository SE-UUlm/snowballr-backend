import {Context} from 'https://deno.land/x/oak/mod.ts';

export const makeErrorMessage = (ctx: Context, httpStatus: number, message?: string) => {
    ctx.response.status = httpStatus;
    if (message) {
        ctx.response.body = `{"error": "${message}"}`
    }
}