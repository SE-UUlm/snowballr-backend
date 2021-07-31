import { createMockApp } from "../mockObjects/oak/mockApp.test.ts";
import { createMockContext } from "../mockObjects/oak/mockContext.test.ts";
import { assertEquals } from "https://deno.land/std@0.97.0/testing/asserts.ts"
import { jsonBodyToObject } from "../../src/helper/body.ts";

Deno.test({
    name: "testEmptyBody",
    async fn(): Promise<void> {
        let app = await createMockApp();
        let ctx = await createMockContext(app, undefined);
        await jsonBodyToObject(ctx)

        assertEquals(ctx.response.status, 401)
    }

})
