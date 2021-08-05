import { assertEquals, assertNotEquals } from "https://deno.land/std@0.83.0/testing/asserts.ts";
import { db,client, saveChildren } from "../../src/controller/database.ts";
import { insertUser } from "../../src/controller/databaseFetcher/user.ts";
import { Batcher } from "../../src/controller/fetch.ts";
import { getPaper, getPaperCitations, getPaperReferences, getPapers } from "../../src/controller/paper.ts";
import { createJWT } from "../../src/controller/validation.ts";
import { setup } from "../../src/helper/setup.ts";
import { Paper } from "../../src/model/db/paper.ts";
import { createMockApp } from "../mockObjects/oak/mockApp.test.ts";
import { createMockContext } from "../mockObjects/oak/mockContext.test.ts";

Deno.test({
    name: "getAllPapers",
    async fn(): Promise<void> {
        await setup(true);
        let user = await insertUser("test@test", "ash", true, "Test", "Tester", "active");

        let paperNumber = (await Paper.all()).length
        let app = await createMockApp();
        await Paper.create({})
        await Paper.create({})
        await Paper.create({})
        await Paper.create({})

        let token = await createJWT(user)
        let ctx = await createMockContext(app, ``, [["Content-Type", "application/json"]], "/", token);
        await getPapers(ctx)
        let answer = JSON.parse(ctx.response.body as string)
        assertEquals(ctx.response.status, 200)
        assertEquals(answer.papers.length, paperNumber + 4)
        await db.close();
        await client.end();
        Batcher.kill()
    },
    sanitizeResources: false,
    sanitizeOps: false,
})

Deno.test({
    name: "getPaper",
    async fn(): Promise<void> {
        await setup(true);
        let user = await insertUser("test@test", "ash", true, "Test", "Tester", "active");

        let paperNumber = (await Paper.all()).length
        let app = await createMockApp();
        let paper = await Paper.create({title: "Hello there", abstract: "General Kenobi"})

        let token = await createJWT(user)
        let ctx = await createMockContext(app, `{}`, [["Content-Type", "application/json"]], "/", token);
        await getPaper(ctx, Number(paper.id))
        let answer = JSON.parse(ctx.response.body as string)
        assertEquals(ctx.response.status, 200)
        assertEquals(answer.title, "Hello there")
        assertEquals(answer.abstract, "General Kenobi")
        assertEquals(answer.ppid, undefined)
        assertEquals(answer.id, Number(paper.id))
        await db.close();
        await client.end();
        Batcher.kill()
    },
    sanitizeResources: false,
    sanitizeOps: false,
})

Deno.test({
    name: "getPaperNoId",
    async fn(): Promise<void> {
        await setup(true);
        let user = await insertUser("test@test", "ash", true, "Test", "Tester", "active");

        let app = await createMockApp();
        await Paper.create({title: "Hello there", abstract: "General Kenobi"})

        let token = await createJWT(user)
        let ctx = await createMockContext(app, `{}`, [["Content-Type", "application/json"]], "/", token);
        await getPaper(ctx, undefined)
        assertEquals(ctx.response.status, 422)
        Batcher.kill()
        await db.close();
        await client.end();
    },
    sanitizeResources: false,
    sanitizeOps: false,
})

Deno.test({
    name: "getPaperWrongId",
    async fn(): Promise<void> {
        await setup(true);
        let user = await insertUser("test@test", "ash", true, "Test", "Tester", "active");
        let app = await createMockApp();
        let paper = await Paper.create({title: "Hello there", abstract: "General Kenobi"})

        let token = await createJWT(user)
        let ctx = await createMockContext(app, ``, [["Content-Type", "application/json"]], "/", token);
        await getPaper(ctx, Number(paper.id) + 1)
        assertEquals(ctx.response.status, 404)
        Batcher.kill()
        await db.close();
        await client.end();
    },
    sanitizeResources: false,
    sanitizeOps: false,
})

Deno.test({
    name: "getPaperReferences",
    async fn(): Promise<void> {
        await setup(true);
        let user = await insertUser("test@test", "ash", true, "Test", "Tester", "active");

        let app = await createMockApp();
        let paper = await Paper.create({title: "Hello there", abstract: "General Kenobi"})
        let paper1 = await Paper.create({title: "Hi", abstract: "this abstract"})
        let paper2 = await Paper.create({title: "mlem", abstract: "mlem mlem"})
        await saveChildren("referencedby", "paperreferencedid", "paperreferencingid", Number(paper.id), Number(paper1.id))
        await saveChildren("referencedby", "paperreferencedid", "paperreferencingid", Number(paper.id), Number(paper2.id))
        let token = await createJWT(user)
        let ctx = await createMockContext(app, ``, [["Content-Type", "application/json"]], "/", token);
        await getPaperReferences(ctx, Number(paper.id))
        let answer = JSON.parse(ctx.response.body as string)
        assertEquals(ctx.response.status, 200)
        assertEquals(answer.papers.length, 2)
        assertEquals(answer.papers[0].title, "Hi")
        assertEquals(answer.papers[0].id, Number(paper1.id))
        assertEquals(answer.papers[1].title, "mlem")
        assertEquals(answer.papers[1].abstract, "mlem mlem")
        await db.close();
        await client.end();
        Batcher.kill()
    },
    sanitizeResources: false,
    sanitizeOps: false,
})

Deno.test({
    name: "getPaperReferencesNoId",
    async fn(): Promise<void> {
        await setup(true);
        let user = await insertUser("test@test", "ash", true, "Test", "Tester", "active");

        let app = await createMockApp();
        let paper = await Paper.create({title: "Hello there", abstract: "General Kenobi"})
        let paper1 = await Paper.create({title: "Hi", abstract: "this abstract"})
        let paper2 = await Paper.create({title: "mlem", abstract: "mlem mlem"})
        await saveChildren("referencedby", "paperreferencedid", "paperreferencingid", Number(paper.id), Number(paper1.id))
        await saveChildren("referencedby", "paperreferencedid", "paperreferencingid", Number(paper.id), Number(paper2.id))
        let token = await createJWT(user)
        let ctx = await createMockContext(app, ``, [["Content-Type", "application/json"]], "/", token);
        await getPaperReferences(ctx, undefined)

        assertEquals(ctx.response.status, 422)

        await db.close();
        await client.end();
        Batcher.kill()
    },
    sanitizeResources: false,
    sanitizeOps: false,
})

Deno.test({
    name: "getPaperReferencesNoId",
    async fn(): Promise<void> {
        await setup(true);
        let user = await insertUser("test@test", "ash", true, "Test", "Tester", "active");

        let app = await createMockApp();
        let paper = await Paper.create({title: "Hello there", abstract: "General Kenobi"})
        let paper1 = await Paper.create({title: "Hi", abstract: "this abstract"})
        let paper2 = await Paper.create({title: "mlem", abstract: "mlem mlem"})
        await saveChildren("referencedby", "paperreferencedid", "paperreferencingid", Number(paper.id), Number(paper1.id))
        await saveChildren("referencedby", "paperreferencedid", "paperreferencingid", Number(paper.id), Number(paper2.id))
        let token = await createJWT(user)
        let ctx = await createMockContext(app, ``, [["Content-Type", "application/json"]], "/", token);
        await getPaperCitations(ctx, undefined)

        assertEquals(ctx.response.status, 422)

        await db.close();
        await client.end();
        Batcher.kill()
    },
    sanitizeResources: false,
    sanitizeOps: false,
})

