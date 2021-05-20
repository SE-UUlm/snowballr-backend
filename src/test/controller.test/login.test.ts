import {insertUser} from "../../controller/databaseFetcher/user.ts";
import {setup} from "../../helper/setup.ts";
import {login} from "../../controller/login.ts";
import {createMockContext} from "../mockObjects/oak/mockContext.ts";
import {createMockApp} from "../mockObjects/oak/mockApp.ts";
import { assertEquals, assertNotEquals } from "https://deno.land/std@0.97.0/testing/asserts.ts"
import {db} from "../../controller/database.ts";
import {RequestBodyMock1, RequestBodyMock2, RequestBodyMock3} from "../mockObjects/oak/mockBody.ts";

Deno.test({
    name: "testLoginAllowed",
    async fn(): Promise<void> {
        await setup(true);
        await insertUser("test@test", "ash", true, "Test", "Tester", "registered");

        let app = await createMockApp();
        let ctx = await createMockContext(app, new RequestBodyMock2());

        let loginWorked: boolean = await login(ctx);

       assertEquals(true, loginWorked);
        db.close();

    }

})

Deno.test({
    name: "testLoginDenied",
    async fn(): Promise<void> {
        await setup(true);
        await insertUser("test@test", "ash", true, "Test", "Tester", "registered");

        let app = await createMockApp();
        let ctx = await createMockContext(app, new RequestBodyMock3());

        let loginWorked: boolean = await login(ctx);

        assertNotEquals(true, loginWorked);
        db.close();

    }

})

Deno.test({
    name: "testLoginOnlyEmail",
    async fn(): Promise<void> {
        await setup(true);
        await insertUser("test@test", "ash", true, "Test", "Tester", "registered");

        let app = await createMockApp();
        let ctx = await createMockContext(app,new RequestBodyMock1());

        let loginWorked: boolean = await login(ctx);

        assertNotEquals(true, loginWorked);
        db.close();
    }

})
