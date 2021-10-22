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
import { checkPaperInProjectStage, getAllPaperMessagesJoin, getAllPapersFromProject, getAllPapersFromStage, getPaperByDoi, getProjectPaperID, getProjectPaperScope } from "./databaseFetcher/paper.ts";
import { PapersMessage } from "../model/messages/papersMessage.ts";
import { PaperScopeForStage } from "../model/db/paperScopeForStage.ts";
import { assignOnlyIfUnassignedPaper, checkIApiPaper, convertDBPaperToIApiPaper, convertIApiPaperToDBPaper, convertPapersToPaperMessage, convertPaperToPaperMessage, convertRowsToPaperMessage } from "../helper/converter/paperConverter.ts";
import { assign, isEqual } from "../helper/assign.ts"
import { IApiPaper, SourceApi } from "../api/iApiPaper.ts";
import { ApiMerger, getDOI } from "../api/apiMerger.ts";
import { Cache, CacheType } from "../api/cache.ts";
import { logger } from "../api/logger.ts";
import { IApiAuthor } from "../api/iApiAuthor.ts";
import { checkAdmin, checkMemberOfProject, checkPO, checkPOofProject, getPayloadFromJWTHeader, getUserID, UserStatus, validateUserEntry } from "./validation.controller.ts";
import { comparisonWeight, makeFetching } from "./fetch.controller.ts";
import { getProjectStageStuff, saveChildren } from "./database.controller.ts";
import { getPaperCitations, getPaperReferences, getRefOrCiteList, paperUpdate, postPaperCitation, postPaperReference } from "./paper.controller.ts";
import { Criteria } from "../model/db/criteria.ts";
import { Review } from "../model/db/review.ts";
import { ReviewMessage } from "../model/messages/review.message.ts";
import { checkUserReviewOfProjectPaper, getAllReviewsFromProjectPaper, getReview } from "./databaseFetcher/review.ts";
import { CriteriaEvaluation } from "../model/db/criteriaEval.ts";
import { writeCSV } from "https://deno.land/x/csv/mod.ts";
import { sortIApiPapersByName, sortPapersByName } from "../helper/loggerHelper.ts";
import { Pdf } from "../model/db/pdf.ts";
import { getAllAuthorsFromPaper } from "./databaseFetcher/author.ts";
import { ProjectUsesApi } from "../model/db/projectUsesApi.ts";
import { IDOfApi } from "../helper/setup.ts";
import { SearchApi } from "../model/db/searchApi.ts";
import {
    compress,
    decompress
} from "https://deno.land/x/zip/mod.ts";
import { isEqualPaper } from "../api/checkIsEqual.ts";
import { IComparisonWeight } from "../api/iComparisonWeight.ts";
import { Semaphore } from "https://deno.land/x/semaphore/mod.ts"

export const paperCache = new Cache<IApiPaper>(CacheType.F, 0, "paperCache")
export const authorCache = new Cache<IApiAuthor>(CacheType.F, 0, "authorCache")

const reducer = (accumulator: string, currentValue: string) => accumulator + " / " + currentValue;

const fetchSemaphore = new Semaphore(1);
/**
 * Creates a project
 *
 * @param ctx
 */
export const createProject = async (ctx: Context) => {
    let validate = await validateUserEntry(ctx, [], UserStatus.needsPO, -1, { needed: true, params: ["name", "minCountReviewers", "countDecisiveReviewers", "type", "combinationOfReviewers"] })
    if (validate) {

        let tresholds = String(validate.combinationOfReviewers).split(",")
        if (tresholds.length !== 2 || isNaN(Number(tresholds[0])) || isNaN(Number(tresholds[1])) || Number(tresholds[1]) < Number(tresholds[0]) || Number(tresholds[0]) < 0 || Number(tresholds[1]) > 10) {
            makeErrorMessage(ctx, 409, "To create a project, the combination of reviewers has to be well formed so a final decision of the paper can be evaluated. This is not the case")
            return;
        }
        tresholds = String(validate.evaluationFormula).split(",")
        if (tresholds.length !== 2 || isNaN(Number(tresholds[0])) || isNaN(Number(tresholds[1])) || Number(tresholds[1]) < Number(tresholds[0]) || Number(tresholds[0]) < -5 || Number(tresholds[1]) > 5) {
            makeErrorMessage(ctx, 409, "To create a project, the evaluation formula has to be well formed so a final decision of the paper can be evaluated. This is not the case")
            return;
        }
        if (validate.type != "both" && validate.type != "forward" && validate.type != "backward") {
            makeErrorMessage(ctx, 409, "The type of a project has to be either 'both', 'forward' or 'backward'")
            return;
        }
        if (validate.mergeThreshold > 1 || validate.mergeThreshold < 0.5) {
            makeErrorMessage(ctx, 409, "The mergeThreshold has to be between 0.5 and 1")
            return;
        }

        try {
            let project = await Project.create({
                name: validate.name,
                minCountReviewers: validate.minCountReviewers,
                countDecisiveReviewers: validate.countDecisiveReviewers,
                combinationOfReviewers: validate.combinationOfReviewers,
                evaluationFormula: validate.evaluationFormula,
                type: validate.type,
                mergeThreshold: validate.mergeThreshold
            })

            await ProjectUsesApi.create({ projectId: Number(project.id), searchapiId: IDOfApi.crossRef, inUse: true })
            await ProjectUsesApi.create({ projectId: Number(project.id), searchapiId: IDOfApi.openCitations, inUse: true })
            await ProjectUsesApi.create({ projectId: Number(project.id), searchapiId: IDOfApi.googleScholar, inUse: false })
            await ProjectUsesApi.create({ projectId: Number(project.id), searchapiId: IDOfApi.IEEE, inUse: true })
            await ProjectUsesApi.create({ projectId: Number(project.id), searchapiId: IDOfApi.semanticScholar, inUse: true })
            await ProjectUsesApi.create({ projectId: Number(project.id), searchapiId: IDOfApi.microsoftAcademic, inUse: true })

            ctx.response.status = 201;
            ctx.response.body = JSON.stringify(project)
        } catch (error) {
            makeErrorMessage(ctx, 422, "given data is not processable")
            return
        }


    }
}

/**
 * Sets the use of an api to true or false
 * @param ctx 
 * @param id 
 */
export const setApiUse = async (ctx: Context, id: number) => {
    let validate = await validateUserEntry(ctx, [], UserStatus.needsPO, -1, { needed: true, params: [] })
    if (validate) {
        if (typeof validate.crossRef === "boolean" &&
            typeof validate.openCitations === "boolean" &&
            typeof validate.googleScholar === "boolean" &&
            typeof validate.IEEE === "boolean" &&
            typeof validate.semanticScholar === "boolean" &&
            typeof validate.microsoftAcademic === "boolean") {
            let poas = await ProjectUsesApi.where({ projectId: id }).get()
            if (Array.isArray(poas)) {
                let items: Promise<ProjectUsesApi>[] = []
                poas.forEach(item => {
                    item.inUse = validate[String(item.name)]
                    items.push(item.update())
                })

                await Promise.all(items)
            }

        } else {
            makeErrorMessage(ctx, 409, "for every api has to be a true/false value set")
        }
    }
    ctx.response.status = 200;
}

/**
 * Gets all the apis of the project
 * @param ctx 
 * @param id 
 */
export const getApis = async (ctx: Context, id: number) => {
    let validate = await validateUserEntry(ctx, [], UserStatus.needsPO, -1, { needed: false, params: [] })
    if (validate) {
        let poas = await ProjectUsesApi.where({ projectId: id }).get()
        ctx.response.status = 200;
        ctx.response.body = JSON.stringify({ apis: poas })
    }
}

/**
 * Replaces current api with a new one for this project
 * @param ctx 
 * @param projectID 
 * @param apiID 
 */
export const replaceApi = async (ctx: Context, projectID: number, apiID: number) => {
    let validate = await validateUserEntry(ctx, [], UserStatus.needsPO, -1, { needed: true, params: ["credentials"] })
    if (validate) {
        let poas = await ProjectUsesApi.where({ projectId: projectID, searchapiId: apiID }).get()
        if (Array.isArray(poas) && poas[0]) {
            let oldApi = await SearchApi.find(Number(poas[0].searchapiId))

            let newApi = await SearchApi.create({
                name: String(oldApi.name),
                credentials: validate.credentials
            })
            await poas[0].delete()
            await ProjectUsesApi.create({ projectId: projectID, searchapiId: Number(newApi.id), inUse: true })
        }
        ctx.response.status = 200;
        ctx.response.body = JSON.stringify({ apis: poas })
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
            makeErrorMessage(ctx, 404, "User or project not found")
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
    const payloadJson = await getPayloadFromJWTHeader(ctx);
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
 * Refetches the paper of a project that have been signed with a final Decision of YES
 * @param ctx 
 * @param projectID 
 */
export const refetchPaperOfProject = async (ctx: Context, projectID: number) => {
    let validate = await validateUserEntry(ctx, [projectID], UserStatus.needsPOOfProject, projectID, { needed: false, params: [] })
    if (validate) {
        let papersPackage = await getAllPapersFromProject(projectID);
        papersPackage = papersPackage.map(items => {
            items.papers = items.papers.filter(paper => String(paper.scope.finalDecision) == "YES")
            return items;
        })
        papersPackage.forEach(paper => {
            let stage = paper.stage;
            paper.papers.forEach(item => {
                let authorName: string | undefined;
                for (let author of item.authors) {
                    if (author.rawString) {
                        authorName = String(author.rawString)
                    }
                }

                fetchToDB(
                    Number(stage.id),
                    projectID,
                    item.paper.doi ? String(item.paper.doi) : undefined,
                    item.paper.title ? String((item.paper).doi) : undefined,
                    authorName,
                    item.paper
                )
            })

        })


        ctx.response.status = 200;
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
const fetchToDB = async (stageID: number, projectID: number, doi?: string, title?: string, authorName?: string, parentPaper?: Paper) => {
    var release = await fetchSemaphore.acquire();
    try {
        let project = await Project.find(projectID)
        let apis: SearchApi[] = [];
        let stage = await Stage.find(stageID)
        let poas = await ProjectUsesApi.where({ projectId: projectID }).get()
        if (Array.isArray(poas)) {
            for (let item of poas) {
                if (item.inUse) {
                    apis.push(await SearchApi.find(Number(item.searchapiId)))
                }
            }
        }
        let sourceApi: [SourceApi, string?][] = (apis).map(item => {
            return [String(item.name) as SourceApi, item.credentials ? String(item.credentials) : undefined]
        })

        console.log("before fetch")
        let fetch = await makeFetching(Number(project.mergeThreshold), sourceApi, doi, title, authorName, String(project.name));
        console.log("after fetch")
        let response = (await fetch.response)


        if (String(project.kind) == "forward") {
            response.forEach(item => {
                item.references = []
            })
        }

        if (String(project.kind) == "backward") {
            response.forEach(item => {
                item.citations = []
            })
        }
        for (let element of response) {
            if (element) {
                let parent: Paper;
                if (parentPaper) {
                    parent = parentPaper
                } else {
                    parent = await savePaper(element.paper, stage, Number(project.mergeThreshold))
                    await PaperScopeForStage.create({ stageId: stageID, paperId: Number(parent.id), finalDecision: "YES" })
                }

                let currentStage = await Stage.find(stageID)

                let nextStage: Stage = await findNextStage(currentStage, projectID)

                let allChildren: Promise<Paper>[] = []
                for (let item of element.citations!) {
                    await createChildren(item, "citedBy", "papercitingid", "papercitedid", Number(parent.id), nextStage, project)
                }
                for (let item of element.references!) {
                    await createChildren(item, "referencedby", "paperreferencedid", "paperreferencingid", Number(parent.id), nextStage, project)
                }

            }
        }

    } catch (err) {
        logger.error(err)
    }
    release();
}

const semaphore = new Semaphore(1);

/**
 * Returns next stage of project by either finding it or creating it
 * @param currentStage 
 * @param projectID 
 * @returns 
 */
export const findNextStage = async (currentStage: Stage, projectID: number) => {
    var release = await semaphore.acquire();
    let stages = (await getAllStagesFromProject(projectID))
    let nextStage = stages.filter((item: Stage) => Number(item.number) == Number(currentStage.number) + 1)[0];
    if (!nextStage) {
        nextStage = await Stage.create({
            name: `Stage ${stages.length}`,
            projectId: projectID,
            number: stages.length
        })
    }
    release();
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
const createChildren = async (item: IApiPaper, into: string, column1: string, column2: string, firstId: number, nextStage: Stage, project: Project) => {
    let child = await savePaper(item, nextStage, Number(project.mergeThreshold))
    await saveChildren(into, column1, column2, firstId, Number(child.id))
    await PaperScopeForStage.create({ stageId: Number(nextStage.id), paperId: Number(child.id) })
    return child;
}


/**
 * Saves a paper to the database.
 * If the paper is already in the database, it will be tried to be merged.
 * Adds leftover values to the paperCache, to be choosen by the user.
 * @param apiPaper 
 * @returns 
 */
const savePaper = async (apiPaper: IApiPaper, stage: Stage, overallWeight: number): Promise<Paper> => {

    let doi = getDOI(apiPaper)

    if (doi[0]) {
        let dbPaper = await getPaperByDoi(doi[0].toLowerCase())
        if (dbPaper) {
            await assignOnlyIfUnassignedPaper(dbPaper, apiPaper)
            return dbPaper.update()
        }

    }

    let papers = await getAllPapersFromStage(Number(stage.id))
    let comparison = {} as IComparisonWeight
    Object.assign(comparison, comparisonWeight)
    comparison.overallWeight = overallWeight
    for (let paperStuff of papers) {
        let dbPaper = paperStuff.paper
        let equal = await isEqualPaper(await convertDBPaperToIApiPaper(dbPaper), apiPaper, comparison)
        if (equal) {
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
        let userID = await getUserID(await getPayloadFromJWTHeader(ctx))
        let paperInfo = await getAllPapersFromStage(stageID);
        let papers = paperInfo.map(async item => { return await item.paper })
        let message: PapersMessage = { papers: await convertPapersToPaperMessage(await Promise.all(papers), stageID, userID) }

        ctx.response.body = JSON.stringify(message)
    }
}

export const getPapersOfProjectStageFast = async (ctx: Context, projectID: number, stageID: number) => {
    try {
        let validate = await validateUserEntry(ctx, [projectID, stageID], UserStatus.needsMemberOfProject, projectID, { needed: false, params: [] })
        if (validate) {
            let answer = (await getProjectStageStuff(stageID)).rows
            let userID = await getUserID(await getPayloadFromJWTHeader(ctx))
            ctx.response.status = 200;
            let message: PapersMessage = { papers: await convertRowsToPaperMessage(answer, userID) }


            ctx.response.body = JSON.stringify(message)
        }
    } catch (e) {
        console.log(e)
        console.log("number " + stageID)
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
            let bodyJson = await jsonBodyToObject(ctx);
            if (!bodyJson) {
                return
            }

            if (bodyJson.finalDecision) {
                let paperScope = await PaperScopeForStage.where("id", ppID).get()
                if (Array.isArray(paperScope)) {
                    paperScope[0].finalDecision = bodyJson.finalDecision
                    await paperScope[0].update()
                }

            }
            let paper = await PaperScopeForStage.where("id", ppID).paper();
            if (paper) {

                await paperUpdate(ctx, paper, bodyJson)
            }
        } catch (e) {
            makeErrorMessage(ctx, 404, "paper not found")
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

/**
 * Makes a csv file with all the cites and refs of a paper
 * @param ctx 
 * @param projectID 
 * @param stageID 
 * @param ppID 
 */
export const makeRefCiteCsv = async (ctx: Context, projectID: number, stageID: number, ppID: number) => {
    let validate = await validateUserEntry(ctx, [projectID, stageID, ppID], UserStatus.needsMemberOfProject, projectID, { needed: false, params: [] })
    if (validate) {
        try {
            let references = getRefs(ctx, projectID, stageID, ppID)
            let citations = getCites(ctx, projectID, stageID, ppID)

            let finRef = await references;
            let finCite = await citations;
            if (finRef && finCite) {
                let finishedRefs = (finRef).sort(sortPapersByName)
                let finishedCites = (finCite).sort(sortPapersByName)
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
/**
 * Makes the replication package of a project.
 * It contains a zip file with all Papers in the project in one csv and also seperated by stage in different csv.
 * @param ctx 
 * @param projectID 
 */
export const makeReplicationPackage = async (ctx: Context, projectID: number) => {
    let validate = await validateUserEntry(ctx, [projectID], UserStatus.needsMemberOfProject, projectID, { needed: false, params: [] })
    if (validate) {
        let allPapers = await getAllPapersFromProject(projectID)
        let papers = allPapers.map(item => {
            return {
                papers: item.papers.map(paper => { return paper.paper })
                , stage: item.stage
            }
        })
        let project = await Project.find(projectID);
        let filepaths: Promise<string>[] = []
        filepaths.push(makeAllPapersCsv(project, papers))
        papers.forEach(async item => {
            filepaths.push(makeStageCsv(project, item.papers, item.stage));
        })

        let allFilepaths = await Promise.all(filepaths)

        compress(allFilepaths, `${String(project.name)}_replicationPackage.zip`)
        ctx.response.status = 200;
        ctx.response.type = "application/zip";
        ctx.response.body = await Deno.readFile(`${String(project.name)}_replicationPackage.zip`)
        ctx.response.headers.set('Content-disposition', `attachment; filename=${String(project.name)}_replicationPackage.zip`);
    }
}
/**
 * Gets all papers of a project in a single csv file
 * @param ctx 
 * @param projectID 
 */
export const getAllPapersCsv = async (ctx: Context, projectID: number) => {
    let validate = await validateUserEntry(ctx, [projectID], UserStatus.needsMemberOfProject, projectID, { needed: false, params: [] })
    if (validate) {
        let allPapers = await getAllPapersFromProject(projectID)
        let papers = allPapers.map(item => {
            return {
                papers: item.papers.map(paper => { return paper.paper })
                , stage: item.stage
            }
        })
        let project = await Project.find(projectID);

        let filePath = await makeAllPapersCsv(project, papers)
        const text = await Deno.readTextFile(filePath);
        ctx.response.status = 200;
        ctx.response.type = "text/csv";
        ctx.response.body = text;
        ctx.response.headers.set('Content-disposition', `attachment; filename=${filePath}`);
    }
}

const makeAllPapersCsv = async (project: Project, allPapers: { papers: Paper[], stage: Stage }[]) => {
    let rows = [["authors", "title", "year", "publisher", "link", "doi"]]
    for (let i = 1; i < (Number(project.countDecisiveReviewers)); i++) {
        rows[0].push(`SuggestedInclusion${i}`)
    }
    rows[0].push("derivedDecision")
    rows[0].push("FinalDecision")
    for (let papers of allPapers) {
        rows = await papersToRow(papers.papers, rows, true, project, Number(papers.stage.id))
    }
    let filePath = `./${String(project.name)}_allPapers.csv`
    const f = await Deno.open(filePath, { write: true, create: true, truncate: true });

    await writeCSV(f, rows);
    f.close();
    return filePath
}

export const getStageCsv = async (ctx: Context, projectID: number, stageID: number) => {
    let validate = await validateUserEntry(ctx, [projectID, stageID], UserStatus.needsMemberOfProject, projectID, { needed: false, params: [] })
    if (validate) {
        let papers = await getAllPapersFromStage(stageID)

        let project = await Project.find(projectID);
        let stage = await Stage.find(stageID);
        let filePath = makeStageCsv(project, papers.map(paper => { return paper.paper }), stage)
        const text = await Deno.readTextFile(`./${String(project.name)}_Stage${Number(stage.number)}.csv`);
        ctx.response.status = 200;
        ctx.response.type = "text/csv";
        ctx.response.body = text;
        ctx.response.headers.set('Content-disposition', `attachment; filename=${String(project.name)}_Stage${Number(stage.number)}.csv`);
    }
}

const makeStageCsv = async (project: Project, papers: Paper[], stage: Stage) => {
    let rows = [["authors", "title", "year", "publisher", "link", "doi"]]
    for (let i = 1; i < (Number(project.countDecisiveReviewers)); i++) {
        rows[0].push(`SuggestedInclusion${i}`)
    }
    rows[0].push("derivedDecision")
    rows[0].push("FinalDecision")
    rows = await papersToRow(papers, rows, true, project, Number(stage.id))
    let filePath = `./${String(project.name)}_Stage${Number(stage.number)}.csv`
    const f = await Deno.open(filePath, { write: true, create: true, truncate: true });

    await writeCSV(f, rows);
    f.close();
    return filePath
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
        if (getReviews && project && stageID) {
            let ppID = await getProjectPaperScope(stageID, Number(item.id))
            if (ppID) {
                let reviews = await getAllReviewsFromProjectPaper(Number(ppID.id));
                if (Array.isArray(reviews)) {
                    if (reviews.length >= Number(project.countDecisiveReviewers)) {
                        reviews = reviews.slice(0, Number(project.countDecisiveReviewers) - 1)
                    }
                    for (let review of reviews) {
                        row.push(review.overallEvaluation ? String(review.overallEvaluation) : "")
                    }

                    for (let i = 0; i < (Number(project.countDecisiveReviewers) - reviews.length - 1); i++) {
                        row.push("")
                    }
                    let tresholds = String(project.combinationOfReviewers).split(",")
                    row.push(String(getFinalDecisionOfPaper(reviews, project, Number(tresholds[0]), Number(tresholds[1]))))
                    row.push(ppID.finalDecision ? String(ppID.finalDecision) : "")
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
            let message: PapersMessage = { papers: await convertPapersToPaperMessage(citations) }
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
        let project = await Project.find(projectID)
        if (paper && project) {
            let papers: Paper[] = await getRefOrCiteList(ctx, "citedBy", "papercitingid", "papercitedid", Number(paper.id))
            if (papers.length > 0) {
                let nextStage = await findNextStage(await Stage.find(stageID), projectID)
                for (let i = 0; i < papers.length; i++) {
                    if (!await checkPaperInProjectStage(await papers[i], Number(nextStage.id))) {
                        delete papers[i]
                    }
                }
            }
            return papers.filter(item => item)

        } else {
            makeErrorMessage(ctx, 404, "paper or project does not exist")
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
            if (papers.length > 0) {
                let nextStage = await findNextStage(await Stage.find(stageID), projectID)
                for (let i = 0; i < papers.length; i++) {
                    if (!await checkPaperInProjectStage(await papers[i], Number(nextStage.id))) {
                        delete papers[i]
                    }
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
        const payloadJson = await getPayloadFromJWTHeader(ctx);
        let userID = await getUserID(payloadJson)
        if (userID && await checkUserReviewOfProjectPaper(ppID, userID)) {
            makeErrorMessage(ctx, 409, "paper is already in review by you")
            return;
        }
        const params = await jsonBodyToObject(ctx)
        let project = await Project.find(projectID)
        console.log(project)
        let tresholds = String(project.combinationOfReviewers).split(",")
        let pp = await PaperScopeForStage.find(ppID);
        let reviews = await getAllReviewsFromProjectPaper(ppID);


        if (pp.finalDecision || reviews.length >= Number(project.countDecisiveReviewers)) {
            makeErrorMessage(ctx, 409, "already enough reviews for this paper")
            return;
        }
        if (userID) {
            try {
                let review = await Review.create({
                    paperId: ppID,
                    userId: userID,
                    stageId: stageID
                })

                if (params.finished === true && (
                    params.overallEvaluation !== "YES" ||
                    params.overallEvaluation !== "MAYBE" ||
                    params.overallEvaluation !== "NO"
                )) {
                    makeErrorMessage(ctx, 409, "When finishing a review, you have to set the overallEvaluation to 'YES', 'NO' or 'MAYBE'")
                }
                if (params.finished != undefined) { review.finished = params.finished }
                if (params.overallEvaluation) { review.overallEvaluation = params.overallEvaluation }
                if (params.finishDate) { review.finishDate = new Date(params.finishDate) }
                await review.update();
                let finished = await calculateFinalDecisionOfPaper(ctx, params.overallEvaluation, review, pp, project, Number(tresholds[0]), Number(tresholds[1]), stageID)
                if (finished) {
                    ctx.response.status = 201;
                    ctx.response.body = JSON.stringify(review)
                }
            } catch (err) {
                makeErrorMessage(ctx, 404, "stage or paper id not found")
            }
        }
    }
}

const calculateFinalDecisionOfPaper = async (ctx: Context, overallEvaluation: string, review: Review, pp: PaperScopeForStage, project: Project, lowerTreshold: number, upperTreshold: number, stageID: number) => {
    let reviews = await getAllReviewsFromProjectPaper(Number(pp.id));
    let finalDecision = getFinalDecisionOfPaper(reviews, project, lowerTreshold, upperTreshold)
    if (finalDecision) {
        console.log("finalDecision: " + finalDecision)
        if (finalDecision === "MAYBE") {
            if (project.countDecisiveReviewers == reviews.length) {
                if (overallEvaluation == "MAYBE") {
                    makeErrorMessage(ctx, 409, "you made the last review of the paper. this one has to be YES or NO")
                    review.finished = false;
                    await review.update()
                    return false;
                } else {
                    pp.finalDecision = overallEvaluation
                }

            }

        } else {
            pp.finalDecision = finalDecision;
        }

        await pp.update()
        if (finalDecision == "YES") {
            startFetchFromProjectPaper(Number(pp.id), stageID, Number(project.id))
        }

    }
    return true;
}

const startFetchFromProjectPaper = async (ppID: number, stageID: number, projectID: number) => {
    let paper = await PaperScopeForStage.where("id", ppID).paper();
    let authors = await getAllAuthorsFromPaper(Number(paper.id))
    let authorName: string | undefined = undefined;
    if (Array.isArray(authors) && authors[0]) {
        authorName = String(authors[0].rawString)
    }
    await fetchToDB(stageID, projectID,
        paper.doi ? String(paper.doi) : undefined,
        paper.title ? String(paper.title) : undefined,
        authorName,
        paper)


}

const getFinalDecisionOfPaper = (reviews: ReviewMessage[], project: Project, lowerTreshold: number, upperTreshold: number) => {
    let allReviewsFinished = !reviews.some(review => !review.finished)
    console.log("finished reviews: " + allReviewsFinished)
    console.log("project: " + JSON.stringify(project))
    console.log("minCount" + project.minCountReviewers)
    console.log(`${reviews.length >= Number(project.minCountReviewers)} ${reviews.length} ${Number(project.minCountReviewers)}`)
    if (allReviewsFinished && reviews.length >= Number(project.minCountReviewers)) {
        let decisionNumber = 0;
        reviews.forEach(review => {
            if (review.overallEvaluation == "YES") {
                decisionNumber += 10;
            } else if (review.overallEvaluation == "MAYBE") {
                decisionNumber += 5;
            }
        })
        console.log(`devision: ${decisionNumber} / ${reviews.length}`)
        decisionNumber = decisionNumber / reviews.length;
        console.log(`threshold: ${upperTreshold} ${lowerTreshold}`)
        if (decisionNumber >= upperTreshold) {
            return "YES"
        } else if (decisionNumber <= lowerTreshold) {
            return "NO"
        } else {
            return "MAYBE"
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
        let pp = await PaperScopeForStage.find(ppID);
        if (pp.finalDecision) {
            makeErrorMessage(ctx, 409, "the paper has already been finally decided. review cannot be changed")
            return;
        }
        if (validate.finished === true && (
            validate.overallEvaluation !== "YES" ||
            validate.overallEvaluation !== "MAYBE" ||
            validate.overallEvaluation !== "NO"
        )) {
            makeErrorMessage(ctx, 409, "When finishing a review, you have to set the overallEvaluation to 'YES', 'NO' or 'MAYBE'")
        }
        let review = await Review.find(reviewID)
        if (review) {
            Object.assign(review, validate)
            let project = await Project.find(projectID)
            let tresholds = String(project.combinationOfReviewers).split(",")
            await review.update()
            ctx.response.status = 200;
            ctx.response.body = JSON.stringify(review)
            await calculateFinalDecisionOfPaper(ctx, validate.overallEvaluation, review, project, pp, Number(tresholds[0]), Number(tresholds[1]), stageID)
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
        let pp = await PaperScopeForStage.find(ppID);
        if (pp.finalDecision) {
            makeErrorMessage(ctx, 409, "paper is already done with evaluation. delete of review not possible")
            return;
        }
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
        await CriteriaEvaluation.deleteById(criteriaEvalID)
        ctx.response.status = 200;
    }
}