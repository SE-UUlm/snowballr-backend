import { Paper } from "../../model/db/paper.ts";
import { PaperMessage } from "../../model/messages/papersMessage.ts";
import { getProjectPaperID } from "../../controller/databaseFetcher/paper.ts";
import { assign } from "../assign.ts"
import { IApiPaper } from "../../api/iApiPaper.ts"
import { getDOI } from "../../api/apiMerger.ts";
import { logger, fileLogger } from "../../api/logger.ts";
import { PaperID } from "../../model/db/paperID.ts";
import { PaperHasID } from "../../model/db/paperHasID.ts";
import { IApiUniqueId } from "../../api/iApiUniqueId.ts";
import { Author } from "../../model/db/author.ts";
import { Wrote } from "../../model/db/wrote.ts";
import { Pdf } from "../../model/db/pdf.ts";
export const convertPapersToPaperMessage = async (papers: Paper[], stageId: number) => {
    let paperMessages: PaperMessage[] = [];
    for (const item of papers) {
        let paperMessage: PaperMessage = await convertPaperToPaperMessage(item, stageId)
        paperMessages.push(paperMessage)
    }
    return paperMessages;
}

export const convertPaperToPaperMessage = async (paper: Paper, stageId: number) => {
    let paperMessage: PaperMessage = { id: Number(paper.id), ppid: await getProjectPaperID(stageId, Number(paper.id)) }
    assign(paperMessage, paper)
    return paperMessage;
}

export const convertPaperToPaperMessageFinished = (paper: Paper, finished: boolean) => {
    let paperMessage: PaperMessage = { id: Number(paper.id) }
    assign(paperMessage, paper)
    return JSON.stringify({ finished: finished, paper: paperMessage })
}



export const convertIApiPaperToDBPaper = async (paper: IApiPaper): Promise<Paper> => {
    let newPaper = await Paper.create({})
    let p: any = paper;
    for (let i in paper) {
        if (["title", "abstract", "publisher", "type", "scopeName", "year"].includes(i)) {
            newPaper[i] = p[i][0]
        } else if (i == "uniqueId") {
            //TODO rest unique keys
            newPaper.doi = getDOI(paper)[0]
            let uniqueId = paper.uniqueId.filter((item: IApiUniqueId) => item.type !== "DOI")

        } else if (i == "author") {
            for (let item of paper.author) {
                let author: Author;
                if (item.orcid[0]) {
                    let oldAuthor = await Author.where({ orcid: item.orcid[0] }).get()
                    if (Array.isArray(oldAuthor) && oldAuthor[0]) {
                        author = oldAuthor[0]
                    } else {
                        author = await Author.create({})
                    }
                } else {
                    author = await Author.create({})
                }
                //TODO unique values....
                if (!author.firstName) { author.firstName = item.firstName[0] }
                if (!author.lastName) { author.lastName = item.lastName[0] }
                if (!author.raw) { author.raw = item.rawString[0] }
                author.update()
                Wrote.create({ paperId: Number(newPaper.id), authorId: Number(author.id) })
            }

        } else if (i == "pdf") {
            paper.pdf.forEach(item => {
                Pdf.create({ paperId: Number(newPaper.id), url: item })
            })
        }
    }
    await newPaper.update()


    return newPaper;




}

export const assignOnlyIfUnassignedPaper = (target: Paper, source: IApiPaper) => {
    let s = <any>source
    for (const key in source) {
        const val = s[key];
        if (val && !target[key]) {
            if (["title", "abstract", "publisher", "type", "scopeName", "year"].includes(key)) {
                target[key] = s[key][0]
                if (s[key].length <= 1) {
                    delete s[key]
                }
            } else if (key == "uniqueId") {
                //TODO rest unique keys
                if (!target.doi) {
                    target.doi = getDOI(source)[0]
                }
            } else if (key == "author") {
                //TODO author
            } else if (key == "pdf") {
                //TODO pdf
            }
        }
    }
    return target;
}
export const checkIApiPaper = (paper: { [index: string]: any }): boolean => {
    for (let i in paper) {
        if (!["id", "uniqueId", "source", "author", "pdf", "numberOfCitations", "numberOfReferences"].includes(i)) {

            if (paper[i] && paper[i].length > 1) {
                logger.critical(`${i} wasn't single`)
                return false;
            }
        }
    }
    return true;
}