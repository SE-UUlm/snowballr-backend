import { Context } from "https://deno.land/x/oak@v11.1.0/context.ts";
import { makeErrorMessage } from "../helper/error.ts";
import { Paper } from "../model/db/paper.ts";
import { ReadingList } from "../model/db/readingList.ts";
import { UserStatus, validateUserEntry } from "./validation.controller.ts";

export const addToReadingList = async (ctx: Context, userID: number) => {
  const validate = await validateUserEntry(
    ctx,
    [userID],
    UserStatus.needsSameUser,
    -1,
    { needed: true, params: ["id"] },
    userID,
  );
  if (validate) {
    try {
      const reading = await ReadingList.create({
        userId: userID,
        paperId: validate.id,
      });
      ctx.response.status = 200;
      ctx.response.body = JSON.stringify(reading);
    } catch (er) {
      makeErrorMessage(ctx, 404, "paper does not exist");
      console.log(er);
    }
  }
};

export const removeFromReadingList = async (
  ctx: Context,
  userID: number,
  readingID: number,
) => {
  const validate = await validateUserEntry(
    ctx,
    [userID, readingID],
    UserStatus.needsSameUser,
    -1,
    { needed: false, params: [] },
    userID,
  );
  if (validate) {
    ReadingList.deleteById(readingID);
    ctx.response.status = 200;
  }
};
type reading = { id: number; paper?: Paper };
export const getReadingList = async (ctx: Context, userID: number) => {
  const validate = await validateUserEntry(
    ctx,
    [userID],
    UserStatus.needsSameUser,
    -1,
    { needed: false, params: [] },
    userID,
  );
  if (validate) {
    const reading = await ReadingList.where({ userId: userID }).get();
    if (Array.isArray(reading)) {
      const paper: Promise<Paper>[] = [];
      reading.forEach((item) => paper.push(Paper.find(Number(item.paperId))));
      ctx.response.status = 200;
      const finalPapers = await Promise.all(paper);
      const finalResponse: reading[] = [];
      for (let i = 0; i < reading.length; i++) {
        finalResponse.push({
          id: Number(reading[i].id),
          paper: finalPapers[i],
        });
      }
      ctx.response.body = JSON.stringify({ readinglist: finalResponse });
    }
  }
};
