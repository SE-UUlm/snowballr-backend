import { Context } from 'https://deno.land/x/oak/mod.ts';
import { assign } from "../helper/assign.ts";
import { jsonBodyToObject } from "../helper/body.ts";
import { makeErrorMessage } from "../helper/error.ts";
import { Paper } from "../model/db/paper.ts";
import { PaperMessage, PapersMessage, Status } from "../model/messages/papersMessage.ts";
import { paperCache } from "./project.controller.ts";
import { convertPapersToPaperMessage, convertPaperToPaperMessage } from "../helper/converter/paperConverter.ts"
import { client } from "./database.controller.ts";

export const getPapers = async (ctx: Context) => {
    ctx.response.status = 200;
    let message: PapersMessage = { papers: await convertPapersToPaperMessage(await Paper.all()) }
    ctx.response.body = JSON.stringify(message)
}
export const getPaper = async (ctx: Context, paperID: number | undefined) => {
    if (!paperID) {
        makeErrorMessage(ctx, 422, "no paperID included")
        return
    }

    let paper: Paper = await Paper.find(paperID)
    if (paper) {
        ctx.response.status = 200;
        ctx.response.body = JSON.stringify(await convertPaperToPaperMessage(paper))
    } else {
        makeErrorMessage(ctx, 404, "paper does not exist")
    }
}

export const getPaperReferences = async (ctx: Context, paperID: number | undefined) => {
    if (!paperID) {
        makeErrorMessage(ctx, 422, "no paperID included")
        return
    }
    await getRefOrCiteList(ctx, "referencedby", "paperreferencedid", "paperreferencingid", paperID)

}

export const getPaperCitations = async (ctx: Context, paperID: number | undefined) => {
    if (!paperID) {
        makeErrorMessage(ctx, 422, "no paperID included")
        return
    }
    await getRefOrCiteList(ctx, "citedBy", "papercitedid", "papercitingid", paperID)

}

const getRefOrCiteList = async (ctx: Context, table: string, column1: string, column2: string, id: number) => {

    let children = await getChildren(table, column1, column2, id)
    let papersToBe: Promise<Paper>[] = []
    children.rows.forEach((item: any[]) => papersToBe.push(Paper.find(Number(item[0]))));
    ctx.response.status = 200;
    let message: PapersMessage = { papers: await convertPapersToPaperMessage(await Promise.all(papersToBe)) }
    ctx.response.body = JSON.stringify(message)
}

const getChildren = (table: string, column1: string, column2: string, id: number) => {
    return client.queryArray(`select p.* from ${table} as i JOIN paper as p ON i.${column2} = p.id WHERE i.${column1} = ${id}`);
}

export const patchPaper = async (ctx: Context, paperID: number | undefined) => {
    if (!paperID) {
        makeErrorMessage(ctx, 422, "no paperID included")
        return
    }

    let paper: Paper = await Paper.find(paperID);
    if (paper) {
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
    } else {
        makeErrorMessage(ctx, 404, "paper does not exist")
    }
}

export const getSourcePaper = (ctx: Context, paperID: number | undefined) => {
    if (!paperID) {
        makeErrorMessage(ctx, 422, "no paperID included")
        return
    }
    ctx.response.status = 200;
    let paper = paperCache.get(String(paperID))
    if (paper) {
        ctx.response.body = JSON.stringify(paper)
    } else {
        makeErrorMessage(ctx, 404, "paper does not exist")
    }



}