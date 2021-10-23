import { Paper } from "../../model/db/paper.ts";
import { PaperMessage, Status } from "../../model/messages/papersMessage.ts";
import { checkUniqueVal, getProjectPaperID, getProjectPaperScope } from "../../controller/databaseFetcher/paper.ts";
import { assign, concatWithoutDuplicates } from "../assign.ts"
import { IApiPaper } from "../../api/iApiPaper.ts"
import { getDOI } from "../../api/apiMerger.ts";
import { logger, fileLogger } from "../../api/logger.ts";
import { PaperID } from "../../model/db/paperID.ts";
import { PaperHasID } from "../../model/db/paperHasID.ts";
import { IApiUniqueId, idType } from "../../api/iApiUniqueId.ts";
import { Author } from "../../model/db/author.ts";
import { Wrote } from "../../model/db/wrote.ts";
import { Pdf } from "../../model/db/pdf.ts";
import { authorCache, paperCache } from "../../controller/project.controller.ts";
import { checkIApiAuthor } from "./authorConverter.ts";
import { getAllAuthorsFromPaper } from "../../controller/databaseFetcher/author.ts";
import { IApiAuthor } from "../../api/iApiAuthor.ts";
import { isEqualAuthor } from "../../api/checkIsEqual.ts";
import { convertAuthorToAuthorMessage } from "./authorConverter.ts"
import { checkUserReviewOfProjectPaper, getAllReviewsFromProjectPaper } from "../../controller/databaseFetcher/review.ts";
import { PaperScopeForStage } from "../../model/db/paperScopeForStage.ts";
import { AuthorMessage } from "../../model/messages/author.message.ts";

export const convertPapersToPaperMessage = async (papers: Paper[], stageId?: number, userId?: number) => {
    let paperMessages: PaperMessage[] = [];
    for (const item of papers) {
        let paperMessage: PaperMessage = await convertPaperToPaperMessage(item, stageId, userId)
        paperMessages.push(paperMessage)
    }
    return paperMessages;
}

export const convertRowsToPaperMessage = (answer: any, userID: number, paperCacheUrls: string[]) => {

    let paperMessage: PaperMessage[] = []
    let lastId = -1;
    for (let element of answer) {
        if (lastId == Number(element[0])) {
            let paper = paperMessage[paperMessage.length - 1]
            if (element[12]) {
                paper.pdf = concatWithoutDuplicates(paper.pdf, [element[12]])
            }
            if (element[13]) {
                if (!paper.authors.some(author => author.id == Number(element[13]))) {
                    paper.authors.push({
                        id: element[13] ? Number(element[13]) : undefined,
                        firstName: element[15] ? String(element[15]) : undefined,
                        lastName: element[16] ? String(element[16]) : undefined,
                        rawString: element[14] ? String(element[14]) : undefined,
                        orcid: element[17] ? String(element[17]) : undefined,
                    })
                }
            }
            if (paper.status == "partiallyEvaluated" && element[19] && Number(element[19]) == userID) {
                paper.status = "evaluatedByMyself"
            }

        } else {
            lastId = Number(element[0])
            let author: AuthorMessage = {
                id: element[13] ? Number(element[13]) : undefined,
                firstName: element[15] ? String(element[15]) : undefined,
                lastName: element[16] ? String(element[16]) : undefined,
                rawString: element[14] ? String(element[14]) : undefined,
                orcid: element[17] ? String(element[17]) : undefined,

            }

            let paper: PaperMessage = {
                id: Number(element[3]),
                doi: element[4] ? String(element[4]) : undefined,
                title: element[5] ? String(element[5]) : undefined,
                abstract: element[6] ? String(element[6]) : undefined,
                year: element[7] ? Number(element[7]) : undefined,
                publisher: element[8] ? String(element[8]) : undefined,
                type: element[9] ? String(element[9]) : undefined,
                scope: element[10] ? String(element[10]) : undefined,
                scopeName: element[11] ? String(element[11]) : undefined,
                ppid: Number(element[0]),
                finalDecision: element[1] ? String(element[1]) : undefined,
                authors: author.id ? [author] : [],
                pdf: element[12] ? [String(element[12])] : []
            }

            if (paperCacheUrls.find(el => el == (String(paper.id)))) {
                paper.status = "unfinished"
            } else if (paper.finalDecision) {
                paper.status = "completelyEvaluated"
            } else if (element[19] && Number(element[19]) == userID) {
                paper.status = "evaluatedByMyself"
            } else if (paper.ppid && element[18]) {
                paper.status = "partiallyEvaluated"
            } else {
                paper.status = "ready"
            }



            paperMessage.push(paper)
        }


    }

    return paperMessage

}
export const convertPaperToPaperMessage = async (paper: Paper, stageId?: number, userId?: number) => {
    let paperMessage: PaperMessage = { id: Number(paper.id), pdf: [], authors: [] }
    let pp: PaperScopeForStage | undefined;
    if (stageId) {
        pp = await getProjectPaperScope(stageId, Number(paper.id))
        if (pp) {
            paperMessage.ppid = Number(pp.id)
            if (pp.finalDecision) { paperMessage.finalDecision = String(pp.finalDecision) }
        }
    }
    if (paperCache.has(String(paper.id))) {
        paperMessage.status = Status.unfinished
    } else if (pp && pp.finalDecision) {
        paperMessage.status = Status.completelyEvaluated
    } else if (userId && paperMessage.ppid && await checkUserReviewOfProjectPaper(paperMessage.ppid, userId)) {
        paperMessage.status = Status.evaluatedByMyself
    } else if (paperMessage.ppid && (await getAllReviewsFromProjectPaper(paperMessage.ppid)).length > 0) {
        paperMessage.status = Status.partiallyEvaluated
    } else {
        paperMessage.status = Status.ready
    }
    let pdf = await Pdf.where({ paperId: Number(paper.id) }).get()
    if (Array.isArray(pdf)) {
        pdf.forEach(url => {
            paperMessage.pdf.push(String(url.url))
        })
    }
    let authors = await getAllAuthorsFromPaper(Number(paper.id))
    authors.forEach(author => paperMessage.authors.push(convertAuthorToAuthorMessage(author)))
    let unfinishedAuthors = paperMessage.authors.some(author => author.status === Status.unfinished)
    if (paperMessage.authors.length > 0 && unfinishedAuthors) {
        paperMessage.status = Status.unfinished
    }
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
            addUniqueToPaper(uniqueId, Number(newPaper.id))

        } else if (i == "author") {
            await addAuthorToDatabase(paper, newPaper)
        } else if (i == "pdf") {
            paper.pdf.forEach(item => {
                Pdf.create({ paperId: Number(newPaper.id), url: item })
            })
        }
    }
    return newPaper.update()
}

export const convertDBPaperToIApiPaper = async (paper: Paper): Promise<IApiPaper> => {
    let pdfs: string[] = []
    let pdf = await Pdf.where({ paperId: Number(paper.id) }).get()
    if (Array.isArray(pdf)) {
        pdf.forEach(url => {
            pdfs.push(String(url.url))
        })
    }
    let authors: IApiAuthor[] = [];
    let dbAuthors = await getAllAuthorsFromPaper(Number(paper.id))
    dbAuthors.forEach(author => {
        authors.push({
            orcid: author.orcid ? [String(author.orcid)] : [],
            firstName: author.firstName ? [String(author.firstName)] : [],
            lastName: author.lastName ? [String(author.lastName)] : [],
            rawString: author.rawString ? [String(author.rawString)] : []
        })
    })
    return {
        title: paper.title ? [String(paper.title)] : [],
        abstract: paper.abstract ? [String(paper.abstract)] : [],
        numberOfCitations: [],
        numberOfReferences: [],
        year: paper.year ? [Number(paper.year)] : [],
        type: paper.type ? [String(paper.type)] : [],
        scope: paper.scope ? [String(paper.scope)] : [],
        scopeName: paper.scopeName ? [String(paper.scopeName)] : [],
        publisher: paper.publisher ? [String(paper.publisher)] : [],
        uniqueId: paper.doi ? [{ type: idType.DOI, value: String(paper.doi) }] : [],
        source: [],
        raw: [],
        pdf: pdfs,
        author: authors




    }
}
/**
 * First tries to find if an author is already there with the same orcid
 * Otherwise it will not be set as same author since author names can be equal but not the same person
 * It just makes a new entry in the database then
 * @param paper 
 * @param newPaper 
 */
const addAuthorToDatabase = async (paper: IApiPaper, newPaper: Paper) => {
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
}
export const assignOnlyIfUnassignedPaper = async (target: Paper, source: IApiPaper) => {
    try {
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
                    target = updateUniqueIDsOfPaper(target, source)
                } else if (key == "author") {
                    target = await updateAuthorsOfPaper(target, source)
                } else if (key == "pdf") {
                    target = await updatePdfOfPaper(target, source)
                }
            }
        }
        return target;
    } catch (error) {
        logger.error(error)
    }
}

/**
 * Updates the uniqueids of a paper by first checking if they are already in the database.
 * If not, they will be put into it.
 * @param target 
 * @param source 
 * @returns 
 */
const updateUniqueIDsOfPaper = (target: Paper, source: IApiPaper) => {
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
    addUniqueToPaper(uniqueId, Number(target.id))
    return target;
}
/**
 * Updates all authors of a paper by first getting them all, iterating over those already in the database by comparing with found entries.
 * If the entries look similar, the author gets updated values where none are set.
 * If no entry looks similar, a new author will be put in the database.
 * @param target 
 * @param source 
 */
const updateAuthorsOfPaper = async (target: Paper, source: IApiPaper) => {
    let authors = await getAllAuthorsFromPaper(Number(target.id))
    for (let element of authors) {
        let iAuthor: IApiAuthor = {
            orcid: element.orcid ? [String(element.orcid)] : [],
            rawString: element.rawString ? [String(element.rawString)] : [],
            lastName: element.lastName ? [String(element.lastName)] : [],
            firstName: element.firstName ? [String(element.firstName)] : []
        }
        source.author = source.author.filter(value => Object.keys(value).length !== 0);
        for (let i = 0; i < source.author.length; i++) {
            //TODO hardcoded
            if (isEqualAuthor(iAuthor, source.author[i]) > 0.9) {
                source.author = source.author.slice(0, i).concat(source.author.slice(i + 1))
                break;
            }
        }
        source.author = source.author.filter(value => Object.keys(value).length !== 0);
        for (let item of source.author) {
            await updateAuthorOfPaper(await Author.create({}), item, Number(target.id))
        }
    }
    return target
}

/**
 * Gets the pdf link list of the source paper, then compares all those links with the new ones and only makes a new database entry for unknown links
 * @param target 
 * @param source 
 */
const updatePdfOfPaper = async (target: Paper, source: IApiPaper) => {
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
    return target;
}
/**
 * Adds a new found unique id to the database
 * @param uniqueId 
 * @param newPaperId 
 */
const addUniqueToPaper = async (uniqueId: IApiUniqueId[], newPaperId: number) => {
    for (let item of uniqueId) {
        let paperId = await PaperID.create({});
        if (item.type) { paperId.type = item.type }
        if (item.value) { paperId.value = item.value }
        PaperHasID.create({ paperId: newPaperId, paperidId: Number(paperId.id) })
    }
}

/**
 * Updates the authorlist of a paper. 
 * @param author 
 * @param item 
 * @param paperId 
 */
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

            if ((paper[i] && paper[i].length > 1) || (i === "raw" && paper[i].length > 0)) {
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
