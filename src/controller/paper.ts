import { Context } from "https://deno.land/x/oak@v7.6.2/context.ts";
import { assign } from "../helper/assign.ts";
import { jsonBodyToObject } from "../helper/body.ts";
import { makeErrorMessage } from "../helper/error.ts";
import { Paper } from "../model/db/paper.ts";
import { paperCache } from "./project.ts";
import { convertPaperToPaperMessageFinished } from "../helper/converter/paperConverter.ts"
export const getPaper = async (ctx: Context, paperID: number | undefined) => {
    if (!paperID) {
        makeErrorMessage(ctx, 422, "no paperID included")
        return
    }

    let paper: Paper = await Paper.find(paperID)
    if (paper) {
        let finished = !paperCache.has(paperID)
        ctx.response.status = 200;
        ctx.response.body = convertPaperToPaperMessageFinished(paper, finished);
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
        let sourcePaper: any = paperCache.get(paperID)
        if (sourcePaper) {
            for (let key in bodyJson) {
                delete sourcePaper[key]
            }
            if (Object.keys(sourcePaper).length > 0) {
                paperCache.add(paperID, sourcePaper)
            } else {
                paperCache.delete(paperID)
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
    ctx.response.body = JSON.stringify(paperCache.get(paperID))



}