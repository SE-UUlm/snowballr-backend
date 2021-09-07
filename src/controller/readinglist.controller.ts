import { Context } from "https://deno.land/x/oak@v9.0.0/context.ts";
import { convertPapersToPaperMessage, convertPaperToPaperMessage } from "../helper/converter/paperConverter.ts";
import { makeErrorMessage } from "../helper/error.ts";
import { Paper } from "../model/db/paper.ts";
import { ReadingList } from "../model/db/readingList.ts";
import { validateUserEntry,UserStatus, getUserID, getPayloadFromJWT } from "./validation.controller.ts";



export const addToReadingList = async (ctx: Context, userID: number) => {
    let validate = await validateUserEntry(ctx, [userID], UserStatus.needsSameUser, -1, { needed: true, params: ["id"] }, userID)
    if (validate) {
        try{
            let reading = await ReadingList.create({userId: userID, paperId: validate.id})
            ctx.response.status = 200
            ctx.response.body = JSON.stringify(reading)
        } catch(er){
            makeErrorMessage(ctx, 404, "paper does not exist")
            console.log(er)
        }  
    }
}

export const removeFromReadingList= async (ctx: Context, userID: number, readingID: number) => {
    let validate = await validateUserEntry(ctx, [userID, readingID], UserStatus.needsSameUser, -1, { needed: false, params: [] }, userID)
    if (validate) {
        ReadingList.deleteById(readingID)
        ctx.response.status = 200;
    }
}
type reading = {id: number, paper?: Paper}
export const getReadingList  = async (ctx: Context, userID: number) => {
    let validate = await validateUserEntry(ctx, [userID], UserStatus.needsSameUser, -1, { needed: false, params: [] }, userID)
    if (validate) {

        let reading = await ReadingList.where({userId: userID}).get()
        if(Array.isArray(reading)){
            let paper: Promise<Paper>[] = [];
            reading.forEach(item => paper.push(Paper.find(Number(item.paperId))))
            ctx.response.status = 200
            let finalPapers = await Promise.all(paper)
            let finalResponse: reading[] = []
            for(let i= 0; i < reading.length; i++){
                finalResponse.push({id: Number(reading[i].id), paper: finalPapers[i]})
            }
            ctx.response.body = JSON.stringify({ ReadingList: finalResponse})
        }

    }
}