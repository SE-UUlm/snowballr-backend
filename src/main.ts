import { Application, RouteParams, Router, RouterContext } from 'https://deno.land/x/oak/mod.ts';
import { validateContentType, validateJWTIfExists } from "./controller/validation.controller.ts";
import { login, refresh } from "./controller/login.controller.ts";
import { setup } from "./helper/setup.ts";
import { createUser, getUser, getUserProjects, getUsers, patchUser, resetPassword } from "./controller/user.controller.ts";
import { logout } from "./controller/logout.controller.ts";
import { SmtpClient } from "https://deno.land/x/smtp/mod.ts";
import {
    addCriteriaToProject,
    addCrtieriaEvalToReview,
    addMemberToProject,
    addPaperToProjectStage,
    addReviewToPaper,
    addStageToProject,
    createProject,
    deleteCriteriaOfProject,
    deleteCritieriaEvalOfReview,
    deletePaperOfProjectStage,
    deleteReviewOfPaper,
    getCitationsOfProjectPaper,
    getCites,
    getCriteriaEvalsOfCriteria,
    getCriteriasOfProject,
    getCritieriaEvalOfReview,
    getCrtieriaEvalsOfReview,
    getMembersOfProject,
    getPaperOfProjectStage,
    getPapersOfProjectStage,
    getProjects,
    getReferencesOfProjectPaper,
    getRefs,
    getReviewOfPaper,
    getReviewsOfPaper,
    makeRefCiteCsv,
    getStageCsv,
    patchCriteriaOfProject,
    patchCritieriaEvalOfReview,
    patchPaperOfProjectStage,
    patchReviewOfPaper,
    postCiteProject,
    postRefProject,
    removeMemberOfProject,
    setApiUse,
    getApis,
    replaceApi,
    makeReplicationPackage,
    getAllPapersCsv,
    refetchPaperOfProject,
    getPapersOfProjectStageFast
} from "./controller/project.controller.ts";
import { addAuthorToPaper, deleteAuthorOfPaper, deleteSourcePaper, getAuthors, getPaper, getPaperCitations, getPaperReferences, getPapers, getSourcePaper, patchPaper, postPaper, postPaperCitation, postPaperReference } from "./controller/paper.controller.ts";
import { deleteSourceAuthor, getAuthor, getSourceAuthor, patchAuthor, postAuthor } from "./controller/author.controller.ts";
import { addToReadingList, getReadingList, removeFromReadingList } from "./controller/readinglist.controller.ts";
import { getActiveBatches } from "./controller/fetch.controller.ts";

await setup(true);
const client = new SmtpClient();

const router = new Router();
router
    .get("/currentBatches", (context) => {
        getActiveBatches(context)
    })
    .get("/users", async (context) => {
        await getUsers(context)
    })
    .post("/users", async (context) => {
        await createUser(context, client)
    })
    .get("/users/:id", async (context) => {
        await getUser(context, Number(context.params.id))
    })
    .patch("/users/:id", async (context) => {
        await patchUser(context, Number(context.params.id))
    })
    .get("/users/:id/readinglist", async (context) => {
        await getReadingList(context, Number(context.params.id))
    })
    .post("/users/:id/readinglist", async (context) => {
        await addToReadingList(context, Number(context.params.id))
    })
    .delete("/users/:id/readinglist/:id2", async (context) => {
        await removeFromReadingList(context, Number(context.params.id), Number(context.params.id2))
    })
    .post("/login", async (context) => {
        await login(context)
    })
    .get("/logout", async (context) => {
        await logout(context)
    })
    .get("/refresh", async (context) => {
        await refresh(context)
    })
    .post("/reset-password", async (context) => {
        await resetPassword(context, client)
    })
    .get("/users/:id/projects", async (context) => {
        await getUserProjects(context, Number(context.params.id))
    })
    .get("/projects", async (context) => {
        await getProjects(context)
    })
    .post("/projects", async (context) => {
        await createProject(context)
    })
    .post("/projects/:id/members", async (context) => {
        await addMemberToProject(context, Number(context.params.id))
    })
    .get("/projects/:id/members", async (context) => {
        await getMembersOfProject(context, Number(context.params.id))
    })
    .post("/projects/:id/refetch", async (context) => {
        await refetchPaperOfProject(context, Number(context.params.id))
    })
    .delete("/projects/:id/members/:id2", async (context) => {
        await removeMemberOfProject(context, Number(context.params.id), Number(context.params.id2))
    })
    .post("/projects/:id/criterias", async (context) => {
        await addCriteriaToProject(context, Number(context.params.id))
    })
    .get("/projects/:id/criterias", async (context) => {
        await getCriteriasOfProject(context, Number(context.params.id))
    })
    .patch("/projects/:id/criterias/:id2", async (context) => {
        await patchCriteriaOfProject(context, Number(context.params.id), Number(context.params.id2))
    })

    .get("/projects/:id/criterias/:id2/criteriaevaluations", async (context) => {
        await getCriteriaEvalsOfCriteria(context, Number(context.params.id), Number(context.params.id2))
    })
    .delete("/projects/:id/members", async (context) => {
        await deleteCriteriaOfProject(context, Number(context.params.id), Number(context.params.id2))
    })
    .post("/projects/:id/stages", async (context) => {
        await addStageToProject(context, Number(context.params.id))
    })
    .get("/projects/:id/replication", async (context) => {
        await makeReplicationPackage(context, Number(context.params.id))
    })
    .get("/projects/:id/csv", async (context) => {
        await getAllPapersCsv(context, Number(context.params.id))
    })
    .get("/projects/:id/apis", async (context) => {
        await getApis(context, Number(context.params.id))
    })
    .post("/projects/:id/apis", async (context) => {
        await setApiUse(context, Number(context.params.id))
    })
    .post("/projects/:id/apis/:id2", async (context) => {
        await replaceApi(context, Number(context.params.id), Number(context.params.id2))
    })
    .post("/projects/:id/stages/:id2/papers", async (context) => {
        await addPaperToProjectStage(context, Number(context.params.id), Number(context.params.id2))
    })
    .get("/projects/:id/stages/:id2/papers", async (context) => {
        await getPapersOfProjectStageFast(context, Number(context.params.id), Number(context.params.id2))
    })
    .get("/projects/:id/stages/:id2/csv", async (context) => {
        await getStageCsv(context, Number(context.params.id), Number(context.params.id2))
    })
    .get("/projects/:id/stages/:id2/papers/:ppid", async (context) => {
        await getPaperOfProjectStage(context, Number(context.params.id), Number(context.params.id2), Number(context.params.ppid))
    })
    .patch("/projects/:id/stages/:id2/papers/:ppid", async (context) => {
        await patchPaperOfProjectStage(context, Number(context.params.id), Number(context.params.id2), Number(context.params.ppid))
    })
    .delete("/projects/:id/stages/:id2/papers/:ppid", async (context) => {
        await deletePaperOfProjectStage(context, Number(context.params.id), Number(context.params.id2), Number(context.params.ppid))
    })
    .get("/projects/:id/stages/:id2/papers/:ppid/references", async (context) => {
        await getReferencesOfProjectPaper(context, Number(context.params.id), Number(context.params.id2), Number(context.params.ppid))
    })
    .get("/projects/:id/stages/:id2/papers/:ppid/csv", async (context) => {
        await makeRefCiteCsv(context, Number(context.params.id), Number(context.params.id2), Number(context.params.ppid))
    })
    .post("/projects/:id/stages/:id2/papers/:ppid/references", async (context) => {
        await postRefProject(context, Number(context.params.id), Number(context.params.id2), Number(context.params.ppid))
    })
    .get("/projects/:id/stages/:id2/papers/:ppid/citations", async (context) => {
        await getCitationsOfProjectPaper(context, Number(context.params.id), Number(context.params.id2), Number(context.params.ppid))
    })
    .post("/projects/:id/stages/:id2/papers/:ppid/citations", async (context) => {
        await postCiteProject(context, Number(context.params.id), Number(context.params.id2), Number(context.params.ppid))
    })
    .get("/projects/:id/stages/:id2/papers/:ppid/reviews", async (context) => {
        await getReviewsOfPaper(context, Number(context.params.id), Number(context.params.id2), Number(context.params.ppid))
    })
    .post("/projects/:id/stages/:id2/papers/:ppid/reviews", async (context) => {
        await addReviewToPaper(context, Number(context.params.id), Number(context.params.id2), Number(context.params.ppid))
    })
    .get("/projects/:id/stages/:id2/papers/:ppid/reviews/:rid", async (context) => {
        await getReviewOfPaper(context, Number(context.params.id), Number(context.params.id2), Number(context.params.ppid), Number(context.params.rid))
    })
    .patch("/projects/:id/stages/:id2/papers/:ppid/reviews/:rid", async (context) => {
        await patchReviewOfPaper(context, Number(context.params.id), Number(context.params.id2), Number(context.params.ppid), Number(context.params.rid))
    })
    .delete("/projects/:id/stages/:id2/papers/:ppid/reviews/:rid", async (context) => {
        await deleteReviewOfPaper(context, Number(context.params.id), Number(context.params.id2), Number(context.params.ppid), Number(context.params.rid))
    })

    .get("/projects/:id/stages/:id2/papers/:ppid/reviews/:rid/criteriaevaluations", async (context) => {
        await getCrtieriaEvalsOfReview(context, Number(context.params.id), Number(context.params.id2), Number(context.params.ppid), Number(context.params.rid))
    })
    .post("/projects/:id/stages/:id2/papers/:ppid/reviews/:rid/criteriaevaluations", async (context) => {
        await addCrtieriaEvalToReview(context, Number(context.params.id), Number(context.params.id2), Number(context.params.ppid), Number(context.params.rid))
    })
    .get("/projects/:id/stages/:id2/papers/:ppid/reviews/:rid/criteriaevaluations/:cid", async (context) => {
        await getCritieriaEvalOfReview(context, Number(context.params.id), Number(context.params.id2), Number(context.params.ppid), Number(context.params.rid), Number(context.params.cid))
    })
    .patch("/projects/:id/stages/:id2/papers/:ppid/reviews/:rid/criteriaevaluations/:cid", async (context) => {
        await patchCritieriaEvalOfReview(context, Number(context.params.id), Number(context.params.id2), Number(context.params.ppid), Number(context.params.rid), Number(context.params.cid))
    })
    .delete("/projects/:id/stages/:id2/papers/:ppid/reviews/:rid/criteriaevaluations/:cid", async (context) => {
        await deleteCritieriaEvalOfReview(context, Number(context.params.id), Number(context.params.id2), Number(context.params.ppid), Number(context.params.rid), Number(context.params.cid))
    })
    .get("/papers/", async (context) => {
        await getPapers(context)
    })
    .post("/papers/", async (context) => {
        await postPaper(context)
    })
    .get("/papers/:id", async (context) => {
        await getPaper(context, Number(context.params.id))
    })
    .patch("/papers/:id", async (context) => {
        await patchPaper(context, Number(context.params.id))
    })
    .get("/sourcePapers/:id", (context) => {
        getSourcePaper(context, Number(context.params.id))
    })
    .delete("/sourcePapers/:id", (context) => {
        deleteSourcePaper(context, Number(context.params.id))
    })
    .get("/papers/:id/references", async (context) => {
        await getPaperReferences(context, Number(context.params.id))
    })
    .post("/papers/:id/references", async (context) => {
        await postPaperReference(context, Number(context.params.id))
    })
    .get("/papers/:id/citations", async (context) => {
        await getPaperCitations(context, Number(context.params.id))
    })
    .post("/papers/:id/citations", async (context) => {
        await postPaperCitation(context, Number(context.params.id))
    })
    .get("/papers/:id/authors", async (context) => {
        await getAuthors(context, Number(context.params.id))
    })
    .post("/papers/:id/authors", async (context) => {
        await addAuthorToPaper(context, Number(context.params.id))
    })
    .delete("/papers/:id/authors/:id2", async (context) => {
        await deleteAuthorOfPaper(context, Number(context.params.id), Number(context.params.id))
    })
    .post("/authors", async (context) => {
        await postAuthor(context)
    })
    .get("/authors/:id", async (context) => {
        await getAuthor(context, Number(context.params.id))
    })
    .patch("/authors/:id", async (context) => {
        await patchAuthor(context, Number(context.params.id))
    })
    .get("/sourceAuthors/:id", async (context) => {
        await getSourceAuthor(context, Number(context.params.id))
    })
    .delete("/sourceAuthors/:id", async (context) => {
        await deleteSourceAuthor(context, Number(context.params.id))
    })

const app = new Application();
app.use(await validateContentType)
app.use(await validateJWTIfExists)
app.use(router.routes());
app.use(router.allowedMethods());

await app.listen({ port: 80 });
