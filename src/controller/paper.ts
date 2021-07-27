import { Context } from 'https://deno.land/x/oak/mod.ts';
import { assign } from "../helper/assign.ts";
import { jsonBodyToObject } from "../helper/body.ts";
import { makeErrorMessage } from "../helper/error.ts";
import { Paper } from "../model/db/paper.ts";
import { PaperMessage, PapersMessage, PaperStatus } from "../model/messages/papersMessage.ts";
import { paperCache } from "./project.ts";
import { convertPapersToPaperMessage, convertPaperToPaperMessage } from "../helper/converter/paperConverter.ts"

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
        ctx.response.body = await convertPaperToPaperMessage(paper)
    } else {
        makeErrorMessage(ctx, 404, "paper does not exist")
    }
}

export const patchPaper = async (ctx: Context, paperID: number | undefined) => {
    if (!paperID) {
        makeErrorMessage(ctx, 422, "no paperID included")
        return
    }

    let paper: Paper = await Paper.find(paperID);
    if (paper) {
        let bodyJson = jsonBodyToObject(ctx);
        if (!bodyJson) {
            return
        }
        let sourcePaper: any = paperCache.get(String(paperID))
        if (sourcePaper) {
            for (let key in bodyJson) {
                delete sourcePaper[key]
            }
            if (Object.keys(sourcePaper).length > 0) {
                paperCache.add(String(paperID), sourcePaper)
            } else {
                paperCache.delete(String(paperID))
            }
        }
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