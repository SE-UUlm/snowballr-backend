import {createMockApp} from "../mockObjects/oak/mockApp.test.ts";
import {createMockContext} from "../mockObjects/oak/mockContext.test.ts";
import {createJWT, validateContentType, validateJWTIfExists} from "../../src/controller/validation.ts";
import { assertEquals } from "https://deno.land/std@0.97.0/testing/asserts.ts"
import {emptyAsyncFunctionTest} from "../mockObjects/emptyAsyncFunction.test.ts";
import {insertUser} from "../../src/controller/databaseFetcher/user.ts";
import {createTerminationDate} from "../../src/helper/dateHelper.ts";
import {setup} from "../../src/helper/setup.ts";
import {db} from "../../src/controller/database.ts";
Deno.test({
    name: "testCorrectContentTypeAndContent",
    async fn(): Promise<void> {
        let app = await createMockApp();
        let ctx = await createMockContext(app,`{"email": "test@test", "password":"ash"}`);
        await validateContentType(ctx, emptyAsyncFunctionTest)

        assertEquals(200, ctx.response.status)
    }

})

Deno.test({
    name: "testCorrectContentTypeWrongContent",
    async fn(): Promise<void> {
        let app = await createMockApp();
        let ctx = await createMockContext(app,`{"email": "test@test", "password":"ash"`);
        await validateContentType(ctx, emptyAsyncFunctionTest)

        assertEquals(415, ctx.response.status)
    }

})

Deno.test({
    name: "testWrongContentType",
    async fn(): Promise<void> {
        let app = await createMockApp();
        let ctx = await createMockContext(app,`{"email": "test@test", "password":"ash"}`,[["Content-Type", "text"]]);
        await validateContentType(ctx,emptyAsyncFunctionTest)

        assertEquals(415, ctx.response.status)
    }

})