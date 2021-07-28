import { Application, Router } from 'https://deno.land/x/oak/mod.ts';
import { validateContentType, validateJWTIfExists } from "./controller/validation.ts";
import { login } from "./controller/login.ts";
import { setup } from "./helper/setup.ts";
import { createUser, getUser, getUserProjects, getUsers, patchUser, resetPassword } from "./controller/user.ts";
import { logout } from "./controller/logout.ts";
import { SmtpClient } from "https://deno.land/x/smtp/mod.ts";
import {
    addMemberToProject,
    addPaperToProjectStage,
    addStageToProject,
    createProject,
    getMembersOfProject,
    getPaperOfProjectStage,
    getPapersOfProjectStage,
    getProjects,
    patchPaperOfProjectStage
} from "./controller/project.ts";
import { getPaper, getPaperCitations, getPaperReferences, getPapers, getSourcePaper, patchPaper } from "./controller/paper.ts";

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
    .get("/papers/:id/citations", async (context) => {
        await getPaperCitations(context, Number(context.params.id))
    })

const app = new Application();
app.use(await validateContentType)
app.use(await validateJWTIfExists)
app.use(router.routes());
app.use(router.allowedMethods());

await app.listen({ port: 80 });
