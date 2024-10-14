import { assertEquals } from "https://deno.land/std@0.150.0/testing/asserts.ts";
import { getDOI } from "../../src/api/apiMerger.ts";
import { Batcher, getActiveBatches, makeFetching } from "../../src/controller/fetch.controller.ts"
import { insertUser } from "../../src/controller/databaseFetcher/user.ts";
import { db, client } from "../../src/controller/database.controller.ts"
import { setup } from "../../src/helper/setup.ts";
import { createMockApp } from "../mockObjects/oak/mockApp.test.ts";
import { createJWT } from "../../src/controller/validation.controller.ts";
import { createMockContext } from "../mockObjects/oak/mockContext.test.ts";
import { SourceApi } from "../../src/api/iApiPaper.ts";

Deno.test({
    name: "GetDoiByTitleAndName",
    async fn(): Promise<void> {
        await setup(true);
        let batch = await makeFetching(0.8, [[SourceApi.CR, "luca999@web.de"], [SourceApi.IE, "4yk5d9an52ejynjsmzqxe62r"], [SourceApi.MA, "9a02225751354cd29397eba3f5382101"], [SourceApi.OC], [SourceApi.S2]], undefined, "Translation of UML 2 Activity Diagrams into Finite State Machines for Model Checking", "alexander raschke")
        assertEquals(getDOI((await batch.response)[0].paper)[0].toUpperCase(), "10.1109/SEAA.2009.60")

        await db.close();
        await client.end();
        Batcher.kill()
    },
    sanitizeResources: false,
    sanitizeOps: false,
})

Deno.test({
    name: "GetCurrentBatches",
    async fn(): Promise<void> {
        await setup(true);
        let user = await insertUser("test@test", "ash", true, "Test", "Tester", "active");

        let app = await createMockApp();
        let token = await createJWT(user)
        let ctx = await createMockContext(app, `{}`, [["Content-Type", "application/json"]], "/", token);

        let batch = await makeFetching(0.8, [[SourceApi.CR, "luca999@web.de"], [SourceApi.IE, "4yk5d9an52ejynjsmzqxe62r"], [SourceApi.MA, "9a02225751354cd29397eba3f5382101"], [SourceApi.OC], [SourceApi.S2]], undefined, "Translation of UML 2 Activity Diagrams into Finite State Machines for Model Checking", "alexander raschke", "DAWDADW")
        getActiveBatches(ctx)
        let answer = JSON.parse(ctx.response.body as string)
        assertEquals(ctx.response.status, 200)
        assertEquals(answer.batches.length, 1)
        await batch;
        await db.close();
        await client.end();
        Batcher.kill()
    },
    sanitizeResources: false,
    sanitizeOps: false,
})

