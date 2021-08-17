import { assertEquals } from "https://deno.land/std@0.97.0/testing/asserts.ts";
import { getDOI } from "../../src/api/apiMerger.ts";
import { Batcher, makeFetching } from "../../src/controller/fetch.controller.ts"
import { insertUser } from "../../src/controller/databaseFetcher/user.ts";
import { db, client } from "../../src/controller/database.controller.ts"
import { setup } from "../../src/helper/setup.ts";

Deno.test({
    name: "GetDoiByTitleAndName",
    async fn(): Promise<void> {
        await setup(true);
        let batch = await makeFetching(undefined, "Translation of UML 2 Activity Diagrams into Finite State Machines for Model Checking", "alexander raschke")
        assertEquals(getDOI((await batch.response)[0].paper)[0].toUpperCase(), "10.1109/SEAA.2009.60")

        await db.close();
        await client.end();
        Batcher.kill()
    },
    sanitizeResources: false,
    sanitizeOps: false,
})
