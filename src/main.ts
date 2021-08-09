import { Application, Router } from 'https://deno.land/x/oak/mod.ts';
import { validateContentType, validateJWTIfExists } from "./controller/validation.controller.ts";
import { login } from "./controller/login.controller.ts";
import { setup } from "./helper/setup.ts";
import { createUser, getUser, getUserProjects, getUsers, patchUser, resetPassword } from "./controller/user.controller.ts";
import { logout } from "./controller/logout.controller.ts";
import { SmtpClient } from "https://deno.land/x/smtp/mod.ts";
import {
    addMemberToProject,
    addPaperToProjectStage,
    addStageToProject,
    createProject,
    deletePaperOfProjectStage,
    getCites,
    getMembersOfProject,
    getPaperOfProjectStage,
    getPapersOfProjectStage,
    getProjects,
    getRefs,
    patchPaperOfProjectStage,
    postCiteProject,
    postRefProject
} from "./controller/project.controller.ts";
import { getPaper, getPaperCitations, getPaperReferences, getPapers, getSourcePaper, patchPaper, postPaperCitation, postPaperReference } from "./controller/paper.controller.ts";
import { getAuthor, getSourceAuthor, patchAuthor } from "./controller/author.controller.ts";

await setup(true);
const client = new SmtpClient();

const router = new Router();
router
    .get("/", (context) => {
        context.response.body = { message: "hello there" }
    })
    .post("/login", async (context) => {
        await login(context)
    })
    .get("/logout", async (context) => {
        await logout(context)
    })
    .post("/reset-password", async (context) => {
        await resetPassword(context, client)
    })
    .post("/users", async (context) => {
        await createUser(context, client)
    })
    .get("/users", async (context) => {
        await getUsers(context)
    })
    .get("/users/:id", async (context) => {
        await getUser(context, Number(context.params.id))
    })
    .patch("/users/:id", async (context, Methods) => {
        await patchUser(context, Number(context.params.id))
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
    .post("/projects/:id/stages", async (context) => {
        await addStageToProject(context, Number(context.params.id))
    })
    .post("/projects/:id/stages/:id2/papers", async (context) => {
        await addPaperToProjectStage(context, Number(context.params.id), Number(context.params.id2))
    })
    .get("/projects/:id/stages/:id2/papers", async (context) => {
        await getPapersOfProjectStage(context, Number(context.params.id), Number(context.params.id2))
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
        await getRefs(context, Number(context.params.id), Number(context.params.id2), Number(context.params.ppid))
    })
    .post("/projects/:id/stages/:id2/papers/:ppid/references", async (context) => {
        await postRefProject(context, Number(context.params.id), Number(context.params.id2), Number(context.params.ppid))
    })
    .get("/projects/:id/stages/:id2/papers/:ppid/citations", async (context) => {
        await getCites(context, Number(context.params.id), Number(context.params.id2), Number(context.params.ppid))
    })
    .post("/projects/:id/stages/:id2/papers/:ppid/citations", async (context) => {
        await postCiteProject(context, Number(context.params.id), Number(context.params.id2), Number(context.params.ppid))
    })
    .get("/papers/", async (context) => {
        await getPapers(context)
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
    .get("/authors/:id", async (context) => {
        await getAuthor(context, Number(context.params.id))
    })
    .patch("/authors/:id", async (context) => {
        await patchAuthor(context, Number(context.params.id))
    })
    .get("/sourceAuthors/:id", async (context) => {
        await getSourceAuthor(context, Number(context.params.id))
    })

const app = new Application();
app.use(await validateContentType)
app.use(await validateJWTIfExists)
app.use(router.routes());
app.use(router.allowedMethods());

await app.listen({ port: 80 });
