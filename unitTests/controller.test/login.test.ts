import { insertUser } from "../../src/controller/databaseFetcher/user.ts";
import { setup } from "../../src/helper/setup.ts";
import { login } from "../../src/controller/login.controller.ts";
import { createMockContext } from "../mockObjects/oak/mockContext.test.ts";
import { createMockApp } from "../mockObjects/oak/mockApp.test.ts";
import { assertEquals, assertNotEquals } from "https://deno.land/std@0.150.0/testing/asserts.ts"
import { client, db } from "../../src/controller/database.controller.ts";
import { Batcher } from "../../src/controller/fetch.controller.ts";

Deno.test({
    name: "testLoginAllowed",
    async fn(): Promise<void> {
        await setup(true);
        await insertUser("test@test", "ash", true, "Test", "Tester", "active");

        let app = await createMockApp();
        let ctx = await createMockContext(app, `{"email": "test@test", "password":"ash"}`);

        let loginWorked: boolean = await login(ctx);

        assertEquals(true, loginWorked);
        let answer = JSON.parse(ctx.response.body as string)
        assertEquals(answer.user.email, "test@test")
        assertEquals(answer.user.isAdmin, true)
        assertEquals(answer.user.firstName, "Test")
        assertEquals(answer.user.lastName, "Tester")
        assertEquals(answer.user.status, "active")
        assertEquals(answer.user.password, undefined)
        assertNotEquals(answer.token, undefined)
        Batcher.kill()
        await db.close();
        await client.end();

    },
    sanitizeResources: false,
    sanitizeOps: false,

})


Deno.test({
    name: "testLoginDenied",
    async fn(): Promise<void> {
        await setup(true);
        await insertUser("test@test", "ash", true, "Test", "Tester", "active");

        let app = await createMockApp();
        let ctx = await createMockContext(app, `{"email": "test@test12", "password":"ash"}`);

        let loginWorked: boolean = await login(ctx);

        assertEquals(loginWorked, false);
        Batcher.kill()
        await db.close();
        await client.end();
    },
    sanitizeResources: false,
    sanitizeOps: false,

})


Deno.test({
    name: "testLoginOnlyEmail",
    async fn(): Promise<void> {
        await setup(true);
        await insertUser("test@test", "ash", true, "Test", "Tester", "registered");

        let app = await createMockApp();
        let ctx = await createMockContext(app, `{"email": "test@test"}`);

        let loginWorked: boolean = await login(ctx);

        assertEquals(loginWorked, false);
        Batcher.kill()
        await db.close();
        await client.end();
    },
    sanitizeResources: false,
    sanitizeOps: false,

})
