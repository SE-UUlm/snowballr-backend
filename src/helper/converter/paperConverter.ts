import { Paper } from "../../model/db/paper.ts";
import { PaperMessage, Status } from "../../model/messages/papersMessage.ts";
import { checkUniqueVal, getProjectPaperID } from "../../controller/databaseFetcher/paper.ts";
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
import { authorCache, paperCache } from "../../controller/project.controller.ts";
import { checkIApiAuthor } from "./authorConverter.ts";
import { getAllAuthorsFromPaper } from "../../controller/databaseFetcher/author.ts";
import { IApiAuthor } from "../../api/iApiAuthor.ts";
import { isEqualAuthor } from "../../api/checkIsEqual.ts";
import { convertAuthorToAuthorMessage } from "./authorConverter.ts"
export const convertPapersToPaperMessage = async (papers: Paper[], stageId?: number) => {
    let paperMessages: PaperMessage[] = [];
    for (const item of papers) {
        let paperMessage: PaperMessage = await convertPaperToPaperMessage(item, stageId)
        paperMessages.push(paperMessage)
    }
    return paperMessages;
}

export const convertPaperToPaperMessage = async (paper: Paper, stageId?: number) => {
    let paperMessage: PaperMessage = { id: Number(paper.id), pdf: [], authors: [] }
    if (stageId) { paperMessage.ppid = await getProjectPaperID(stageId, Number(paper.id)) }
    if (paperCache.has(String(paper.id))) {
        paperMessage.status = Status.unfinished
    } else {
        paperMessage.status = Status.finished
    }
    let pdf = await Pdf.where({ paperId: Number(paper.id) }).get()
    if (Array.isArray(pdf)) {
        pdf.forEach(url => {
            paperMessage.pdf.push(String(url.url))
        })
    }
    let authors = await getAllAuthorsFromPaper(Number(paper.id))
    authors.forEach(author => paperMessage.authors.push(convertAuthorToAuthorMessage(author)))
    assign(paperMessage, paper)
    return paperMessage;
}

export const convertIApiPaperToDBPaper = async (paper: IApiPaper): Promise<Paper> => {

    let newPaper = await Paper.create({})
    let p: any = paper;
    for (let i in paper) {
        if (["title", "abstract", "publisher", "type", "scope", "scopeName", "year"].includes(i)) {
            newPaper[i] = p[i][0]
        } else if (i == "uniqueId") {
            newPaper.doi = getDOI(paper)[0]
            let uniqueId = paper.uniqueId.filter((item: IApiUniqueId) => item.type !== "DOI")
            updateUniqueIdOfPaper(uniqueId, Number(newPaper.id))

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
                await updateAuthorOfPaper(author, item, Number(newPaper.id))
            }

        } else if (i == "pdf") {
            paper.pdf.forEach(item => {
                Pdf.create({ paperId: Number(newPaper.id), url: item })
            })
        }
    }
    return newPaper.update()
}


export const assignOnlyIfUnassignedPaper = async (target: Paper, source: IApiPaper) => {
    let s = <any>source
    for (const key in source) {
        const val = s[key];
        if (val && !target[key]) {
            if (["title", "abstract", "publisher", "type", "scope", "scopeName", "year"].includes(key)) {
                target[key] = s[key][0]
                if (s[key].length <= 1) {
                    delete s[key]
                }
            } else if (key == "uniqueId") {
                if (!target.doi) {
                    target.doi = getDOI(source)[0]
                }
                let uniqueId = source.uniqueId.filter((item: IApiUniqueId) => item.type !== "DOI")
                for (let i = 0; i < uniqueId.length; i++) {
                    if (uniqueId[i].type && uniqueId[i].value && checkUniqueVal(String(uniqueId[i].type), String(uniqueId[i].value))) {
                        delete uniqueId[i];
                    }
                }
                uniqueId = uniqueId.filter(item => item);
                updateUniqueIdOfPaper(uniqueId, Number(target.id))

            } else if (key == "author") {
                let authors = await getAllAuthorsFromPaper(Number(target.id))
                for (let element of authors) {
                    let iAuthor: IApiAuthor = {
                        orcid: element.orcid ? [String(element.orcid)] : [],
                        rawString: element.rawString ? [String(element.rawString)] : [],
                        lastName: element.lastName ? [String(element.lastName)] : [],
                        firstName: element.firstName ? [String(element.firstName)] : []
                    }
                    for (let i = 0; i < source.author.length; source.author) {
                        //TODO hardcoded
                        if (isEqualAuthor(iAuthor, source.author[i]) > 0.9) {
                            source.author = source.author.slice(0, i).concat(source.author.slice(i + 1))
                            break;
                        }
                    }
                    for (let item of source.author) {
                        await updateAuthorOfPaper(await Author.create({}), item, Number(target.id))
                    }
                }
            } else if (key == "pdf") {
                let pdf = Pdf.where({ paperId: Number(target.id) }).get();
                if (Array.isArray(pdf)) {
                    let urls: string[] = []
                    pdf.forEach(item => {
                        urls.push(String(item.url))
                    })
                    for (let item of source.pdf) {
                        if (!urls.includes(item)) {
                            await Pdf.create({ paperId: Number(target.id), url: item })
                        }
                    }
                }
            }
        }
    }
    return target;
}

const updateUniqueIdOfPaper = async (uniqueId: IApiUniqueId[], newPaperId: number) => {
    for (let item of uniqueId) {
        let paperId = await PaperID.create({});
        if (item.type) { paperId.type = item.type }
        if (item.value) { paperId.value = item.value }
        PaperHasID.create({ paperId: newPaperId, paperidId: Number(paperId.id) })
    }
}
const updateAuthorOfPaper = async (author: Author, item: IApiAuthor, paperId: number) => {
    if (!author.firstName) { author.firstName = item.firstName[0] }
    if (!author.lastName) { author.lastName = item.lastName[0] }
    if (!author.rawString) { author.rawString = item.rawString[0] }
    await author.update()
    Wrote.create({ paperId: Number(paperId), authorId: Number(author.id) })
    if (!checkIApiAuthor(item)) {
        authorCache.add(String(author.id), item)
    }
}
export const checkIApiPaper = (paper: { [index: string]: any }): boolean => {
    let check = true;
    for (let i in paper) {
        if (i.includes("Source")) {
            continue;
        }

        if (!["id", "uniqueId", "source", "author", "pdf", "numberOfCitations", "numberOfReferences"].includes(i)) {

            if (paper[i] && paper[i].length > 1) {
                check = false;
            } else {
                delete paper[i]
                delete paper[i + "Source"]
            }
        } else {
            delete paper[i]
            delete paper[i + "Source"]
        }
    }
    return check;
}
