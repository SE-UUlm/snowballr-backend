import { Context, send } from "https://deno.land/x/oak/mod.ts";
import { makeErrorMessage } from "../helper/error.ts";
import { jsonBodyToObject } from "../helper/body.ts";
import { Project } from "../model/db/project.ts";
import { UserIsPartOfProject } from "../model/db/userIsPartOfProject.ts";
import { ProjectMembersMessage } from "../model/messages/projectMembers.message.ts";
import { getAllMembersOfProject } from "./databaseFetcher/userProject.ts";
import { convertProjectToProjectMessage } from "../helper/converter/projectConverter.ts";
import { Stage } from "../model/db/stage.ts";
import { Paper } from "../model/db/paper.ts";
import { getAllStagesFromProject } from "./databaseFetcher/stage.ts";
import { checkPaperInProjectStage, getAllPapersFromStage, getPaperByDoi, getProjectPaperID, getProjectPaperScope } from "./databaseFetcher/paper.ts";
import { PapersMessage } from "../model/messages/papersMessage.ts";
import { PaperScopeForStage } from "../model/db/paperScopeForStage.ts";
import { assignOnlyIfUnassignedPaper, checkIApiPaper, convertIApiPaperToDBPaper, convertPapersToPaperMessage, convertPaperToPaperMessage } from "../helper/converter/paperConverter.ts";
import { assign } from "../helper/assign.ts"
import { IApiPaper } from "../api/iApiPaper.ts";
import { getDOI } from "../api/apiMerger.ts";
import { Cache, CacheType } from "../api/cache.ts";
import { logger } from "../api/logger.ts";
import { IApiAuthor } from "../api/iApiAuthor.ts";
import { checkAdmin, checkMemberOfProject, checkPO, checkPOofProject, getPayloadFromJWT, getUserID, UserStatus, validateUserEntry } from "./validation.controller.ts";
import { makeFetching } from "./fetch.controller.ts";
import { saveChildren } from "./database.controller.ts";
import { getPaperCitations, getPaperReferences, getRefOrCiteList, paperUpdate, postPaperCitation, postPaperReference } from "./paper.controller.ts";
import { Criteria } from "../model/db/criteria.ts";
import { Review } from "../model/db/review.ts";
import { ReviewMessage } from "../model/messages/review.message.ts";
import { getAllReviewsFromProjectPaper, getReview } from "./databaseFetcher/review.ts";
import { CriteriaEvaluation } from "../model/db/criteriaEval.ts";
import { writeCSV } from "https://deno.land/x/csv/mod.ts";
import { sortIApiPapersByName, sortPapersByName } from "../../userTests/loggerHelper.ts";
import { Pdf } from "../model/db/pdf.ts";
import { getAllAuthorsFromPaper } from "./databaseFetcher/author.ts";

export const paperCache = new Cache<IApiPaper>(CacheType.F, 0, "paperCache")
export const authorCache = new Cache<IApiAuthor>(CacheType.F, 0, "authorCache")

const reducer = (accumulator: string, currentValue: string) => accumulator + " / " + currentValue;
/**
 * Creates a project
 *
 * @param ctx
 */
export const createProject = async (ctx: Context) => {
    let validate = await validateUserEntry(ctx, [], UserStatus.needsPO, -1, { needed: true, params: ["name", "minCountReviewers", "countDecisiveReviewers", "type", "combinationOfReviewers"] })
    if (validate) {
        try {
            let project = await Project.create({
                name: validate.name,
                minCountReviewers: validate.minCountReviewers,
                countDecisiveReviewers: validate.countDecisiveReviewers,
                combinationOfReviewers: validate.combinationOfReviewers,
                type: validate.type
            })
            if (validate.evaluationFormula) {
                project.evaluationFormula = validate.evaluationFormula;
                await project.update();
            }
            ctx.response.status = 201;
            ctx.response.body = JSON.stringify(project)

        } catch (error) {
            makeErrorMessage(ctx, 422, "unable to process given data")
            return
        }
    }
}



/**
 * Adds a person to a project
 *
 * @param ctx
 * @param id id of project
 */
export const addMemberToProject = async (ctx: Context, id: number) => {
    let validate: any = await validateUserEntry(ctx, [id], UserStatus.needsPOOfProject, id, { needed: true, params: ["id"] })
    if (validate) {
        const params = await jsonBodyToObject(ctx)
        try {
            await UserIsPartOfProject.create({
                isOwner: params.isOwner ? params.isOwner : false,
                userId: validate.id,
                projectId: id
            })
        } catch (error) {
            makeErrorMessage(ctx, 404, "User not found")
            return
        }
        ctx.response.status = 201;
    }

}

/**
 * Removes a member from the project
 * @param ctx 
 * @param projectID 
 * @returns 
 */
export const removeMemberOfProject = async (ctx: Context, projectID: number, userID: number) => {
    let validate: any = await validateUserEntry(ctx, [projectID, userID], UserStatus.needsPOOfProject, projectID, { needed: false, params: [] })
    if (validate) {
        await UserIsPartOfProject.where({ projectId: projectID, userId: userID }).delete()
        ctx.response.status = 200;
    }
}

/**
 * Returns all members, that are currently part of the looked up project.
 * @param ctx 
 * @param id 
 * @returns 
 */
export const getMembersOfProject = async (ctx: Context, id: number) => {
    let validate = await validateUserEntry(ctx, [id], UserStatus.needsMemberOfProject, id, { needed: false, params: [] })
    if (validate) {
        ctx.response.status = 200;
        let message: ProjectMembersMessage = { members: await getAllMembersOfProject(id) }
        ctx.response.body = JSON.stringify(message)
    }
}

/**
 * Gets all projects
 * @param ctx 
 */
export const getProjects = async (ctx: Context) => {
    const payloadJson = await getPayloadFromJWT(ctx);
    if (await checkAdmin(payloadJson)) {
        let projects = await Project.all();
        let projectMessage = await convertProjectToProjectMessage(projects);
        ctx.response.status = 200;
        ctx.response.body = JSON.stringify(projectMessage)
    } else {
        makeErrorMessage(ctx, 401, "not authorized");
    }
}

/**
 * Adds a nextStage to a project
 *
 * @param ctx
 * @param id id of project
 */
export const addStageToProject = async (ctx: Context, id: number) => {
    let validate = await validateUserEntry(ctx, [id], UserStatus.needsPOOfProject, id, { needed: false, params: [] })
    if (validate) {
        const requestParameter = await jsonBodyToObject(ctx)
        const stages = await getAllStagesFromProject(id);

        let stage = await Stage.create({
            name: requestParameter.name ? requestParameter.name : `Stage ${stages.length}`,
            projectId: id,
            number: stages.length
        })

        ctx.response.status = 201;
        ctx.response.body = JSON.stringify(stage)
    }
}

/**
 * Adds a paper to a project Stage 0 and also starts to fetch the info of the paper and the cites and refs
 *
 * @param ctx
 * @param projectID id of project
 * @param stageID id of nextStage
 */
export const addPaperToProjectStage = async (ctx: Context, projectID: number, stageID: number, awaitFetch?: boolean) => {
    let validate = await validateUserEntry(ctx, [projectID, stageID], UserStatus.needsMemberOfProject, projectID, { needed: false, params: [] })
    if (validate) {
        const requestParameter = await jsonBodyToObject(ctx)
        if (!requestParameter.doi && !requestParameter.title) {
            makeErrorMessage(ctx, 422, "to add a paper to a nextStage, at least a DOI or a title is needed")
            return;
        }

        if (awaitFetch) {
            await fetchToDB(stageID, projectID, requestParameter.doi, requestParameter.title, requestParameter.author)
        } else {
            fetchToDB(stageID, projectID, requestParameter.doi, requestParameter.title, requestParameter.author)
        }
        ctx.response.status = 201;
    }
}

/**
 * Saves all paper that were fetched by the APIs to the db and corresponding stage.
 * @param stageID 
 * @param projectID 
 * @param doi 
 * @param title 
 * @param authorName 
 */
const fetchToDB = async (stageID: number, projectID: number, doi?: string, title?: string, authorName?: string) => {

    let fetch = await makeFetching(doi, title, authorName);
    let response = (await fetch.response)

    for (let element of response) {
        if (element) {
            let parent = await savePaper(element.paper)

            PaperScopeForStage.create({ stageId: stageID, paperId: Number(parent.id) })
            let currentStage = await Stage.find(stageID)

            let nextStage: Stage = await findNextStage(currentStage, projectID)

            let allChildren: Promise<Paper>[] = []
            for (let item of element.citations!) {
                allChildren.push(createChildren(item, "citedBy", "papercitingid", "papercitedid", Number(parent.id), Number(nextStage.id)))
            }
            for (let item of element.references!) {
                allChildren.push(createChildren(item, "referencedby", "paperreferencedid", "paperreferencingid", Number(parent.id), Number(nextStage.id)))
            }
            await Promise.all(allChildren)
        }

    }

}
/**
 * Returns next stage of project by either finding it or creating it
 * @param currentStage 
 * @param projectID 
 * @returns 
 */
export const findNextStage = async (currentStage: Stage, projectID: number) => {
    let stages = (await getAllStagesFromProject(projectID))
    let nextStage = stages.filter((item: Stage) => Number(item.number) == Number(currentStage.number) + 1)[0];
    if (!nextStage) {
        nextStage = await Stage.create({
            name: `Stage ${stages.length}`,
            projectId: projectID,
            number: stages.length
        })
    }
    return nextStage
}
/**
 * Saves all cites and refs in the cite and ref table and puts them to their corresponding stage
 * @param item 
 * @param into 
 * @param column1 
 * @param column2 
 * @param firstId 
 * @param nextStageId 
 * @returns 
 */
const createChildren = async (item: IApiPaper, into: string, column1: string, column2: string, firstId: number, nextStageId: number) => {
    let child = await savePaper(item)
    await saveChildren(into, column1, column2, firstId, Number(child.id))
    await PaperScopeForStage.create({ stageId: Number(nextStageId), paperId: Number(child.id) })
    return child;
}


/**
 * Saves a paper to the database.
 * If the paper is already in the database, it will be tried to be merged.
 * Adds leftover values to the paperCache, to be choosen by the user.
 * @param apiPaper 
 * @returns 
 */
const savePaper = async (apiPaper: IApiPaper): Promise<Paper> => {
    let doi = getDOI(apiPaper)

    if (doi[0]) {
        let dbPaper = await getPaperByDoi(doi[0])

        if (dbPaper) {
            await assignOnlyIfUnassignedPaper(dbPaper, apiPaper)
            return dbPaper.update()
        }

    }
    let paper = await convertIApiPaperToDBPaper(apiPaper)

    if (!checkIApiPaper(apiPaper)) {
        paperCache.add(String(paper.id), (apiPaper))
    }
    return paper;

}
/**
 * Gets all papers that are part of the selected stage of a project
 * @param ctx
 * @param projectID
 * @param stageID
 */
export const getPapersOfProjectStage = async (ctx: Context, projectID: number, stageID: number) => {
    let validate = await validateUserEntry(ctx, [projectID, stageID], UserStatus.needsMemberOfProject, projectID, { needed: false, params: [] })
    if (validate) {
        ctx.response.status = 200;
        let message: PapersMessage = { papers: await convertPapersToPaperMessage(await getAllPapersFromStage(stageID), stageID) }
        ctx.response.body = JSON.stringify(message)
    }
}

/**
 * Gets a single paper by using the project specific paper id (not the normal id of a paper!)
 *
 * @param ctx
 * @param projectID
 * @param stageID
 * @param ppID project specific paper id
 */
export const getPaperOfProjectStage = async (ctx: Context, projectID: number, stageID: number, ppID: number) => {
    let validate = await validateUserEntry(ctx, [projectID, stageID, ppID], UserStatus.needsMemberOfProject, projectID, { needed: false, params: [] })
    if (validate) {
        try {
            let paper = await PaperScopeForStage.where("id", ppID).paper();
            if (paper) {
                ctx.response.status = 200;
                ctx.response.body = JSON.stringify(await convertPaperToPaperMessage(paper, stageID));
            }
        } catch (e) {
            makeErrorMessage(ctx, 404, "paper does not exist")
        }
    }
}
/**
 * Patches a paper by its project paper id.
 * @param ctx 
 * @param projectID 
 * @param stageID 
 * @param ppID 
 * @returns 
 */
export const patchPaperOfProjectStage = async (ctx: Context, projectID: number, stageID: number, ppID: number) => {
    let validate = await validateUserEntry(ctx, [projectID, stageID, ppID], UserStatus.needsMemberOfProject, projectID, { needed: false, params: [] })
    if (validate) {
        try {
            let pp = await PaperScopeForStage.where("id", ppID);
            if(validate.finalDecision){
                let paperScope = (await pp.get())
                if(Array.isArray(paperScope)){
                    paperScope[0].finalDecision = validate.finalDecision}
                }
            let paper = await pp.paper();
            if (paper) {
                await paperUpdate(ctx, paper)
            }
        } catch (e) {
            makeErrorMessage(ctx, 404, "paper does not exist")
        }
    }
}

/**
 * Removes a paper from a project stage (not the paper itself)
 * @param ctx 
 * @param projectID 
 * @param stageID 
 * @param ppID 
 */
export const deletePaperOfProjectStage = async (ctx: Context, projectID: number, stageID: number, ppID: number) => {
    let validate = await validateUserEntry(ctx, [projectID, stageID, ppID], UserStatus.needsMemberOfProject, projectID, { needed: false, params: [] })
    if (validate) {
        await PaperScopeForStage.deleteById(ppID)
        ctx.response.status = 200;
    }
}


export const makeRefCiteCsv = async (ctx: Context, projectID: number, stageID: number, ppID: number) => {
    let validate = await validateUserEntry(ctx, [projectID, stageID, ppID], UserStatus.needsMemberOfProject, projectID, { needed: false, params: [] })
    if (validate) {
        try {
            let references = getRefs(ctx, projectID, stageID, ppID)
            let citations = getCites(ctx, projectID, stageID, ppID)

            let finRef = await references;
            let finCite = await citations;
            if (finRef && finCite) {
                let finishedRefs = (await Promise.all(finRef)).sort(sortPapersByName)
                let finishedCites = (await Promise.all(finCite)).sort(sortPapersByName)
                let paper = await PaperScopeForStage.where("id", ppID).paper();
                let rows = [["authors", "title", "year", "publisher", "link", "doi"], [], ["References"], []]

                rows = await papersToRow(finishedRefs, rows, false);

                rows.push([], ["Citations"], [])
                rows = await papersToRow(finishedCites, rows, false);
                const f = await Deno.open(`./${String(paper.title).replaceAll(" ", "_") + ".csv"}`, { write: true, create: true, truncate: true });

                await writeCSV(f, rows);
                f.close();
                const text = await Deno.readTextFile(String(paper.title).replaceAll(" ", "_") + ".csv");
                ctx.response.status = 200;
                ctx.response.type = "text/csv";
                ctx.response.body = text;
                ctx.response.headers.set('Content-disposition', `attachment; filename=${String(paper.title).replaceAll(" ", "_")}.csv`);
            }
        } catch (err) {
            console.log(err)
        }
    }
}

export const makeStageCsv = async (ctx: Context, projectID: number, stageID: number) =>{
    let validate = await validateUserEntry(ctx, [projectID, stageID], UserStatus.needsMemberOfProject, projectID, { needed: false, params: [] })
        if (validate) {
        let papers = await getAllPapersFromStage(stageID)
        let rows = [["authors", "title", "year", "publisher", "link", "doi"]]
        let project = await Project.find(projectID);
        let stage = await Stage.find(stageID);
        for(let i = 1; i <= (Number(project.countDecisiveReviewers));i++){
            rows[0].push(`SuggestedInclusion${i}`)
        }
        rows[0].push("FinalDecision")
        rows = await papersToRow(papers, rows, true, project, stageID)
        const f = await Deno.open(`./${String(project.name)}_Stage${Number(stage.number)}.csv`, { write: true, create: true, truncate: true });

        await writeCSV(f, rows);
        f.close();
        const text = await Deno.readTextFile(`./${String(project.name)}_Stage${Number(stage.number)}.csv`);
        ctx.response.status = 200;
        ctx.response.type = "text/csv";
        ctx.response.body = text;
        ctx.response.headers.set('Content-disposition', `attachment; filename=${String(project.name)}_Stage${Number(stage.number)}.csv`);
    }
}

const papersToRow = async (papers: Paper[], rows: string[][], getReviews: boolean, project?: Project, stageID?: number) => {
        for (let item of papers) {

            let link = ""
            if (item.doi) {
                link = `=HYPERLINK("https://doi.org/${String(item.doi)}")`
            } else {
                let pdfs = await Pdf.where("paperId", Number(item.id)).get()
                if (Array.isArray(pdfs) && pdfs[0]) {
                    link = `=HYPERLINK("${String(pdfs[0].url)}")`
                }
            }
            let authors = (await getAllAuthorsFromPaper(Number(item.id))).map(item => String(item.rawString))
            let row = [
                authors.length > 0 ? authors.reduce(reducer) : "",
                item.title ? String(item.title) : "",
                item.year ? String(item.year) : "",
                item.publisher ? String(item.publisher) : "",
                link,
                item.doi ? String(item.doi) : ""
            ]
            if(getReviews && project && stageID){
                let ppID = await getProjectPaperScope(stageID, Number(item.id))
                if(ppID){
                    let reviews = await Review.where(Review.field("paper_id"), Number(ppID.id)).get()

                    if(Array.isArray(reviews)){
                    for(let review of reviews){
                        row.push(review.overallEvaluation? String(review.overallEvaluation): "")
                    }

                    for(let i = 0; i < (Number(project.countDecisiveReviewers)- reviews.length);i++){
                        row.push("")
                    }

                    row.push(ppID.finalDecision? String(ppID.finalDecision): "")
            }
            }

            }
            rows.push(row)
        }
    return rows
}
/**
 * Puts all citations of a paper by the project specific paper id into a message
 * @param ctx
 * @param projectID
 * @param stageID
 * @param ppID
 */
export const getCitationsOfProjectPaper = async (ctx: Context, projectID: number, stageID: number, ppID: number) => {
    let validate = await validateUserEntry(ctx, [projectID, stageID, ppID], UserStatus.needsMemberOfProject, projectID, { needed: false, params: [] })
    if (validate) {
        let citations = await getCites(ctx, projectID, stageID, ppID)
        if (citations) {
            ctx.response.status = 200;
            let message: PapersMessage = { papers: await convertPapersToPaperMessage(await Promise.all(citations)) }
            ctx.response.body = JSON.stringify(message)
        }
    }
}
/**
 * Returns all citations of a paper by the project specific paper id
 * @param ctx 
 * @param projectID 
 * @param stageID 
 * @param ppID 
 */
export const getCites = async (ctx: Context, projectID: number, stageID: number, ppID: number) => {

    try {
        let paper = await PaperScopeForStage.where("id", ppID).paper();
        if (paper) {
            let papers: Promise<Paper>[] = await getRefOrCiteList(ctx, "citedBy", "papercitingid", "papercitedid", Number(paper.id))
            let nextStage = await findNextStage(await Stage.find(stageID), projectID)
            for (let i = 0; i < papers.length; i++) {
                if (!await checkPaperInProjectStage(await papers[i], Number(nextStage.id))) {
                    delete papers[i]
                }
            }
            return papers.filter(item => item)

        }
    } catch (e) {
        makeErrorMessage(ctx, 404, "paper does not exist")
    }

}

/**
 * Puts all references of a paper by the project specific paper id into a message
 * @param ctx
 * @param projectID
 * @param stageID
 * @param ppID
 */
export const getReferencesOfProjectPaper = async (ctx: Context, projectID: number, stageID: number, ppID: number) => {
    let validate = await validateUserEntry(ctx, [projectID, stageID, ppID], UserStatus.needsMemberOfProject, projectID, { needed: false, params: [] })
    if (validate) {
        let references = await getRefs(ctx, projectID, stageID, ppID)
        if (references) {
            ctx.response.status = 200;
            let message: PapersMessage = { papers: await convertPapersToPaperMessage(await Promise.all(references)) }
            ctx.response.body = JSON.stringify(message)
        }
    }
}

/**
 * Returns all references of a paper by the project specific paper id
 * @param ctx 
 * @param projectID 
 * @param stageID 
 * @param ppID 
 */
export const getRefs = async (ctx: Context, projectID: number, stageID: number, ppID: number) => {

    try {
        let paper = await PaperScopeForStage.where("id", ppID).paper();
        if (paper) {
            let papers = await getRefOrCiteList(ctx, "referencedby", "paperreferencedid", "paperreferencingid", Number(paper.id))
            let nextStage = await findNextStage(await Stage.find(stageID), projectID)
            for (let i = 0; i < papers.length; i++) {
                if (!await checkPaperInProjectStage(await papers[i], Number(nextStage.id))) {
                    delete papers[i]
                }
            }
            return papers.filter(item => item)

        }

    } catch (e) {
        makeErrorMessage(ctx, 404, "paper does not exist")
    }

}
/**
 * Posts a new cite to a paper and adds it to the project stage
 * @param ctx 
 * @param projectID 
 * @param stageID 
 * @param ppID 
 */
export const postCiteProject = async (ctx: Context, projectID: number, stageID: number, ppID: number) => {
    let validate = await validateUserEntry(ctx, [projectID, stageID, ppID], UserStatus.needsMemberOfProject, projectID, { needed: false, params: [] })
    if (validate) {
        try {
            let paper = await PaperScopeForStage.where("id", ppID).paper();
            let stage = await Stage.find(stageID)
            if (paper && stage) {
                let paper2 = await postPaperCitation(ctx, Number(paper.id))
                if (paper2) {
                    let nextStage = await findNextStage(stage, projectID)
                    await PaperScopeForStage.create({ stageId: Number(nextStage.id), paperId: Number(paper2.id) })
                }
            }
        } catch (e) {
            makeErrorMessage(ctx, 404, "paper does not exist")
        }
    }
}
/**
 *  Posts a new reference to a paper and adds it to the project stage
 * @param ctx 
 * @param projectID 
 * @param stageID 
 * @param ppID 
 */
export const postRefProject = async (ctx: Context, projectID: number, stageID: number, ppID: number) => {
    let validate = await validateUserEntry(ctx, [projectID, stageID, ppID], UserStatus.needsMemberOfProject, projectID, { needed: false, params: [] })
    if (validate) {
        try {
            let paper = await PaperScopeForStage.where("id", ppID).paper();
            let stage = await Stage.find(stageID)
            if (paper && stage) {
                let paper2 = await postPaperReference(ctx, Number(paper.id))
                if (paper2) {
                    let nextStage = await findNextStage(stage, projectID)
                    await PaperScopeForStage.create({ stageId: Number(nextStage.id), paperId: Number(paper2.id) })
                }
            }
        } catch (e) {
            makeErrorMessage(ctx, 404, "paper does not exist")
        }
    }
}


/**
 * Returns all criterias that are used to evaluate a paper of a project
 * @param ctx 
 * @param projectID 
 */
export const getCriteriasOfProject = async (ctx: Context, projectID: number) => {
    let validate = await validateUserEntry(ctx, [projectID], UserStatus.needsMemberOfProject, projectID, { needed: false, params: [] })
    if (validate) {
        let criterias = await Criteria.where({ projectId: projectID }).get()
        if (Array.isArray(criterias)) {
            ctx.response.status = 200;
            ctx.response.body = JSON.stringify({ criterias: criterias })
        }
    }
}
export const getCriteriaOfProject = async (ctx: Context, projectID: number, criteriaId: number) => {
    let validate = await validateUserEntry(ctx, [projectID, criteriaId], UserStatus.needsMemberOfProject, projectID, { needed: false, params: [] })
    if (validate) {
        let criteria = await Criteria.find(criteriaId)
        ctx.response.status = 200;
        ctx.response.body = JSON.stringify(criteria)
    }
}
/**
 * Returns all evaluations of a criteria set by a project
 * @param ctx 
 * @param projectID 
 * @param criteriaId 
 */
export const getCriteriaEvalsOfCriteria = async (ctx: Context, projectID: number, criteriaId: number) => {
    let validate = await validateUserEntry(ctx, [projectID, criteriaId], UserStatus.needsMemberOfProject, projectID, { needed: false, params: [] })
    if (validate) {
        let criteriaEvals = await CriteriaEvaluation.where({ criteriaId: criteriaId }).get()
        if (Array.isArray(criteriaEvals)) {
            ctx.response.status = 200;
            ctx.response.body = JSON.stringify({ criteriaevaluations: criteriaEvals })
        }
    }
}
/**
 * Adds a criteria to a project
 * @param ctx 
 * @param projectID 
 * @returns 
 */
export const addCriteriaToProject = async (ctx: Context, projectID: number) => {
    let validate = await validateUserEntry(ctx, [projectID], UserStatus.needsPOOfProject, projectID, { needed: true, params: ["short", "description", "abbreviation", "inclusionExclusion", "weight"] })
    if (validate) {
        if (!["inclusion", "hard exclusion", "exclusion"].includes(validate.inclusionExclusion)) {
            makeErrorMessage(ctx, 422, "inclusionExclusion must be one of the options: 'inclusion', 'exclusion', 'hard exclusion'")
            return
        }
        if (!Number(validate.weight)) {
            makeErrorMessage(ctx, 422, "weight must be a number")
            return
        }
        try {
            let criteria = await Criteria.create({ projectId: projectID, description: validate.description, short: validate.short, abbreviation: validate.abbreviation, inclusionExclusion: validate.inclusionExclusion, weight: validate.weight })
            ctx.response.status = 201;
            ctx.response.body = JSON.stringify(criteria)
        } catch (err) {

            makeErrorMessage(ctx, 404, "Project not found")
        }
    }
}
/**
 * Patches a criteria of a project
 * @param ctx 
 * @param projectID 
 * @param criteriaID 
 * @returns 
 */
export const patchCriteriaOfProject = async (ctx: Context, projectID: number, criteriaID: number) => {
    let validate = await validateUserEntry(ctx, [projectID, criteriaID], UserStatus.needsMemberOfProject, projectID, { needed: true, params: [] })
    if (validate) {
        if (!Number(validate.weight)) {
            makeErrorMessage(ctx, 422, "weight must be a number")
            return
        }

        delete validate.id;
        let criteria = await Criteria.find(criteriaID)
        if (criteria) {
            Object.assign(criteria, validate)
            await criteria.update()
            ctx.response.status = 200;
            ctx.response.body = JSON.stringify(criteria)
        } else {
            makeErrorMessage(ctx, 404, "criteria not found")
        }
    }
}
/**
 * Deletes a criteria of a project
 * @param ctx 
 * @param projectID 
 * @param criteriaID 
 */
export const deleteCriteriaOfProject = async (ctx: Context, projectID: number, criteriaID: number) => {
    let validate = await validateUserEntry(ctx, [projectID, criteriaID], UserStatus.needsMemberOfProject, projectID, { needed: false, params: [] })
    if (validate) {
        try {
            await Criteria.deleteById(criteriaID)
            ctx.response.status = 200;
        } catch (err) {
            makeErrorMessage(ctx, 403, "Criteria has already been used to evaluate paper. For safety those have to be removed first")
        }
    }
}

/**
 * Returns all reviews of a paper
 * @param ctx 
 * @param projectID 
 * @param stageID 
 * @param ppID 
 */
export const getReviewsOfPaper = async (ctx: Context, projectID: number, stageID: number, ppID: number) => {
    let validate = await validateUserEntry(ctx, [projectID, stageID, ppID], UserStatus.needsMemberOfProject, projectID, { needed: false, params: [] })
    if (validate) {
        ctx.response.status = 200;
        ctx.response.body = JSON.stringify({ reviews: await getAllReviewsFromProjectPaper(ppID) })
    }
}

/**
 * Adds a review to a paper
 * @param ctx 
 * @param projectID 
 * @param stageID 
 * @param ppID 
 */
export const addReviewToPaper = async (ctx: Context, projectID: number, stageID: number, ppID: number) => {
    let validate = await validateUserEntry(ctx, [projectID, stageID, ppID], UserStatus.needsMemberOfProject, projectID, { needed: false, params: [] })
    if (validate) {
        const payloadJson = await getPayloadFromJWT(ctx);
        let userID = await getUserID(payloadJson)
        const params = await jsonBodyToObject(ctx)
        let project = await Project.find(projectID);
        let tresholds=  String(project.combinationOfReviewers).split(",")
        if(tresholds.length !== 2 || isNaN(Number(tresholds[0])) || isNaN(Number(tresholds[1])) || isNaN(Number(tresholds[2]))|| tresholds[2] < tresholds[1] || tresholds[1] < tresholds[0]){
            makeErrorMessage(ctx, 409, "Before a review can happen, the combination of reviewers has to be well formed so a final decision of the paper can be evaluated. This is not the case")
        }
        if (userID) {
            try {
                let review = await Review.create({
                    paperId: ppID,
                    userId: userID,
                    stageId: stageID
                })

                if (params.finished != undefined) { review.finished = params.finished }
                if (params.overallEvaluation) { review.overallEvaluation = params.overallEvaluation }
                if (params.finishDate) { review.finishDate = new Date(params.finishDate) }
                await review.update();
                let reviews = await getAllReviewsFromProjectPaper(ppID);

                ctx.response.status = 201;
                ctx.response.body = JSON.stringify(review)
            } catch (err) {
                makeErrorMessage(ctx, 404, "stage or paper id not found")
            }
        }
    }
}

/**
 * Patches a review of a paper
 * @param ctx 
 * @param projectID 
 * @param stageID 
 * @param ppID 
 * @param reviewID 
 */
export const patchReviewOfPaper = async (ctx: Context, projectID: number, stageID: number, ppID: number, reviewID: number) => {
    let validate = await validateUserEntry(ctx, [projectID, stageID, ppID, reviewID], UserStatus.needsMemberOfProject, projectID, { needed: true, params: [] })
    if (validate) {
        delete validate.id;
        delete validate.userId;
        let review = await Review.find(reviewID)
        if (review) {
            Object.assign(review, validate)
            await review.update()
            ctx.response.status = 200;
            ctx.response.body = JSON.stringify(review)
        } else {
            makeErrorMessage(ctx, 404, "review not found")
        }
    }
}
/**
 * Retuns a review of a paper
 * @param ctx 
 * @param projectID 
 * @param stageID 
 * @param ppID 
 * @param reviewID 
 */
export const getReviewOfPaper = async (ctx: Context, projectID: number, stageID: number, ppID: number, reviewID: number) => {
    let validate = await validateUserEntry(ctx, [projectID, stageID, ppID, reviewID], UserStatus.needsMemberOfProject, projectID, { needed: false, params: [] })
    if (validate) {
        let review = await getReview(reviewID)
        if (review) {
            ctx.response.status = 200;
            ctx.response.body = JSON.stringify(review[0])
        } else {
            makeErrorMessage(ctx, 404, "review not found")
        }
    }
}
/**
 * Deletes a review of a paper
 * @param ctx 
 * @param projectID 
 * @param stageID 
 * @param ppID 
 * @param reviewID 
 */
export const deleteReviewOfPaper = async (ctx: Context, projectID: number, stageID: number, ppID: number, reviewID: number) => {
    let validate = await validateUserEntry(ctx, [projectID, stageID, ppID], UserStatus.needsMemberOfProject, projectID, { needed: false, params: [] })
    if (validate) {
        try {
            await Review.deleteById(reviewID)
            ctx.response.status = 200;
        } catch (err) {
            makeErrorMessage(ctx, 403, "Review has already been used to evaluate paper. For safety those have to be removed first")
        }
    }
}



/**
 * Retuns all criteria evaluations of a review
 * @param ctx 
 * @param projectID 
 * @param stageID 
 * @param ppID 
 * @param reviewID 
 */
export const getCrtieriaEvalsOfReview = async (ctx: Context, projectID: number, stageID: number, ppID: number, reviewID: number) => {
    let validate = await validateUserEntry(ctx, [projectID, stageID, ppID, reviewID], UserStatus.needsMemberOfProject, projectID, { needed: false, params: [] })
    if (validate) {
        ctx.response.status = 200;
        ctx.response.body = JSON.stringify({ criteriaevaluations: await CriteriaEvaluation.where(CriteriaEvaluation.field("review_id"), Number(reviewID)).get() })
    }
}
/**
 * Adds a evaluation of a criteria to a review
 * @param ctx 
 * @param projectID 
 * @param stageID 
 * @param ppID 
 * @param reviewID 
 */
export const addCrtieriaEvalToReview = async (ctx: Context, projectID: number, stageID: number, ppID: number, reviewID: number) => {
    let validate = await validateUserEntry(ctx, [projectID, stageID, ppID, reviewID], UserStatus.needsSameMemberOfProject, projectID, { needed: true, params: ["value", "criteriaId"] })
    if (validate) {
        if (!Number(validate.criteriaId)) {
            makeErrorMessage(ctx, 422, "criteriaId must be number")
        }
        try {
            let criteria = await CriteriaEvaluation.create({
                criteriaId: validate.criteriaId,
                reviewId: reviewID,
                value: validate.value
            })
            ctx.response.status = 201;
            ctx.response.body = JSON.stringify(criteria)
        } catch (err) {
            makeErrorMessage(ctx, 404, "review or criteria not found")
        }
    }
}

/**
 * Patches a evaluation of a criteria of a review
 * @param ctx 
 * @param projectID 
 * @param stageID 
 * @param ppID 
 * @param reviewID 
 * @param criteriaEvalID 
 */
export const patchCritieriaEvalOfReview = async (ctx: Context, projectID: number, stageID: number, ppID: number, reviewID: number, criteriaEvalID: number) => {
    let validate = await validateUserEntry(ctx, [projectID, stageID, ppID, reviewID, criteriaEvalID], UserStatus.needsSameMemberOfProject, projectID, { needed: true, params: [] })
    if (validate) {
        delete validate.id;
        let criteriaeval = await CriteriaEvaluation.find(criteriaEvalID)
        if (criteriaeval) {
            Object.assign(criteriaeval, validate)
            await criteriaeval.update()
            ctx.response.status = 200;
            ctx.response.body = JSON.stringify(criteriaeval)
        } else {
            makeErrorMessage(ctx, 404, "review not found")
        }
    }
}

/**
 * Retuns a evaluation of a criteria of a review
 * @param ctx 
 * @param projectID 
 * @param stageID 
 * @param ppID 
 * @param reviewID 
 * @param criteriaEvalID 
 */
export const getCritieriaEvalOfReview = async (ctx: Context, projectID: number, stageID: number, ppID: number, reviewID: number, criteriaEvalID: number) => {
    let validate = await validateUserEntry(ctx, [projectID, stageID, ppID, reviewID, criteriaEvalID], UserStatus.needsMemberOfProject, projectID, { needed: false, params: [] })
    if (validate) {
        let criteriaeval = await CriteriaEvaluation.find(criteriaEvalID)
        if (criteriaeval) {
            ctx.response.status = 200;
            ctx.response.body = JSON.stringify(criteriaeval)
        } else {
            makeErrorMessage(ctx, 404, "review not found")
        }
    }
}
/**
 * Deletes a evaluation of a criteria of a review
 * @param ctx 
 * @param projectID 
 * @param stageID 
 * @param ppID 
 * @param reviewID 
 * @param criteriaEvalID 
 */
export const deleteCritieriaEvalOfReview = async (ctx: Context, projectID: number, stageID: number, ppID: number, reviewID: number, criteriaEvalID: number) => {
    let validate = await validateUserEntry(ctx, [projectID, stageID, ppID, reviewID, criteriaEvalID], UserStatus.needsSameMemberOfProject, projectID, { needed: false, params: [] })
    if (validate) {
        await Review.deleteById(criteriaEvalID)
        ctx.response.status = 200;
    }
}
export const getApis = async (ctx: Context, projectID: number) => {

}
