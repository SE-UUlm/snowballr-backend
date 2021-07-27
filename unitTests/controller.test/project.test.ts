import { setup } from "../../src/helper/setup.ts";
import { insertUser } from "../../src/controller/databaseFetcher/user.ts";
import { createMockApp } from "../mockObjects/oak/mockApp.test.ts";
import { checkMemberOfProject, createJWT } from "../../src/controller/validation.ts";
import { createMockContext } from "../mockObjects/oak/mockContext.test.ts";
import {
    addMemberToProject,
    addPaperToProjectStage,
    addStageToProject,
    createProject,
    getMembersOfProject,
    getProjects
} from "../../src/controller/project.ts";
import { Project } from "../../src/model/db/project.ts";
import { assertEquals, assertNotEquals } from "https://deno.land/std/testing/asserts.ts"
import { UserIsPartOfProject } from "../../src/model/db/userIsPartOfProject.ts";
import { client, db } from "../../src/controller/database.ts";
import { Stage } from "../../src/model/db/stage.ts";
import { Batcher } from "../../src/controller/fetch.ts";
/*
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
})

Deno.test({
    name: "getMembersUnauthorized",
    async fn(): Promise<void> {
        await setup(true);
        let user = await insertUser("test@test", "ash", false, "Test", "Tester", "active");
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
    }
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
        let ctx = await createMockContext(app, `{ "id": "${Number(user2.id)}"}`, [["Content-Type", "application/json"]], "/", token);
        await addMemberToProject(ctx, Number(project.id))
        let worked = await checkMemberOfProject(Number(project.id), {
            id: Number(user2.id),
            isAdmin: false,
            email: "",
            firstName: "",
            lastName: "",
            status: "active"
        })
        assertEquals(worked, true)

        await db.close();
        await client.end();
    }
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
    }
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
        let ctx = await createMockContext(app, `{}`, [["Content-Type", "application/json"]], "/", token);
        await addMemberToProject(ctx, undefined)
        assertEquals(ctx.response.status, 422)

        await db.close();
        await client.end();
    }
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
    }
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
    }
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
    }
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
    }
})
*/
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
        await addPaperToProjectStage(ctx, Number(project.id), Number(stage.id), true)

        assertEquals(ctx.response.status, 200)

        Batcher.kill()
        await db.close();
        await client.end();

    },
    sanitizeResources: false,
    sanitizeOps: false,
})
