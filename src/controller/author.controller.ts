import { Context } from "https://deno.land/x/oak/mod.ts";
import { assign } from "../helper/assign.ts";
import { jsonBodyToObject } from "../helper/body.ts";
import { makeErrorMessage } from "../helper/error.ts";
import { Author } from "../model/db/author.ts";
import { authorCache } from "./project.ts";
import {convertAuthorToAuthorMessage} from "../helper/converter/authorConverter.ts"

export const getAuthor = async (ctx: Context, authorID: number | undefined) => {
    if (!authorID) {
        makeErrorMessage(ctx, 422, "no authorID included")
        return
    }

    let author: Author = await Author.find(authorID)
    if (author) {
        ctx.response.status = 200;
        ctx.response.body = JSON.stringify(convertAuthorToAuthorMessage(author))
    } else {
        makeErrorMessage(ctx, 404, "author does not exist")
    }
}


export const patchAuthor = async (ctx: Context, authorID: number | undefined) => {
    if (!authorID) {
        makeErrorMessage(ctx, 422, "no authorID included")
        return
    }

    let author: Author = await Author.find(authorID);
    if (author) {
        let bodyJson = await jsonBodyToObject(ctx);
        if (!bodyJson) {
            return
        }
        let sourceAuthor: any = authorCache.get(String(authorID))
        if (sourceAuthor) {

            for (let key in bodyJson) {
                delete sourceAuthor[key]
            }
            if (Object.keys(sourceAuthor).length > 0) {
                await authorCache.add(String(authorID), sourceAuthor)
            } else {
                await authorCache.delete(String(authorID))
            }

        }
        assign(author, bodyJson);
        await author.update()
        ctx.response.status = 200;
        ctx.response.body = JSON.stringify(author)
    } else {
        makeErrorMessage(ctx, 404, "author does not exist")
    }
}

export const getSourceAuthor = (ctx: Context, authorID: number | undefined) => {
    if (!authorID) {
        makeErrorMessage(ctx, 422, "no authorID included")
        return
    }
    ctx.response.status = 200;
    let author = authorCache.get(String(authorID))
    if (author) {
        ctx.response.body = JSON.stringify(author)
    } else {
        makeErrorMessage(ctx, 404, "author does not exist")
    }



}