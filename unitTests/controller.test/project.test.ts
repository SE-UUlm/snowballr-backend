import { setup } from "../../src/helper/setup.ts";
import { insertUser } from "../../src/controller/databaseFetcher/user.ts";
import { createMockApp } from "../mockObjects/oak/mockApp.test.ts";
import { checkMemberOfProject, createJWT } from "../../src/controller/validation.controller.ts";
import { createMockContext } from "../mockObjects/oak/mockContext.test.ts";
import {
    addMemberToProject,
    addPaperToProjectStage,
    addStageToProject,
    authorCache,
    createProject,
    getMembersOfProject,
    getPaperOfProjectStage,
    getPapersOfProjectStage,
    getProjects,
    paperCache,
    patchPaperOfProjectStage
} from "../../src/controller/project.controller.ts";
import { Project } from "../../src/model/db/project.ts";
import { assertEquals, assertNotEquals } from "https://deno.land/std/testing/asserts.ts"
import { UserIsPartOfProject } from "../../src/model/db/userIsPartOfProject.ts";
import { client, db } from "../../src/controller/database.controller.ts";
import { Stage } from "../../src/model/db/stage.ts";
import { Batcher } from "../../src/controller/fetch.controller.ts";
import { getPaper, getPaperCitations, getPaperReferences, getSourcePaper } from "../../src/controller/paper.controller.ts";
import { Paper } from "../../src/model/db/paper.ts";
import { Author } from "../../src/model/db/author.ts";
import { Wrote } from "../../src/model/db/wrote.ts";
import { PaperScopeForStage } from "../../src/model/db/paperScopeForStage.ts";

Deno.test({
    name: "createProjectUnauth",
    async fn(): Promise<void> {
        await setup(true);
        let user = await insertUser("test@test", "ash", false, "Test", "Tester", "active");
        let app = await createMockApp();
        let token = await createJWT(user)
        let ctx = await createMockContext(app, `{"name":"3", "minCountReviewers": 5, "countDecisiveReviewers":8}`, [["Content-Type", "application/json"]], "/", token);
        await createProject(ctx)
        assertEquals(ctx.response.status, 401)
        await db.close();
        await client.end();
    },
    sanitizeResources: false,
    sanitizeOps: false,
})

Deno.test({
    name: "createProject",
    async fn(): Promise<void> {
        await setup(true);
        let user = await insertUser("test@test", "ash", true, "Test", "Tester", "active");
        let app = await createMockApp();
        let token = await createJWT(user)

        let projects = await Project.all()

        let size = projects.length
        let name = "nameee"
        let minCount = 3
        let countDecRev = 5
        let ctx = await createMockContext(app, `{"name":"${name}", "minCountReviewers": ${minCount}, "countDecisiveReviewers":${countDecRev}}`, [["Content-Type", "application/json"]], "/", token);
        await createProject(ctx)
        projects = await Project.all()
        assertEquals(projects.length, size + 1)
        let answer = JSON.parse(ctx.response.body as string)
        assertEquals(answer.name, name)
        assertEquals(answer.minCountReviewers, minCount)
        assertEquals(answer.countDecisiveReviewers, countDecRev)
        await db.close();
        await client.end();
    },
    sanitizeResources: false,
    sanitizeOps: false,
})

Deno.test({
    name: "createProjectNoParams",
    async fn(): Promise<void> {
        await setup(true);
        let user = await insertUser("test123@test", "ash", true, "Test", "Tester", "active");
        let app = await createMockApp();
        let token = await createJWT(user)

        5
        let ctx = await createMockContext(app, `{}`, [["Content-Type", "application/json"]], "/", token);
        await createProject(ctx)
        assertEquals(ctx.response.status, 422)
        await db.close();
        await client.end();
    },
    sanitizeResources: false,
    sanitizeOps: false,
})
Deno.test({
    name: "createProjectEvalFormula",
    async fn(): Promise<void> {
        await setup(true);
        let user = await insertUser("test@test", "ash", true, "Test", "Tester", "active");
        let app = await createMockApp();
        let token = await createJWT(user)

        let evalFormula = "NoExampleKnownYet"
        let ctx = await createMockContext(app, `{"name":"name", "minCountReviewers": 3, "countDecisiveReviewers":5, "evaluationFormula": "${evalFormula}"}`, [["Content-Type", "application/json"]], "/", token);
        await createProject(ctx)
        let answer = JSON.parse(ctx.response.body as string)
        assertEquals(answer.evaluationFormula, evalFormula)

        await db.close();
        await client.end();
    },
    sanitizeResources: false,
    sanitizeOps: false,
})

Deno.test({
    name: "createProjectNoName",
    async fn(): Promise<void> {
        await setup(true);
        let user = await insertUser("test@test", "ash", true, "Test", "Tester", "active");
        let app = await createMockApp();
        let token = await createJWT(user)

        let projects = await Project.all()

        let size = projects.length

        let ctx = await createMockContext(app, `{"minCountReviewers": 3, "countDecisiveReviewers":5}`, [["Content-Type", "application/json"]], "/", token);
        await createProject(ctx)
        projects = await Project.all()
        assertEquals(projects.length, size)
        assertEquals(ctx.response.status, 422)
        await db.close();
        await client.end();
    },
    sanitizeResources: false,
    sanitizeOps: false,
})

Deno.test({
    name: "getProject",
    async fn(): Promise<void> {
        await setup(true);
        let user = await insertUser("test@test", "ash", true, "Test", "Tester", "active");
        await Project.create({
            name: "test",
            minCountReviewers: 1,
            countDecisiveReviewers: 1
        })
        let app = await createMockApp();
        let token = await createJWT(user)
        let ctx = await createMockContext(app, `{}`, [["Content-Type", "application/json"]], "/", token);
        await getProjects(ctx)

        let answer = JSON.parse(ctx.response.body as string)
        assertNotEquals(answer.projects.length, 0)

        await db.close();
        await client.end();
    },
    sanitizeResources: false,
    sanitizeOps: false,
})

Deno.test({
    name: "getProjectUnauthorized",
    async fn(): Promise<void> {
        await setup(true);
        let user = await insertUser("test@test", "ash", true, "Test", "Tester", "active");
        await Project.create({
            name: "test",
            minCountReviewers: 1,
            countDecisiveReviewers: 1
        })
        let app = await createMockApp();
        let token = await createJWT(user)
        let ctx = await createMockContext(app, `{}`, [["Content-Type", "application/json"]], "/", token);
        await getProjects(ctx)

        assertNotEquals(ctx.response.status, 422)

        await db.close();
        await client.end();
    },

    sanitizeResources: false,
    sanitizeOps: false,
})

Deno.test({
    name: "getMembersAsMember",
    async fn(): Promise<void> {
        await setup(true);
        let user = await insertUser("test@test", "ash", false, "Test", "Tester", "active");
        let project = await Project.create({
            name: "test",
            minCountReviewers: 1,
            countDecisiveReviewers: 1
        })
        await UserIsPartOfProject.create({
            projectId: Number(project.id),
            userId: Number(user.id),
            isOwner: false
        })
        let app = await createMockApp();
        let token = await createJWT(user)
        let ctx = await createMockContext(app, `{}`, [["Content-Type", "application/json"]], "/", token);
        await getMembersOfProject(ctx, Number(project.id))

        let answer = JSON.parse(ctx.response.body as string)
        assertNotEquals(answer.members.length, 0)

        await db.close();
        await client.end();
    },
    sanitizeResources: false,
    sanitizeOps: false,
})

Deno.test({
    name: "getMembersNoId",
    async fn(): Promise<void> {
        await setup(true);
        let user = await insertUser("test@test", "ash", false, "Test", "Tester", "active");
        let project = await Project.create({
            name: "test",
            minCountReviewers: 1,
            countDecisiveReviewers: 1
        })
        await UserIsPartOfProject.create({
            projectId: Number(project.id),
            userId: Number(user.id),
            isOwner: false
        })
        let app = await createMockApp();
        let token = await createJWT(user)
        let ctx = await createMockContext(app, `{}`, [["Content-Type", "application/json"]], "/", token);
        await getMembersOfProject(ctx, undefined)

        assertEquals(ctx.response.status, 422)

        await db.close();
        await client.end();
    },
    sanitizeResources: false,
    sanitizeOps: false,
})

Deno.test({
    name: "getMembersUnauthorized",
    async fn(): Promise<void> {
        await setup(true);
        let user = await insertUser("test523423424@test", "ash", false, "Test", "Tester", "active");
        let project = await Project.create({
            name: "test",
            minCountReviewers: 1,
            countDecisiveReviewers: 1
        })
        let app = await createMockApp();
        let token = await createJWT(user)
        let ctx = await createMockContext(app, `{}`, [["Content-Type", "application/json"]], "/", token);
        await getMembersOfProject(ctx, Number(project.id))

        assertEquals(ctx.response.status, 401)

        await db.close();
        await client.end();
    },
    sanitizeResources: false,
    sanitizeOps: false,
})

Deno.test({
    name: "addMemberAsPO",
    async fn(): Promise<void> {
        await setup(true);
        let user = await insertUser("test@test", "ash", false, "Test", "Tester", "active");
        let user2 = await insertUser("test2@test", "ash2", false, "Test2", "Tester2", "active");
        let project = await Project.create({
            name: "test",
            minCountReviewers: 1,
            countDecisiveReviewers: 1
        })
        let bla = await UserIsPartOfProject.create({
            projectId: Number(project.id),
            userId: Number(user.id),
            isOwner: true
        })
        let app = await createMockApp();
        let token = await createJWT(user)
        let ctx = await createMockContext(app, `{ "id": "${Number(user2.id)}", "isOwner": true}`, [["Content-Type", "application/json"]], "/", token);
        await addMemberToProject(ctx, Number(project.id))
        let worked = await checkMemberOfProject(Number(project.id), {
            id: Number(user2.id),
            isAdmin: false,
            email: "fesffer",
            firstName: "",
            lastName: "",
            status: "active"
        })
        assertEquals(worked, true)

        await db.close();
        await client.end();
    },
    sanitizeResources: false,
    sanitizeOps: false,
})

Deno.test({
    name: "addMemberNoProjectId",
    async fn(): Promise<void> {
        await setup(true);
        let user = await insertUser("test@test", "ash", false, "Test", "Tester", "active");
        let user2 = await insertUser("test2@test", "ash2", false, "Test2", "Tester2", "active");
        let project = await Project.create({
            name: "test",
            minCountReviewers: 1,
            countDecisiveReviewers: 1
        })
        let bla = await UserIsPartOfProject.create({
            projectId: Number(project.id),
            userId: Number(user.id),
            isOwner: true
        })
        let app = await createMockApp();
        let token = await createJWT(user)
        let ctx = await createMockContext(app, `{ "id": "${Number(user2.id)}"}`, [["Content-Type", "application/json"]], "/", token);
        await addMemberToProject(ctx, undefined)
        assertEquals(ctx.response.status, 422)

        await db.close();
        await client.end();
    },
    sanitizeResources: false,
    sanitizeOps: false,
})


Deno.test({
    name: "addMemberNoBody",
    async fn(): Promise<void> {
        await setup(true);
        let user = await insertUser("test@test", "ash", false, "Test", "Tester", "active");
        let user2 = await insertUser("test2@test", "ash2", false, "Test2", "Tester2", "active");
        let project = await Project.create({
            name: "test",
            minCountReviewers: 1,
            countDecisiveReviewers: 1
        })
        let bla = await UserIsPartOfProject.create({
            projectId: Number(project.id),
            userId: Number(user.id),
            isOwner: true
        })
        let app = await createMockApp();
        let token = await createJWT(user)
        let ctx = await createMockContext(app, ``, [["Content-Type", "application/json"]], "/", token);
        await addMemberToProject(ctx, undefined)
        assertEquals(ctx.response.status, 422)

        await db.close();
        await client.end();
    },
    sanitizeResources: false,
    sanitizeOps: false,
})

Deno.test({
    name: "addMemberNoUserId",
    async fn(): Promise<void> {
        await setup(true);
        let user = await insertUser("test@test", "ash", false, "Test", "Tester", "active");
        let user2 = await insertUser("test2@test", "ash2", false, "Test2", "Tester2", "active");
        let project = await Project.create({
            name: "test",
            minCountReviewers: 1,
            countDecisiveReviewers: 1
        })
        let bla = await UserIsPartOfProject.create({
            projectId: Number(project.id),
            userId: Number(user.id),
            isOwner: true
        })
        let app = await createMockApp();
        let token = await createJWT(user)
        let ctx = await createMockContext(app, `{"something wrong": 3}`, [["Content-Type", "application/json"]], "/", token);
        await addMemberToProject(ctx, undefined)
        assertEquals(ctx.response.status, 422)

        await db.close();
        await client.end();
    },
    sanitizeResources: false,
    sanitizeOps: false,
})


Deno.test({
    name: "addMemberUnauthorized",
    async fn(): Promise<void> {
        await setup(true);
        let user = await insertUser("test@test", "ash", false, "Test", "Tester", "active");
        let user2 = await insertUser("test2@test", "ash2", false, "Test2", "Tester2", "active");
        let project = await Project.create({
            name: "test",
            minCountReviewers: 1,
            countDecisiveReviewers: 1
        })
        await UserIsPartOfProject.create({
            projectId: Number(project.id),
            userId: Number(user.id),
            isOwner: false
        })
        let app = await createMockApp();
        let token = await createJWT(user)
        let ctx = await createMockContext(app, `{ "id": {Number(user2.id)}`, [["Content-Type", "application/json"]], "/", token);
        await addMemberToProject(ctx, Number(project.id))

        assertEquals(ctx.response.status, 401)

        await db.close();
        await client.end();
    },
    sanitizeResources: false,
    sanitizeOps: false,
})
Deno.test({
    name: "addStage",
    async fn(): Promise<void> {
        await setup(true);
        let user = await insertUser("test@test", "ash", true, "Test", "Tester", "active");
        let project = await Project.create({
            name: "test",
            minCountReviewers: 1,
            countDecisiveReviewers: 1
        })
        await UserIsPartOfProject.create({
            projectId: Number(project.id),
            userId: Number(user.id),
            isOwner: false
        })
        let app = await createMockApp();
        let token = await createJWT(user)
        let ctx = await createMockContext(app, `{ "name": "namee"}`, [["Content-Type", "application/json"]], "/", token);
        await addStageToProject(ctx, Number(project.id))

        assertEquals(ctx.response.status, 201)

        await db.close();
        await client.end();
    },
    sanitizeResources: false,
    sanitizeOps: false,
})

Deno.test({
    name: "addStageNoId",
    async fn(): Promise<void> {
        await setup(true);
        let user = await insertUser("test@test", "ash", false, "Test", "Tester", "active");
        let project = await Project.create({
            name: "test",
            minCountReviewers: 1,
            countDecisiveReviewers: 1
        });

        let app = createMockApp();
        let token = await createJWT(user)
        let ctx = await createMockContext(app, `{ "name": "namee"}`, [["Content-Type", "application/json"]], "/", token);
        await addStageToProject(ctx, undefined)

        assertEquals(ctx.response.status, 422)

        await db.close();
        await client.end();
    },
    sanitizeResources: false,
    sanitizeOps: false,
})

Deno.test({
    name: "addStageNoParams",
    async fn(): Promise<void> {
        await setup(true);
        let user = await insertUser("test@test", "ash", true, "Test", "Tester", "active");
        let project = await Project.create({
            name: "test",
            minCountReviewers: 1,
            countDecisiveReviewers: 1
        });

        let app = createMockApp();
        let token = await createJWT(user)
        let ctx = await createMockContext(app, ``, [["Content-Type", "application/json"]], "/", token);
        await addStageToProject(ctx, undefined)

        assertEquals(ctx.response.status, 422)

        await db.close();
        await client.end();
    },
    sanitizeResources: false,
    sanitizeOps: false,
})

Deno.test({
    name: "addTwoStages",
    async fn(): Promise<void> {
        await setup(true);
        let user = await insertUser("test@test", "ash", true, "Test", "Tester", "active");
        let project = await Project.create({
            name: "test",
            minCountReviewers: 1,
            countDecisiveReviewers: 1
        })
        await UserIsPartOfProject.create({
            projectId: Number(project.id),
            userId: Number(user.id),
            isOwner: false
        })
        let app = await createMockApp();
        let token = await createJWT(user)
        let ctx = await createMockContext(app, `{ "name": "namee"}`, [["Content-Type", "application/json"]], "/", token);
        await addStageToProject(ctx, Number(project.id))
        let answer = JSON.parse(ctx.response.body as string)
        assertEquals(answer.number, 0)
        ctx = await createMockContext(app, `{ "name": "namee2"}`, [["Content-Type", "application/json"]], "/", token);
        await addStageToProject(ctx, Number(project.id))
        answer = JSON.parse(ctx.response.body as string)
        assertEquals(answer.number, 1)
        assertEquals(ctx.response.status, 201)

        await db.close();
        await client.end();
    },
    sanitizeResources: false,
    sanitizeOps: false,
})

Deno.test({
    name: "addStageUnauthorized",
    async fn(): Promise<void> {
        await setup(true);
        let user = await insertUser("test@test", "ash", false, "Test", "Tester", "active");
        let project = await Project.create({
            name: "test",
            minCountReviewers: 1,
            countDecisiveReviewers: 1
        })
        await UserIsPartOfProject.create({
            projectId: Number(project.id),
            userId: Number(user.id),
            isOwner: false
        })
        let app = await createMockApp();
        let token = await createJWT(user)
        let ctx = await createMockContext(app, `{ "name": "namee"}`, [["Content-Type", "application/json"]], "/", token);
        await addStageToProject(ctx, Number(project.id))

        assertEquals(ctx.response.status, 401)

        await db.close();
        await client.end();
    },
    sanitizeResources: false,
    sanitizeOps: false,
})

Deno.test({
    name: "AddPaperToProject",
    async fn(): Promise<void> {
        await setup(true);
        let user = await insertUser("test@test", "ash", true, "Test", "Tester", "active");
        let project = await Project.create({
            name: "test",
            minCountReviewers: 1,
            countDecisiveReviewers: 1
        })
        let stage = await Stage.create({
            name: "TestStage",
            projectId: Number(project.id),
            number: 0
        })
        let app = await createMockApp();
        let token = await createJWT(user)
        let ctx = await createMockContext(app, `{"doi":"10.1109/SEAA.2009.60" }`, [["Content-Type", "application/json"]], "/", token);
        let bla = await addPaperToProjectStage(ctx, Number(project.id), Number(stage.id), true)

        assertEquals(ctx.response.status, 201)

        for (let i = 1; i < 30; i++) {
            await getPaperCitations(ctx, i)
        }
        paperCache.clear()
        authorCache.clear()
        Batcher.kill()
        await db.close();
        await client.end();

    },
    sanitizeResources: false,
    sanitizeOps: false,
})

Deno.test({
    name: "GetPapersOfProjectStage",
    async fn(): Promise<void> {
        await setup(true);
        let user = await insertUser("test@test", "ash", true, "Test", "Tester", "active");
        let project = await Project.create({
            name: "test",
            minCountReviewers: 1,
            countDecisiveReviewers: 1
        })
        let stage = await Stage.create({
            name: "TestStage",
            projectId: Number(project.id),
            number: 0
        })
        let paper = await Paper.create({
            doi: "fasfafraf",
            title: "nice title",
            abstract: "hello there"

        })
        let paper2 = await Paper.create({
            doi: "fasfd",
            title: "nicer title",
            abstract: "General Kenobi"

        })
        let author = await Author.create({
            rawString: "Awsome Name"
        })
        await PaperScopeForStage.create({
            paperId: Number(paper.id),
            stageId: Number(stage.id)
        })
        await PaperScopeForStage.create({
            paperId: Number(paper2.id),
            stageId: Number(stage.id)
        })
        let wrote = await Wrote.create({ authorId: Number(author.id), paperId: Number(paper.id) })
        let app = await createMockApp();
        let token = await createJWT(user)
        let ctx = await createMockContext(app, `{"doi":"10.1109/SEAA.2009.60" }`, [["Content-Type", "application/json"]], "/", token);
        let bla = await getPapersOfProjectStage(ctx, Number(project.id), Number(stage.id))

        let answer = JSON.parse(ctx.response.body as string)
        assertEquals(Array.isArray(answer.papers), true)
        assertEquals(answer.papers[0].title, "nice title")
        assertEquals(answer.papers[1].title, "nicer title")
        paperCache.clear()
        authorCache.clear()
        Batcher.kill()
        await db.close();
        await client.end();

    },
    sanitizeResources: false,
    sanitizeOps: false,
})

Deno.test({
    name: "GetPaperOfProjectStage",
    async fn(): Promise<void> {
        await setup(true);
        let user = await insertUser("test@test", "ash", true, "Test", "Tester", "active");
        let project = await Project.create({
            name: "test",
            minCountReviewers: 1,
            countDecisiveReviewers: 1
        })
        let stage = await Stage.create({
            name: "TestStage",
            projectId: Number(project.id),
            number: 0
        })
        let paper = await Paper.create({
            doi: "fasfafraf",
            title: "nice title",
            abstract: "hellu"

        })
        let author = await Author.create({
            rawString: "Awsome Name"
        })
        let pp = await PaperScopeForStage.create({
            paperId: Number(paper.id),
            stageId: Number(stage.id)
        })
        let wrote = await Wrote.create({ authorId: Number(author.id), paperId: Number(paper.id) })
        let app = await createMockApp();
        let token = await createJWT(user)
        let ctx = await createMockContext(app, `{"doi":"10.1109/SEAA.2009.60" }`, [["Content-Type", "application/json"]], "/", token);
        let bla = await getPaperOfProjectStage(ctx, Number(project.id), Number(stage.id), Number(pp.id))

        let answer = JSON.parse(ctx.response.body as string)
        assertEquals(answer.title, "nice title")
        assertEquals(answer.abstract, "hellu")
        assertEquals(answer.authors[0].rawString, "Awsome Name")
        paperCache.clear()
        authorCache.clear()
        Batcher.kill()
        await db.close();
        await client.end();

    },
    sanitizeResources: false,
    sanitizeOps: false,
})

Deno.test({
    name: "GetPaperOfProjectStage#unauth",
    async fn(): Promise<void> {
        await setup(true);
        let user = await insertUser("test@test", "ash", false, "Test", "Tester", "active");
        let project = await Project.create({
            name: "test",
            minCountReviewers: 1,
            countDecisiveReviewers: 1
        })
        let stage = await Stage.create({
            name: "TestStage",
            projectId: Number(project.id),
            number: 0
        })
        let paper = await Paper.create({
            doi: "fasfafraf",
            title: "nice title",
            abstract: "hellu"

        })
        let author = await Author.create({
            rawString: "Awsome Name"
        })
        let pp = await PaperScopeForStage.create({
            paperId: Number(paper.id),
            stageId: Number(stage.id)
        })
        let wrote = await Wrote.create({ authorId: Number(author.id), paperId: Number(paper.id) })
        let app = await createMockApp();
        let token = await createJWT(user)
        let ctx = await createMockContext(app, `{"doi":"10.1109/SEAA.2009.60" }`, [["Content-Type", "application/json"]], "/", token);
        let bla = await getPaperOfProjectStage(ctx, Number(project.id), Number(stage.id), Number(pp.id))

        assertEquals(ctx.response.status, 401)
        paperCache.clear()
        authorCache.clear()
        Batcher.kill()
        await db.close();
        await client.end();

    },
    sanitizeResources: false,
    sanitizeOps: false,
})

Deno.test({
    name: "GetPaperOfProjectStageNoId",
    async fn(): Promise<void> {
        await setup(true);
        let user = await insertUser("test@test", "ash", false, "Test", "Tester", "active");
        let project = await Project.create({
            name: "test",
            minCountReviewers: 1,
            countDecisiveReviewers: 1
        })
        let stage = await Stage.create({
            name: "TestStage",
            projectId: Number(project.id),
            number: 0
        })
        let paper = await Paper.create({
            doi: "fasfafraf",
            title: "nice title",
            abstract: "hellu"

        })
        let author = await Author.create({
            rawString: "Awsome Name"
        })
        await PaperScopeForStage.create({
            paperId: Number(paper.id),
            stageId: Number(stage.id)
        })
        let wrote = await Wrote.create({ authorId: Number(author.id), paperId: Number(paper.id) })
        let app = await createMockApp();
        let token = await createJWT(user)
        let ctx = await createMockContext(app, `{"doi":"10.1109/SEAA.2009.60" }`, [["Content-Type", "application/json"]], "/", token);
        let bla = await getPaperOfProjectStage(ctx, Number(project.id), Number(stage.id), undefined)

        assertEquals(ctx.response.status, 422)
        paperCache.clear()
        authorCache.clear()
        Batcher.kill()
        await db.close();
        await client.end();

    },
    sanitizeResources: false,
    sanitizeOps: false,
})

Deno.test({
    name: "GetPapersOfProjectStage#unauth",
    async fn(): Promise<void> {
        await setup(true);
        let user = await insertUser("test@test", "ash", false, "Test", "Tester", "active");
        let project = await Project.create({
            name: "test",
            minCountReviewers: 1,
            countDecisiveReviewers: 1
        })
        let stage = await Stage.create({
            name: "TestStage",
            projectId: Number(project.id),
            number: 0
        })
        let paper = await Paper.create({
            doi: "fasfafraf",
            title: "nice title",
            abstract: "hellu"

        })
        let author = await Author.create({
            rawString: "Awsome Name"
        })
        let wrote = await Wrote.create({ authorId: Number(author.id), paperId: Number(paper.id) })
        await PaperScopeForStage.create({
            paperId: Number(paper.id),
            stageId: Number(stage.id)
        })
        let app = await createMockApp();
        let token = await createJWT(user)
        let ctx = await createMockContext(app, `{"doi":"10.1109/SEAA.2009.60" }`, [["Content-Type", "application/json"]], "/", token);
        let bla = await getPapersOfProjectStage(ctx, Number(project.id), Number(stage.id))

        assertEquals(ctx.response.status, 401)
        paperCache.clear()
        authorCache.clear()
        Batcher.kill()
        await db.close();
        await client.end();

    },
    sanitizeResources: false,
    sanitizeOps: false,
})

Deno.test({
    name: "GetPapersOfProjectStageNoId",
    async fn(): Promise<void> {
        await setup(true);
        let user = await insertUser("test@test", "ash", false, "Test", "Tester", "active");
        let project = await Project.create({
            name: "test",
            minCountReviewers: 1,
            countDecisiveReviewers: 1
        })
        let stage = await Stage.create({
            name: "TestStage",
            projectId: Number(project.id),
            number: 0
        })
        let paper = await Paper.create({
            doi: "fasfafraf",
            title: "nice title",
            abstract: "hellu"

        })
        let author = await Author.create({
            rawString: "Awsome Name"
        })
        await PaperScopeForStage.create({
            paperId: Number(paper.id),
            stageId: Number(stage.id)
        })
        let wrote = await Wrote.create({ authorId: Number(author.id), paperId: Number(paper.id) })
        let app = await createMockApp();
        let token = await createJWT(user)
        let ctx = await createMockContext(app, `{"doi":"10.1109/SEAA.2009.60" }`, [["Content-Type", "application/json"]], "/", token);
        let bla = await getPapersOfProjectStage(ctx, Number(project.id), undefined)

        assertEquals(ctx.response.status, 422)
        paperCache.clear()
        authorCache.clear()
        Batcher.kill()
        await db.close();
        await client.end();

    },
    sanitizeResources: false,
    sanitizeOps: false,
})

Deno.test({
    name: "PatchPaperOfProjectStage",
    async fn(): Promise<void> {
        await setup(true);
        let user = await insertUser("test@test", "ash", true, "Test", "Tester", "active");
        let project = await Project.create({
            name: "test",
            minCountReviewers: 1,
            countDecisiveReviewers: 1
        })
        let stage = await Stage.create({
            name: "TestStage",
            projectId: Number(project.id),
            number: 0
        })
        let paper = await Paper.create({
            doi: "fasfafraf",
            title: "nice title",
            abstract: "hellu"

        })
        let author = await Author.create({
            rawString: "Awsome Name"
        })
        let pp = await PaperScopeForStage.create({
            paperId: Number(paper.id),
            stageId: Number(stage.id)
        })
        let wrote = await Wrote.create({ authorId: Number(author.id), paperId: Number(paper.id) })
        let app = await createMockApp();
        let token = await createJWT(user)
        let ctx = await createMockContext(app, `{"title":"New Title" }`, [["Content-Type", "application/json"]], "/", token);
        let bla = await patchPaperOfProjectStage(ctx, Number(project.id), Number(stage.id), Number(pp.id))

        assertEquals(ctx.response.status, 200)
        let paperFinal = await Paper.find(Number(paper.id))
        assertEquals(paperFinal.title, "New Title")
        paperCache.clear()
        authorCache.clear()
        Batcher.kill()
        await db.close();
        await client.end();

    },
    sanitizeResources: false,
    sanitizeOps: false,
})


Deno.test({
    name: "PatchPapersOfProjectStageNoId",
    async fn(): Promise<void> {
        await setup(true);
        let user = await insertUser("test@test", "ash", true, "Test", "Tester", "active");
        let project = await Project.create({
            name: "test",
            minCountReviewers: 1,
            countDecisiveReviewers: 1
        })
        let stage = await Stage.create({
            name: "TestStage",
            projectId: Number(project.id),
            number: 0
        })
        let paper = await Paper.create({
            doi: "fasfafraf",
            title: "nice title",
            abstract: "hellu"

        })
        let author = await Author.create({
            rawString: "Awsome Name"
        })
        let pp = await PaperScopeForStage.create({
            paperId: Number(paper.id),
            stageId: Number(stage.id)
        })
        let wrote = await Wrote.create({ authorId: Number(author.id), paperId: Number(paper.id) })
        let app = await createMockApp();
        let token = await createJWT(user)
        let ctx = await createMockContext(app, `{"doi":"10.1109/SEAA.2009.60" }`, [["Content-Type", "application/json"]], "/", token);
        let bla = await patchPaperOfProjectStage(ctx, Number(project.id), Number(stage.id), undefined)

        assertEquals(ctx.response.status, 422)
        paperCache.clear()
        authorCache.clear()
        Batcher.kill()
        await db.close();
        await client.end();

    },
    sanitizeResources: false,
    sanitizeOps: false,
})

Deno.test({
    name: "PatchPapersOfProjectStageUnAuthorized",
    async fn(): Promise<void> {
        await setup(true);
        let user = await insertUser("test@test", "ash", false, "Test", "Tester", "active");
        let project = await Project.create({
            name: "test",
            minCountReviewers: 1,
            countDecisiveReviewers: 1
        })
        let stage = await Stage.create({
            name: "TestStage",
            projectId: Number(project.id),
            number: 0
        })
        let paper = await Paper.create({
            doi: "fasfafraf",
            title: "nice title",
            abstract: "hellu"

        })
        let author = await Author.create({
            rawString: "Awsome Name"
        })
        await PaperScopeForStage.create({
            paperId: Number(paper.id),
            stageId: Number(stage.id)
        })
        let wrote = await Wrote.create({ authorId: Number(author.id), paperId: Number(paper.id) })
        let app = await createMockApp();
        let token = await createJWT(user)
        let ctx = await createMockContext(app, `{"doi":"10.1109/SEAA.2009.60" }`, [["Content-Type", "application/json"]], "/", token);
        let bla = await patchPaperOfProjectStage(ctx, Number(project.id), Number(stage.id), Number(wrote.id))

        assertEquals(ctx.response.status, 401)
        paperCache.clear()
        authorCache.clear()
        Batcher.kill()
        await db.close();
        await client.end();

    },
    sanitizeResources: false,
    sanitizeOps: false,
})

Deno.test({
    name: "PatchPapersOfProjectStageNoPaper",
    async fn(): Promise<void> {
        await setup(true);
        let user = await insertUser("test@test", "ash", true, "Test", "Tester", "active");
        let project = await Project.create({
            name: "test",
            minCountReviewers: 1,
            countDecisiveReviewers: 1
        })
        let stage = await Stage.create({
            name: "TestStage",
            projectId: Number(project.id),
            number: 0
        })
        let paper = await Paper.create({
            doi: "fasfafraf",
            title: "nice title",
            abstract: "hellu"

        })
        let author = await Author.create({
            rawString: "Awsome Name"
        })
        let pp = await PaperScopeForStage.create({
            paperId: Number(paper.id),
            stageId: Number(stage.id)
        })
        let wrote = await Wrote.create({ authorId: Number(author.id), paperId: Number(paper.id) })
        let app = await createMockApp();
        let token = await createJWT(user)
        let ctx = await createMockContext(app, `{"doi":"10.1109/SEAA.2009.60" }`, [["Content-Type", "application/json"]], "/", token);
        let bla = await patchPaperOfProjectStage(ctx, Number(project.id), Number(stage.id), Number(pp.id) + 1)

        assertEquals(ctx.response.status, 404)
        paperCache.clear()
        authorCache.clear()
        Batcher.kill()
        await db.close();
        await client.end();

    },
    sanitizeResources: false,
    sanitizeOps: false,
})
