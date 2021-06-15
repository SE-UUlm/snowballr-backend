import {insertUser} from "../../src/controller/databaseFetcher/user.ts";
import {setup} from "../../src/helper/setup.ts";
import {login} from "../../src/controller/login.ts";
import {createMockContext} from "../mockObjects/oak/mockContext.test.ts";
import {createMockApp} from "../mockObjects/oak/mockApp.test.ts";
import {assertEquals, assertNotEquals} from "https://deno.land/std@0.97.0/testing/asserts.ts"
import {db} from "../../src/controller/database.ts";


Deno.test({
    name: "testLoginAllowed",
    async fn(): Promise<void> {
        await setup(true);
        await insertUser("test@test", "ash", true, "Test", "Tester", "active");

        let app = await createMockApp();
        let ctx = await createMockContext(app, `{"email": "test@test", "password":"ash"}`);

        let loginWorked: boolean = await login(ctx);

        assertEquals(true, loginWorked);
        db.close();

    }

})


Deno.test({
    name: "testLoginDenied",
    async fn(): Promise<void> {
        await setup(true);
        await insertUser("test@test", "ash", true, "Test", "Tester", "active");

        let app = await createMockApp();
        let ctx = await createMockContext(app, `{"email": "test@test12", "password":"ash"}`);

        let loginWorked: boolean = await login(ctx);

        assertNotEquals(loginWorked, true);
        db.close();

    }

})


Deno.test({
    name: "testLoginOnlyEmail",
    async fn(): Promise<void> {
        await setup(true);
        await insertUser("test@test", "ash", true, "Test", "Tester", "registered");

        let app = await createMockApp();
        let ctx = await createMockContext(app, `{"email": "test@test"}`);

        let loginWorked: boolean = await login(ctx);

        assertNotEquals(loginWorked, true);
        db.close();
    }

})
