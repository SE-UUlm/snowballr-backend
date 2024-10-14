import { Context } from "https://deno.land/x/oak@v11.1.0/mod.ts";
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
    if (validate) {
        let author = await Author.create({});
        if (validate.rawString) { author.rawString = validate.rawString }
        if (validate.orcid) { author.orcid = validate.orcid }
        if (validate.lastName) { author.lastName = validate.lastName }
        if (validate.firstName) { author.firstName = validate.firstName }
        await author.update();

        ctx.response.status = 200;
        ctx.response.body = JSON.stringify(author)
    }


}

/**
 * Returns one Author
 * @param ctx 
 * @param authorID id of the author
 *
 */
export const getAuthor = async (ctx: Context, authorID: number) => {
    let validate = await validateUserEntry(ctx, [], UserStatus.none, -1, { needed: false, params: [] })
    if (validate) {
        let author: Author = await Author.find(authorID)
        if (author) {
            ctx.response.status = 200;
            ctx.response.body = JSON.stringify(convertAuthorToAuthorMessage(author))
        } else {
            makeErrorMessage(ctx, 404, "author does not exist")
        }
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
    if (validate) {
        let author: Author = await Author.find(authorID);
        if (author) {
            let bodyJson = await jsonBodyToObject(ctx);
            if (!bodyJson) {
                return
            }
           deleteKeyOfAuthorSourceFile(authorID, bodyJson)
            assign(author, bodyJson);
            await author.update()
            ctx.response.status = 200;
            ctx.response.body = JSON.stringify(author)
        } else {
            makeErrorMessage(ctx, 404, "author does not exist")
        }
    }
}

/**
 * Checks if there is still a source author file.
 * If yes, deletes all the keys someone has patched
 * @param authorID 
 * @param bodyJson 
 */
const deleteKeyOfAuthorSourceFile = async (authorID: number, bodyJson: any) =>{
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
} 

/**
 * Returns the author values, that are currently in the filecache of the correspondend author
 * @param ctx 
 * @param authorID 
 * @returns 
 */
export const getSourceAuthor = async (ctx: Context, authorID: number | undefined) => {
    let validate = await validateUserEntry(ctx, [], UserStatus.none, -1, { needed: false, params: [] })
    if (validate) {
        ctx.response.status = 200;
        let author = authorCache.get(String(authorID))
        if (author) {
            ctx.response.body = JSON.stringify(author)
        } else {
            makeErrorMessage(ctx, 404, "author does not exist")
        }
    }
}

/**
 * Deletes a source author
 * @param ctx 
 * @param paperID 
 * @returns 
 */
 export const deleteSourceAuthor = async (ctx: Context,authorID: number) => {
    let validate = await validateUserEntry(ctx, [authorID], UserStatus.none, -1, { needed: false, params: [] })
    if (validate) {
        ctx.response.status = 200;
        authorCache.delete(String(authorID))
    }
}

