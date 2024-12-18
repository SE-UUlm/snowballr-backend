import { Context } from "https://deno.land/x/oak@v11.1.0/mod.ts";
import { assign } from "../helper/assign.ts";
import { jsonBodyToObject } from "../helper/body.ts";
import { makeErrorMessage } from "../helper/error.ts";
import { Paper } from "../model/db/paper.ts";
import { PapersMessage } from "../model/messages/papersMessage.ts";
import { paperCache } from "./project.controller.ts";
import {
  convertPapersToPaperMessage,
  convertPaperToPaperMessage,
} from "../helper/converter/paperConverter.ts";
import { getChildren, saveChildren } from "./database.controller.ts";
import { UserStatus, validateUserEntry } from "./validation.controller.ts";
import { getAllAuthorsFromPaper } from "./databaseFetcher/author.ts";
import { convertAuthorToAuthorMessage } from "../helper/converter/authorConverter.ts";
import { Wrote } from "../model/db/wrote.ts";

/**
 * Gets all papers
 * @param ctx
 */
export const getPapers = async (ctx: Context) => {
  ctx.response.status = 200;
  const message: PapersMessage = {
    papers: await convertPapersToPaperMessage(await Paper.all()),
  };
  ctx.response.body = JSON.stringify(message);
};

/**
 * Posts a paper
 * @param ctx
 */
export const postPaper = async (ctx: Context) => {
  const validate = await validateUserEntry(ctx, [], UserStatus.none, -1, {
    needed: true,
    params: [],
  });
  if (validate) {
    if (validate.id) delete validate.id;
    const paper = await Paper.create({});
    Object.assign(paper, validate);
    await paper.update();
    ctx.response.status = 200;
    ctx.response.body = JSON.stringify(paper);
  }
};

/**
 * Gets a single paper by it's ID
 * @param ctx
 * @param paperID
 * @returns
 */
export const getPaper = async (ctx: Context, paperID: number) => {
  const validate = await validateUserEntry(
    ctx,
    [paperID],
    UserStatus.none,
    -1,
    { needed: false, params: [] },
  );
  if (validate) {
    const paper: Paper = await Paper.find(paperID);
    if (paper) {
      ctx.response.status = 200;
      ctx.response.body = JSON.stringify(
        await convertPaperToPaperMessage(paper),
      );
    } else {
      makeErrorMessage(ctx, 404, "paper does not exist");
    }
  }
};
/**
 * Returns all papers that are referencing the corresponding paper
 * @param ctx
 * @param paperID
 * @returns
 */
export const getPaperReferences = async (ctx: Context, paperID: number) => {
  const validate = await validateUserEntry(
    ctx,
    [paperID],
    UserStatus.none,
    -1,
    { needed: false, params: [] },
  );
  if (validate) {
    const papers = await getRefOrCiteList(
      ctx,
      "referencedby",
      "paperreferencedid",
      "paperreferencingid",
      paperID,
    );
    ctx.response.status = 200;
    const message: PapersMessage = {
      papers: await convertPapersToPaperMessage(papers),
    };
    ctx.response.body = JSON.stringify(message);
  }
};

/**
 * Returns all paper that are cited by the corresponding paper
 * @param ctx
 * @param paperID
 * @returns
 */
export const getPaperCitations = async (ctx: Context, paperID: number) => {
  const validate = await validateUserEntry(
    ctx,
    [paperID],
    UserStatus.none,
    -1,
    { needed: false, params: [] },
  );
  if (validate) {
    const papers = await getRefOrCiteList(
      ctx,
      "citedBy",
      "papercitingid",
      "papercitedid",
      paperID,
    );
    ctx.response.status = 200;
    const message: PapersMessage = {
      papers: await convertPapersToPaperMessage(await Promise.all(papers)),
    };
    ctx.response.body = JSON.stringify(message);
  }
};

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
export const getRefOrCiteList = async (
  ctx: Context,
  table: string,
  column1: string,
  column2: string,
  id: number,
) => {
  const children = await getChildren(table, column1, column2, id);
  const papersToBe: Paper[] = [];
  for (const item of children.rows) {
    papersToBe.push(await Paper.find(Number(item[0])));
  }
  return papersToBe;
};

/**
 * Patches the values of the paper.
 * @param ctx
 * @param paperID
 * @returns
 */
export const patchPaper = async (ctx: Context, paperID: number) => {
  const validate = await validateUserEntry(
    ctx,
    [paperID],
    UserStatus.none,
    -1,
    { needed: false, params: [] },
  );
  if (validate) {
    const paper: Paper = await Paper.find(paperID);
    if (paper) {
      const bodyJson = await jsonBodyToObject(ctx);
      if (!bodyJson) {
        return;
      }
      await paperUpdate(ctx, paper, bodyJson);
    } else {
      makeErrorMessage(ctx, 404, "paper does not exist");
    }
  }
};

/**
 * Updates a paper by the given values.
 * If the paper has chooseable values in the filecache and that value is fetched, the corresponding choosable values will be deleted.
 * If not a single value is left behind in the filecache, the file itself will be deleted.
 * @param ctx
 * @param paper
 * @returns
 */
export const paperUpdate = async (
  ctx: Context,
  paper: Paper,
  bodyJson: any,
) => {
  const paperID = Number(paper.id);

  const sourcePaper: any = paperCache.get(String(paperID));
  if (sourcePaper) {
    for (const key in bodyJson) {
      delete sourcePaper[key];
      delete sourcePaper[key + "Source"];
    }
    if (Object.keys(sourcePaper).length > 0) {
      await paperCache.add(String(paperID), sourcePaper);
    } else {
      await paperCache.delete(String(paperID));
    }
  }
  delete bodyJson.author;
  assign(paper, bodyJson);
  await paper.update();
  ctx.response.status = 200;
  ctx.response.body = JSON.stringify(paper);
};

/**
 * Returns all values left to be choosen by a user of a given paper.
 * @param ctx
 * @param paperID
 * @returns
 */
export const getSourcePaper = async (ctx: Context, paperID: number) => {
  const validate = await validateUserEntry(
    ctx,
    [paperID],
    UserStatus.none,
    -1,
    { needed: false, params: [] },
  );
  if (validate) {
    ctx.response.status = 200;
    const paper = paperCache.get(String(paperID));
    if (paper) {
      ctx.response.body = JSON.stringify(paper);
    } else {
      makeErrorMessage(ctx, 404, "paper does not exist");
    }
  }
};

/**
 * Deletes a source paper
 * @param ctx
 * @param paperID
 * @returns
 */
export const deleteSourcePaper = async (ctx: Context, paperID: number) => {
  const validate = await validateUserEntry(
    ctx,
    [paperID],
    UserStatus.none,
    -1,
    { needed: false, params: [] },
  );
  if (validate) {
    ctx.response.status = 200;
    paperCache.delete(String(paperID));
  }
};

/**
 * Adds a citation of a paper into the database
 * @param ctx
 * @param id
 * @returns
 */
export const postPaperCitation = async (ctx: Context, id: number) => {
  const validate = await validateUserEntry(ctx, [id], UserStatus.none, -1, {
    needed: true,
    params: ["id"],
  });
  if (validate) {
    const paper = await Paper.find(id);
    const paper2 = await Paper.find(validate.id);
    if (paper && paper2) {
      await saveChildren(
        "citedby",
        "papercitedid",
        "papercitingid",
        Number(paper2.id),
        Number(paper.id),
      );
      ctx.response.status = 200;
      return paper2;
    }
    makeErrorMessage(ctx, 404, "Paper not found");
    return;
  }
};
/**
 * Adds a reference by a paper into the database
 * @param ctx
 * @param id
 * @returns
 */
export const postPaperReference = async (ctx: Context, id: number) => {
  const validate = await validateUserEntry(ctx, [id], UserStatus.none, -1, {
    needed: true,
    params: ["id"],
  });
  if (validate) {
    const paper = await Paper.find(id);
    const paper2 = await Paper.find(validate.id);
    if (paper && paper2) {
      await saveChildren(
        "referencedby",
        "paperreferencedid",
        "paperreferencingid",
        Number(paper.id),
        Number(paper2.id),
      );
      return paper2;
    }

    makeErrorMessage(ctx, 404, "Paper not found");
    return;
  }
};

/**
 * Returns all authors of a paper
 * @param ctx
 * @param id
 * @returns
 */
export const getAuthors = async (ctx: Context, id: number) => {
  const validate = await validateUserEntry(ctx, [id], UserStatus.none, -1, {
    needed: false,
    params: [],
  });
  if (validate) {
    const authors = await getAllAuthorsFromPaper(id);
    const message = {
      authors: authors.map((item) => convertAuthorToAuthorMessage(item)),
    };
    ctx.response.body = JSON.stringify(message);
    ctx.response.status = 200;
  }
};

/**
 * Adds an author to a paper
 * @param ctx
 * @param id
 * @returns
 */
export const addAuthorToPaper = async (ctx: Context, id: number) => {
  const validate = await validateUserEntry(ctx, [id], UserStatus.none, -1, {
    needed: true,
    params: ["id"],
  });
  if (validate) {
    try {
      await Wrote.create({ paperId: id, authorId: validate.id });
      ctx.response.status = 200;
    } catch (_) {
      makeErrorMessage(ctx, 404, "Paper or Author not found");
    }
  }
};

/**
 * Removes an author as author of a paper
 * @param ctx
 * @param id
 * @returns
 */
export const deleteAuthorOfPaper = async (
  ctx: Context,
  paperID: number,
  authorID: number,
) => {
  const validate = await validateUserEntry(
    ctx,
    [paperID, authorID],
    UserStatus.none,
    -1,
    { needed: false, params: [] },
  );
  if (validate) {
    await Wrote.where({ paperId: paperID, authorId: authorID }).delete();
    ctx.response.status = 200;
  }
};
