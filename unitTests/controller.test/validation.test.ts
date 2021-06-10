import {createMockApp} from "../mockObjects/oak/mockApp.test.ts";
import {createMockContext} from "../mockObjects/oak/mockContext.test.ts";
import {createJWT, validateContentType, validateJWTIfExists} from "../../src/controller/validation.ts";
import {assertEquals} from "https://deno.land/std@0.97.0/testing/asserts.ts"
import {emptyAsyncFunctionTest} from "../mockObjects/emptyAsyncFunction.test.ts";
import {setup} from "../../src/helper/setup.ts";
import {insertUser} from "../../src/controller/databaseFetcher/user.ts";
import {Project} from "../../src/model/db/project.ts";
import {UserIsPartOfProject} from "../../src/model/db/userIsPartOfProject.ts";

Deno.test({
    name: "testCorrectContentTypeAndContent",
    async fn(): Promise<void> {
        let app = await createMockApp();
        let ctx = await createMockContext(app, `{"email": "test@test", "password":"ash"}`);
        await validateContentType(ctx, emptyAsyncFunctionTest)

        assertEquals(ctx.response.status, 200)
    }

})

Deno.test({
    name: "testEmptyContent",
    async fn(): Promise<void> {
        let app = await createMockApp();
        let ctx = await createMockContext(app, undefined);
        await validateContentType(ctx, emptyAsyncFunctionTest)

        assertEquals(ctx.response.status, 200)
    }

})

Deno.test({
    name: "testCorrectContentTypeWrongContent",
    async fn(): Promise<void> {
        let app = await createMockApp();
        let ctx = await createMockContext(app, `{"email": "test@test", "password":"ash"`);
        await validateContentType(ctx, emptyAsyncFunctionTest)

        assertEquals(ctx.response.status, 415)
    }

})

Deno.test({
    name: "testWrongContentType",
    async fn(): Promise<void> {
        let app = await createMockApp();
        let ctx = await createMockContext(app, `{"email": "test@test", "password":"ash"}`, [["Content-Type", "text"]]);
        await validateContentType(ctx, emptyAsyncFunctionTest)

        assertEquals(ctx.response.status, 415)
    }

})

Deno.test({
    name: "loginPageUnauthorizedAllowed",
    async fn(): Promise<void> {
        let app = await createMockApp();
        let ctx = await createMockContext(app, `{"email": "test@test", "password":"ash"}`, [["Content-Type", "text"]], "/login/");
        await validateJWTIfExists(ctx, emptyAsyncFunctionTest)

        assertEquals(ctx.response.status, 200)
    }

})

Deno.test({
    name: "UnauthorizedRequest",
    async fn(): Promise<void> {
        let app = await createMockApp();
        let ctx = await createMockContext(app, `{"email": "test@test", "password":"ash"}`, [["Content-Type", "text"]]);
        await validateJWTIfExists(ctx, emptyAsyncFunctionTest)

        assertEquals(ctx.response.status, 401)
    }

})

Deno.test({
    name: "authorizedRequest",
    async fn(): Promise<void> {
        await setup(true);
        let user = await insertUser("test@test", "ash", true, "Test", "Tester", "active");

        let app = await createMockApp();
        let token = await createJWT(user)
        let ctx = await createMockContext(app, undefined, [["Content-Type", "text"]], "/", token);
        await validateJWTIfExists(ctx, emptyAsyncFunctionTest)

        assertEquals(ctx.response.status, 200)
    },
    sanitizeResources: false,
})

Deno.test({
    name: "unAuthorizedRequestWithWrongToken",
    async fn(): Promise<void> {
        await setup(true);
        let user = await insertUser("test@test", "ash", true, "Test", "Tester", "active");

        let app = await createMockApp();
        let token = await createJWT(user)
        let ctx = await createMockContext(app, undefined, [["Content-Type", "text"]], "/", token + "a");
        await validateJWTIfExists(ctx, emptyAsyncFunctionTest)

        assertEquals(ctx.response.status, 401)
    },
})

Deno.test({
    name: "isPO",
    async fn(): Promise<void> {
        await setup(true);
        let user = await insertUser("test@test", "ash", false, "Test", "Tester", "active");
        let app = await createMockApp();
        let token = await createJWT(user)
        let project = await Project.create({name: "bla"})
        let userIsPartOfProject = await UserIsPartOfProject.create({
            isOwner: true,
            userId: Number(user.id),
            projectId: Number(project.id)
        })
        let ctx = await createMockContext(app, undefined, [["Content-Type", "text"]], "/", token);
        assertEquals(await checkPO(ctx), true)

    }

})

Deno.test({
    name: "isNoPO",
    async fn(): Promise<void> {
        await setup(true);
        let user = await insertUser("test@test", "ash", false, "Test", "Tester", "active");
        let app = await createMockApp();
        let token = await createJWT(user)
        let project = await Project.create({name: "bla"})
        let userIsPartOfProject = await UserIsPartOfProject.create({
            isOwner: false,
            userId: Number(user.id),
            projectId: Number(project.id)
        })
        let ctx = await createMockContext(app, undefined, [["Content-Type", "text"]], "/", token);
        assertEquals(await checkPO(ctx), false)

    }

})
