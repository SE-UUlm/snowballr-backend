import { Context } from 'https://deno.land/x/oak/mod.ts';
import { assign } from "../helper/assign.ts";
import { jsonBodyToObject } from "../helper/body.ts";
import { makeErrorMessage } from "../helper/error.ts";
import { Paper } from "../model/db/paper.ts";
import { PaperMessage, PapersMessage, Status } from "../model/messages/papersMessage.ts";
import { paperCache } from "./project.controller.ts";
import { convertPapersToPaperMessage, convertPaperToPaperMessage } from "../helper/converter/paperConverter.ts"
import { client, getChildren, saveChildren } from "./database.controller.ts";
import { UserStatus, validateUserEntry } from "./validation.controller.ts";
import { getAllAuthorsFromPaper } from "./databaseFetcher/author.ts";
import { AuthorMessage } from "../model/messages/author.message.ts";
import { convertAuthorToAuthorMessage } from "../helper/converter/authorConverter.ts";
import { Wrote } from "../model/db/wrote.ts";

/**
 * Gets all papers
 * @param ctx 
 */
export const getPapers = async (ctx: Context) => {
    ctx.response.status = 200;
    let message: PapersMessage = { papers: await convertPapersToPaperMessage(await Paper.all()) }
    ctx.response.body = JSON.stringify(message)
}

/**
 * Posts a paper
 * @param ctx 
 */
export const postPaper = async (ctx: Context) => {
    let validate = await validateUserEntry(ctx, [], UserStatus.none, -1, { needed: true, params: [] })
    if (validate) {
        if (validate.id) { delete validate.id }
        let paper = await Paper.create({})
        Object.assign(paper, validate)
        await paper.update()
        ctx.response.status = 200;
        ctx.response.body = JSON.stringify(paper)
    }

}

/**
 * Gets a single paper by it's ID
 * @param ctx 
 * @param paperID 
 * @returns 
 */
export const getPaper = async (ctx: Context, paperID: number) => {
    let validate = await validateUserEntry(ctx, [paperID], UserStatus.none, -1, { needed: false, params: [] })
    if (validate) {

        let paper: Paper = await Paper.find(paperID)
        if (paper) {
            ctx.response.status = 200;
            ctx.response.body = JSON.stringify(await convertPaperToPaperMessage(paper))
        } else {
            makeErrorMessage(ctx, 404, "paper does not exist")
        }
    }

}
/**
 * Returns all papers that are referencing the corresponding paper
 * @param ctx 
 * @param paperID 
 * @returns 
 */
export const getPaperReferences = async (ctx: Context, paperID: number) => {
    let validate = await validateUserEntry(ctx, [paperID], UserStatus.none, -1, { needed: false, params: [] })
    if (validate) {
        return getRefOrCiteList(ctx, "referencedby", "paperreferencedid", "paperreferencingid", paperID)
    }
}

/**
 * Returns all paper that are cited by the corresponding paper
 * @param ctx 
 * @param paperID 
 * @returns 
 */
export const getPaperCitations = async (ctx: Context, paperID: number) => {
    let validate = await validateUserEntry(ctx, [paperID], UserStatus.none, -1, { needed: false, params: [] })
    if (validate) {
        return getRefOrCiteList(ctx, "citedBy", "papercitingid", "papercitedid", paperID)
    }



}

/**
 * Gets all the paper refs or cites of one paper. 
 * It uses the special sql call to retrieve the ids of the paper.
 * The papers value will then be fetched through denodb, to have consistency in the data.
 * @param ctx 
 * @param table 
 * @param column1 
 * @param column2 
 * @param id 
 */
const getRefOrCiteList = async (ctx: Context, table: string, column1: string, column2: string, id: number) => {
    let children = await getChildren(table, column1, column2, id)
    let papersToBe: Promise<Paper>[] = []
    children.rows.forEach((item: any[]) => papersToBe.push(Paper.find(Number(item[0]))));
    ctx.response.status = 200;
    let message: PapersMessage = { papers: await convertPapersToPaperMessage(await Promise.all(papersToBe)) }
    ctx.response.body = JSON.stringify(message)
}

/**
 * Patches the values of the paper.
 * @param ctx 
 * @param paperID 
 * @returns 
 */
export const patchPaper = async (ctx: Context, paperID: number) => {
    let validate = await validateUserEntry(ctx, [paperID], UserStatus.none, -1, { needed: false, params: [] })
    if (validate) {
        let paper: Paper = await Paper.find(paperID);
        if (paper) {
            await paperUpdate(ctx, paper)
        } else {
            makeErrorMessage(ctx, 404, "paper does not exist")
        }
    }
}

/**
 * Updates a paper by the given values.
 * If the paper has chooseable values in the filecache and that value is fetched, the corresponding choosable values will be deleted.
 * If not a single value is left behind in the filecache, the file itself will be deleted.
 * @param ctx 
 * @param paper 
 * @returns 
 */
export const paperUpdate = async (ctx: Context, paper: Paper) => {
    let paperID = Number(paper.id)
    let bodyJson = await jsonBodyToObject(ctx);
    if (!bodyJson) {
        return
    }
    let sourcePaper: any = paperCache.get(String(paperID))
    if (sourcePaper) {
        for (let key in bodyJson) {
            delete sourcePaper[key]
        }
        if (Object.keys(sourcePaper).length > 0) {
            await paperCache.add(String(paperID), sourcePaper)
        } else {
            await paperCache.delete(String(paperID))
        }
    }
    delete bodyJson.author;
    assign(paper, bodyJson);
    await paper.update()
    ctx.response.status = 200;
    ctx.response.body = JSON.stringify(paper)
}

/**
 * Returns all values left to be choosen by a user of a given paper.
 * @param ctx 
 * @param paperID 
 * @returns 
 */
export const getSourcePaper = async (ctx: Context, paperID: number) => {
    let validate = await validateUserEntry(ctx, [paperID], UserStatus.none, -1, { needed: false, params: [] })
    if (validate) {
        ctx.response.status = 200;
        let paper = paperCache.get(String(paperID))
        if (paper) {
            ctx.response.body = JSON.stringify(paper)
        } else {
            makeErrorMessage(ctx, 404, "paper does not exist")
        }
    }
}

/**
 * Adds a citation of a paper into the database
 * @param ctx 
 * @param id 
 * @returns 
 */
export const postPaperCitation = async (ctx: Context, id: number) => {
    let validate = await validateUserEntry(ctx, [id], UserStatus.none, -1, { needed: true, params: ["id"] })
    if (validate) {
        let paper;
        let paper2;

        paper = await Paper.find(id)
        paper2 = await Paper.find(validate.id)
        if (paper && paper2) {
            await saveChildren("citedby", "papercitedid", "papercitingid", Number(paper2.id), Number(paper.id));
            ctx.response.status = 200
            return paper2;
        }
        makeErrorMessage(ctx, 404, "Paper not found")
        return
    }
}
/**
 * Adds a reference by a paper into the database
 * @param ctx 
 * @param id 
 * @returns 
 */
export const postPaperReference = async (ctx: Context, id: number) => {
    let validate = await validateUserEntry(ctx, [id], UserStatus.none, -1, { needed: true, params: ["id"] })
    if (validate) {
        let paper = await Paper.find(id)
        let paper2 = await Paper.find(validate.id)
        if (paper && paper2) {
            await saveChildren("referencedby", "paperreferencedid", "paperreferencingid", Number(paper.id), Number(paper2.id));
            return paper2;
        }

        makeErrorMessage(ctx, 404, "Paper not found")
        return
    }
}

/**
 * Returns all authors of a paper
 * @param ctx 
 * @param id 
 * @returns 
 */
export const getAuthors = async (ctx: Context, id: number) => {
    let validate = await validateUserEntry(ctx, [id], UserStatus.none, -1, { needed: false, params: [] })
    if (validate) {
        let authors = await getAllAuthorsFromPaper(id)
        let message = { authors: authors.map(item => convertAuthorToAuthorMessage(item)) }
        ctx.response.body = JSON.stringify(message)
        ctx.response.status = 200
    }
}

/**
 * Adds an author to a paper
 * @param ctx 
 * @param id 
 * @returns 
 */
export const addAuthorToPaper = async (ctx: Context, id: number) => {
    let validate = await validateUserEntry(ctx, [id], UserStatus.none, -1, { needed: true, params: ["id"] })
    if (validate) {
        try {
            await Wrote.create({ paperId: id, authorId: validate.id })
            ctx.response.status = 200
        } catch (error) {
            makeErrorMessage(ctx, 404, "Paper or Author not found")
        }
    }
}

/**
 * Removes an author as author of a paper
 * @param ctx 
 * @param id 
 * @returns 
 */
export const deleteAuthorOfPaper = async (ctx: Context, paperID: number, authorID: number) => {
    let validate = await validateUserEntry(ctx, [paperID, authorID], UserStatus.none, -1, { needed: false, params: [] })
    if (validate) {
        await Wrote.where({ paperId: paperID, authorId: authorID }).delete()
        ctx.response.status = 200
    }
}