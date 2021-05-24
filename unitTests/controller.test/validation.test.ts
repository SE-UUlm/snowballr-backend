import {createMockApp} from "../mockObjects/oak/mockApp.ts";
import {createMockContext} from "../mockObjects/oak/mockContext.ts";
import {validateContentType} from "../../src/controller/validation.ts";
import { assertEquals } from "https://deno.land/std@0.97.0/testing/asserts.ts"
import {emptyAsyncFunction} from "../mockObjects/emptyAsyncFunction.ts";
Deno.test({
    name: "testCorrectContentTypeAndContent",
    async fn(): Promise<void> {
        let app = await createMockApp();
        let ctx = await createMockContext(app,`{"email": "test@test", "password":"ash"}`);
        await validateContentType(ctx, emptyAsyncFunction)

        assertEquals(200, ctx.response.status)
    }

})

Deno.test({
    name: "testCorrectContentTypeWrongContent",
    async fn(): Promise<void> {
        let app = await createMockApp();
        let ctx = await createMockContext(app,`{"email": "test@test", "password":"ash"`);
        await validateContentType(ctx, emptyAsyncFunction)

        assertEquals(415, ctx.response.status)
    }

})

Deno.test({
    name: "testWrongContentType",
    async fn(): Promise<void> {
        let app = await createMockApp();
        let ctx = await createMockContext(app,`{"email": "test@test", "password":"ash"}`,[["Content-Type", "text"]]);
        await validateContentType(ctx,emptyAsyncFunction)

        assertEquals(415, ctx.response.status)
    }

})

