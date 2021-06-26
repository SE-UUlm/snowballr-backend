import {setup} from "../../src/helper/setup.ts";
import {insertUser} from "../../src/controller/databaseFetcher/user.ts";
import {createMockApp} from "../mockObjects/oak/mockApp.test.ts";
import {checkMemberOfProject, createJWT} from "../../src/controller/validation.ts";
import {createMockContext} from "../mockObjects/oak/mockContext.test.ts";
import {addMemberToProject, getMembersOfProject, getProjects} from "../../src/controller/project.ts";
import {Project} from "../../src/model/db/project.ts";
import {assertEquals, assertNotEquals} from "https://deno.land/std/testing/asserts.ts"
import {UserIsPartOfProject} from "../../src/model/db/userIsPartOfProject.ts";

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
    },
    sanitizeResources: false,
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
    },
    sanitizeResources: false,
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
    },
    sanitizeResources: false,
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
    },

    sanitizeResources: false,
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
        let ctx = await createMockContext(app, `{ id: ${Number(user2.id)}`, [["Content-Type", "application/json"]], "/", token);
        await addMemberToProject(ctx, Number(project.id))

        assertEquals(ctx.response.status, 401)
    },
    sanitizeResources: false,
})