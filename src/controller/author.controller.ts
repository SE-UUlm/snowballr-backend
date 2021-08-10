import { Context } from "https://deno.land/x/oak/mod.ts";
import { assign } from "../helper/assign.ts";
import { jsonBodyToObject } from "../helper/body.ts";
import { makeErrorMessage } from "../helper/error.ts";
import { Author } from "../model/db/author.ts";
import { convertAuthorToAuthorMessage } from "../helper/converter/authorConverter.ts"
import { authorCache } from "./project.controller.ts";
import { UserStatus, validateUserEntry } from "./validation.controller.ts";


/**
 * Inserts an author to the database
 * @param ctx 
 * @returns 
 */
export const postAuthor = async (ctx: Context) => {
    let validate = await validateUserEntry(ctx, [], UserStatus.none, -1, { needed: true, params: [] })
    if (!validate) {
        return
    }

    let author = await Author.create({});
    if (validate.rawString) { author.rawString = validate.rawString }
    if (validate.orcid) { author.orcid = validate.orcid }
    if (validate.lastName) { author.lastName = validate.lastName }
    if (validate.firstName) { author.firstName = validate.firstName }
    await author.update();

    ctx.response.status = 200;
    ctx.response.body = JSON.stringify(author)
}

/**
 * Returns one Author
 * @param ctx 
 * @param authorID id of the author
 *
 */
export const getAuthor = async (ctx: Context, authorID: number) => {
    let validate = await validateUserEntry(ctx, [], UserStatus.none, -1, { needed: false, params: [] })
    if (!validate) {
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

/**
 * Changes the Parameter of an author
 * @param ctx 
 * @param authorID 
 * @returns 
 */
export const patchAuthor = async (ctx: Context, authorID: number) => {
    let validate = await validateUserEntry(ctx, [], UserStatus.none, -1, { needed: false, params: [] })
    if (!validate) {
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

/**
 * Returns the author values, that are currently in the filecache of the correspondend author
 * @param ctx 
 * @param authorID 
 * @returns 
 */
export const getSourceAuthor = async (ctx: Context, authorID: number | undefined) => {
    let validate = await validateUserEntry(ctx, [], UserStatus.none, -1, { needed: false, params: [] })
    if (!validate) {
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