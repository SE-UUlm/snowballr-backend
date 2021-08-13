import { Context } from 'https://deno.land/x/oak/mod.ts';
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
import { getAllPapersFromStage, getPaperByDoi } from "./databaseFetcher/paper.ts";
import { PapersMessage } from "../model/messages/papersMessage.ts";
import { PaperScopeForStage } from "../model/db/paperScopeForStage.ts";
import { assignOnlyIfUnassignedPaper, checkIApiPaper, convertIApiPaperToDBPaper, convertPapersToPaperMessage, convertPaperToPaperMessage } from "../helper/converter/paperConverter.ts";
import { assign } from "../helper/assign.ts"
import { IApiPaper } from "../api/iApiPaper.ts";
import { getDOI } from "../api/apiMerger.ts";
import { Cache, CacheType } from "../api/cache.ts";
import { logger } from "../api/logger.ts";
import { IApiAuthor } from "../api/iApiAuthor.ts";
import { checkAdmin, checkMemberOfProject, checkPO, checkPOofProject, getPayloadFromJWT, UserStatus, validateUserEntry } from "./validation.controller.ts";
import { makeFetching } from "./fetch.controller.ts";
import { saveChildren } from "./database.controller.ts";
import { getPaperCitations, getPaperReferences, paperUpdate, postPaperCitation, postPaperReference } from "./paper.controller.ts";
import { Criteria } from "../model/db/criteria.ts";

export const paperCache = new Cache<IApiPaper>(CacheType.F, 0, "paperCache")
export const authorCache = new Cache<IApiAuthor>(CacheType.F, 0, "authorCache")
/**
 * Creates a project
 *
 * @param ctx
 */
export const createProject = async (ctx: Context) => {
    let validate = await validateUserEntry(ctx, [], UserStatus.needsPO, -1, { needed: true, params: ["name", "minCountReviewers", "countDecisiveReviewers", "type", "combinationOfReviewers"] })
    if (!validate) {
        return
    }
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



/**
 * Adds a person to a project
 *
 * @param ctx
 * @param id id of project
 */
export const addMemberToProject = async (ctx: Context, id: number) => {
    let validate: any = await validateUserEntry(ctx, [id], UserStatus.needsPOOfProject, id, { needed: true, params: ["id"] })
    if (!validate) {
        return
    }
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

/**
 * Removes a member from the project
 * @param ctx 
 * @param projectID 
 * @returns 
 */
export const removeMemberOfProject = async (ctx: Context, projectID: number, userID: number) => {
    let validate: any = await validateUserEntry(ctx, [projectID, userID], UserStatus.needsPOOfProject, projectID, { needed: false, params: [] })
    if (!validate) {
        return
    }

    await UserIsPartOfProject.where({ projectId: projectID, userId: userID }).delete()
    ctx.response.status = 200;
}

/**
 * Returns all members, that are currently part of the looked up project.
 * @param ctx 
 * @param id 
 * @returns 
 */
export const getMembersOfProject = async (ctx: Context, id: number) => {
    let validate = await validateUserEntry(ctx, [id], UserStatus.needsMemberOfProject, id, { needed: false, params: [] })
    if (!validate) {
        return
    }
    ctx.response.status = 200;
    let message: ProjectMembersMessage = { members: await getAllMembersOfProject(id) }
    ctx.response.body = JSON.stringify(message)

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
    if (!validate) {
        return
    }
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

/**
 * Adds a paper to a project Stage 0 and also starts to fetch the info of the paper and the cites and refs
 *
 * @param ctx
 * @param projectID id of project
 * @param stageID id of nextStage
 */
export const addPaperToProjectStage = async (ctx: Context, projectID: number, stageID: number, awaitFetch?: boolean) => {
    let validate = await validateUserEntry(ctx, [projectID, stageID], UserStatus.needsMemberOfProject, projectID, { needed: false, params: [] })
    if (!validate) {
        return
    }

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
    if (!validate) {
        return
    }

    ctx.response.status = 200;
    let message: PapersMessage = { papers: await convertPapersToPaperMessage(await getAllPapersFromStage(stageID), stageID) }
    ctx.response.body = JSON.stringify(message)

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
    if (!validate) {
        return
    }

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
    if (!validate) {
        return
    }

    try {
        let paper = await PaperScopeForStage.where("id", ppID).paper();
        if (paper) {
            await paperUpdate(ctx, paper)
        }
    } catch (e) {
        makeErrorMessage(ctx, 404, "paper does not exist")
    }


}

export const deletePaperOfProjectStage = async (ctx: Context, projectID: number, stageID: number, ppID: number) => {
    let validate = await validateUserEntry(ctx, [projectID, stageID, ppID], UserStatus.needsMemberOfProject, projectID, { needed: false, params: [] })
    if (!validate) {
        return
    }

    await PaperScopeForStage.deleteById(ppID)
    ctx.response.status = 200;


}

export const getCites = async (ctx: Context, projectID: number, stageID: number, ppID: number) => {
    let validate = await validateUserEntry(ctx, [projectID, stageID, ppID], UserStatus.needsMemberOfProject, projectID, { needed: false, params: [] })
    if (!validate) {
        return
    }
    try {
        let paper = await PaperScopeForStage.where("id", ppID).paper();
        if (paper) {
            await getPaperCitations(ctx, Number(paper.id))
        }
    } catch (e) {
        makeErrorMessage(ctx, 404, "paper does not exist")
    }
}

export const getRefs = async (ctx: Context, projectID: number, stageID: number, ppID: number) => {
    let validate = await validateUserEntry(ctx, [projectID, stageID, ppID], UserStatus.needsMemberOfProject, projectID, { needed: false, params: [] })
    if (!validate) {
        return
    }
    try {
        let paper = await PaperScopeForStage.where("id", ppID).paper();
        if (paper) {
            await getPaperReferences(ctx, Number(paper.id))
        }
    } catch (e) {
        makeErrorMessage(ctx, 404, "paper does not exist")
    }
}

export const postCiteProject = async (ctx: Context, projectID: number, stageID: number, ppID: number) => {
    let validate = await validateUserEntry(ctx, [projectID, stageID, ppID], UserStatus.needsMemberOfProject, projectID, { needed: false, params: [] })
    if (!validate) {
        return
    }
    try {
        let paper = await PaperScopeForStage.where("id", ppID).paper();
        let stage = await Stage.find(stageID)
        if (paper && stage) {
            let paper2 = await postPaperCitation(ctx, Number(paper.id))
            if (paper2) {
                let nextStage = await findNextStage(stage, projectID)
                PaperScopeForStage.create({ stageId: Number(nextStage.id), paperId: Number(paper2.id) })
            }
        }
    } catch (e) {
        makeErrorMessage(ctx, 404, "paper does not exist")
    }
}

export const postRefProject = async (ctx: Context, projectID: number, stageID: number, ppID: number) => {
    let validate = await validateUserEntry(ctx, [projectID, stageID, ppID], UserStatus.needsMemberOfProject, projectID, { needed: false, params: [] })
    if (!validate) {
        return
    }
    try {
        let paper = await PaperScopeForStage.where("id", ppID).paper();
        let stage = await Stage.find(stageID)
        if (paper && stage) {
            let paper2 = await postPaperReference(ctx, Number(paper.id))
            if (paper2) {
                let nextStage = await findNextStage(stage, projectID)
                PaperScopeForStage.create({ stageId: Number(nextStage.id), paperId: Number(paper2.id) })
            }
        }
    } catch (e) {
        makeErrorMessage(ctx, 404, "paper does not exist")
    }
}

export const getCriteriasOfProject = async (ctx: Context, projectID: number) => {
    let validate = await validateUserEntry(ctx, [projectID], UserStatus.needsMemberOfProject, projectID, { needed: false, params: [] })
    if (!validate) {
        return
    }

    let criterias = await Criteria.where({ projectId: projectID }).get()
    if (Array.isArray(criterias)) {
        ctx.response.status = 200;
        ctx.response.body = JSON.stringify({ criterias: criterias })
    }
}

export const addCriteriaToProject = async (ctx: Context, projectID: number) => {
    let validate = await validateUserEntry(ctx, [projectID], UserStatus.needsPOOfProject, projectID, { needed: true, params: ["short", "description", "abbreviation", "inclusionExclusion", "weight"] })
    if (!validate) {
        return
    }
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

export const patchCriteriaOfProject = async (ctx: Context, projectID: number, criteriaID: number) => {
    let validate = await validateUserEntry(ctx, [projectID, criteriaID], UserStatus.needsMemberOfProject, projectID, { needed: true, params: [] })
    if (!validate) {
        return
    }

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

export const deleteCriteriaOfProject = async (ctx: Context, projectID: number, criteriaID: number) => {
    let validate = await validateUserEntry(ctx, [projectID, criteriaID], UserStatus.needsMemberOfProject, projectID, { needed: false, params: [] })
    if (!validate) {
        return
    }
    try {
        await Criteria.deleteById(criteriaID)
        ctx.response.status = 200;
    } catch (err) {
        makeErrorMessage(ctx, 403, "Criteria has already been used to evaluate paper. For safety those have to be removed first")
    }
}
export const getApis = async (ctx: Context, projectID: number) => {

}