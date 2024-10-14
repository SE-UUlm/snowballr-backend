import { assertEquals } from "https://deno.land/std@0.150.0/testing/asserts.ts";
import { IApiAuthor } from "../../src/api/iApiAuthor.ts";
import { getAuthor, getSourceAuthor, patchAuthor, postAuthor } from "../../src/controller/author.controller.ts";
import { db, client } from "../../src/controller/database.controller.ts";
import { insertUser } from "../../src/controller/databaseFetcher/user.ts";
import { Batcher } from "../../src/controller/fetch.controller.ts";
import { authorCache } from "../../src/controller/project.controller.ts";
import { createJWT } from "../../src/controller/validation.controller.ts";
import { setup } from "../../src/helper/setup.ts";
import { Author } from "../../src/model/db/author.ts";
import { createMockApp } from "../mockObjects/oak/mockApp.test.ts";
import { createMockContext } from "../mockObjects/oak/mockContext.test.ts";


Deno.test({
    name: "postAuthor",
    async fn(): Promise<void> {
        await setup(true);
        let user = await insertUser("test@test", "ash", true, "Test", "Tester", "active");

        let app = await createMockApp();
        let token = await createJWT(user)
        let ctx = await createMockContext(app, `{"firstName":"This", "lastName":"Works", "rawString": "This Works", "orcid":"12ab"}`, [["Content-Type", "application/json"]], "/", token);
        await postAuthor(ctx)
        let answer = JSON.parse(ctx.response.body as string)
        assertEquals(ctx.response.status, 200)
        assertEquals(answer.rawString, "This Works")
        assertEquals(answer.firstName, "This")
        assertEquals(answer.lastName, "Works")
        assertEquals(answer.orcid, "12ab")

        let dbAuthor = await Author.find(answer.id)
        assertEquals(String(dbAuthor.rawString), "This Works")
        assertEquals(String(dbAuthor.firstName), "This")
        assertEquals(String(dbAuthor.lastName), "Works")
        assertEquals(String(dbAuthor.orcid), "12ab")
        await db.close()
        await client.end();
        Batcher.kill()
        authorCache.clear()
    },
    sanitizeResources: false,
    sanitizeOps: false,
})

Deno.test({
    name: "getAuthor",
    async fn(): Promise<void> {
        await setup(true);
        let user = await insertUser("test@test", "ash", true, "Test", "Tester", "active");

        let app = await createMockApp();
        let author = await Author.create({ rawString: "test tester", firstName: "test", lastName: "tester", orcid: "fafadadf" })

        let token = await createJWT(user)
        let ctx = await createMockContext(app, `{}`, [["Content-Type", "application/json"]], "/", token);
        await getAuthor(ctx, Number(author.id))
        let answer = JSON.parse(ctx.response.body as string)
        assertEquals(ctx.response.status, 200)
        assertEquals(answer.rawString, "test tester")
        assertEquals(answer.firstName, "test")
        assertEquals(answer.lastName, "tester")
        assertEquals(answer.orcid, "fafadadf")
        await db.close();
        await client.end();
        Batcher.kill()
        authorCache.clear()
    },
    sanitizeResources: false,
    sanitizeOps: false,
})

Deno.test({
    name: "getAuthorWrongId",
    async fn(): Promise<void> {
        await setup(true);
        let user = await insertUser("test@test", "ash", true, "Test", "Tester", "active");

        let app = await createMockApp();
        let author = await Author.create({ rawString: "test tester", firstName: "test", lastName: "tester", orcid: "fafadadf" })

        let token = await createJWT(user)
        let ctx = await createMockContext(app, `{}`, [["Content-Type", "application/json"]], "/", token);
        await getAuthor(ctx, Number(author.id) + 1)
        assertEquals(ctx.response.status, 404)
        Batcher.kill()
        await db.close();
        await client.end();
        authorCache.clear()
    },
    sanitizeResources: false,
    sanitizeOps: false,
})



Deno.test({
    name: "getAuthorSource",
    async fn(): Promise<void> {
        await setup(true);
        await authorCache.fileCache!.purge()
        let user = await insertUser("test@test", "ash", true, "Test", "Tester", "active");

        let app = await createMockApp();
        let author = await Author.create({ rawString: "test tester", firstName: "test", lastName: "tester", orcid: "fafadadf" })
        let source: IApiAuthor = { rawString: ["test tester", "testing tester"] } as IApiAuthor
        await authorCache.add(String(author.id), source)

        let token = await createJWT(user)
        let ctx = await createMockContext(app, ``, [["Content-Type", "application/json"]], "/", token);
        await getSourceAuthor(ctx, Number(author.id))
        let answer = JSON.parse(ctx.response.body as string)
        assertEquals(ctx.response.status, 200)
        assertEquals(answer.rawString, ["test tester", "testing tester"])

        await db.close();
        await client.end();
        Batcher.kill()
        authorCache.clear()
    },
    sanitizeResources: false,
    sanitizeOps: false,
})

Deno.test({
    name: "getAuthorSourceWrongId",
    async fn(): Promise<void> {
        await setup(true);
        await authorCache.fileCache!.purge()
        let user = await insertUser("test@test", "ash", true, "Test", "Tester", "active");

        let app = await createMockApp();
        let author = await Author.create({ rawString: "test tester", firstName: "test", lastName: "tester", orcid: "fafadadf" })
        let source: IApiAuthor = { rawString: ["test tester", "testing tester"] } as IApiAuthor
        await authorCache.add(String(author.id), source)

        let token = await createJWT(user)
        let ctx = await createMockContext(app, ``, [["Content-Type", "application/json"]], "/", token);
        await getSourceAuthor(ctx, Number(author.id) + 1)
        assertEquals(ctx.response.status, 404)


        await db.close();
        await client.end();
        Batcher.kill()
        authorCache.clear()
    },
    sanitizeResources: false,
    sanitizeOps: false,
})

Deno.test({
    name: "PatchAuthor",
    async fn(): Promise<void> {
        await setup(true);
        let user = await insertUser("test@test", "ash", true, "Test", "Tester", "active");

        let app = await createMockApp();
        let author = await Author.create({ rawString: "test tester", firstName: "test", lastName: "tester", orcid: "fafadadf" })

        let token = await createJWT(user)
        let ctx = await createMockContext(app, `{"rawString":"Test Tester"}`, [["Content-Type", "application/json"]], "/", token);
        await patchAuthor(ctx, Number(author.id))
        assertEquals(ctx.response.status, 200)
        author = await Author.find(Number(author.id))
        assertEquals(String(author.rawString), "Test Tester")


        await db.close();
        await client.end();
        Batcher.kill()
    },
    sanitizeResources: false,
    sanitizeOps: false,
})

Deno.test({
    name: "PatchAuthorWrongId",
    async fn(): Promise<void> {
        await setup(true);
        let user = await insertUser("test@test", "ash", true, "Test", "Tester", "active");

        let app = await createMockApp();
        let author = await Author.create({ rawString: "test tester", firstName: "test", lastName: "tester", orcid: "fafadadf" })

        let token = await createJWT(user)
        let ctx = await createMockContext(app, `{"rawString":"Test Tester"}`, [["Content-Type", "application/json"]], "/", token);
        await patchAuthor(ctx, Number(author.id) + 1)
        assertEquals(ctx.response.status, 404)

        await db.close();
        await client.end();
        Batcher.kill()
    },
    sanitizeResources: false,
    sanitizeOps: false,
})

Deno.test({
    name: "PatchAuthorAuthorCache",
    async fn(): Promise<void> {
        await setup(true);
        let user = await insertUser("test@test", "ash", true, "Test", "Tester", "active");
        let app = await createMockApp();
        let author = await Author.create({ rawString: "test tester", firstName: "test", lastName: "tester", orcid: "fafadadf" })
        await authorCache.add(String(author.id), { rawString: ["test tester", "testing tester"] } as IApiAuthor)
        let token = await createJWT(user)
        let ctx = await createMockContext(app, `{"rawString":"Test Tester"}`, [["Content-Type", "application/json"]], "/", token);
        await getAuthor(ctx, Number(author.id))
        let answer = JSON.parse(ctx.response.body as string)
        assertEquals(answer.status, "unfinished")
        await patchAuthor(ctx, Number(author.id))
        assertEquals(ctx.response.status, 200)
        author = await Author.find(Number(author.id))
        assertEquals(String(author.rawString), "Test Tester")
        let source = authorCache.get(String(author.id))
        assertEquals(source, undefined)
        await getAuthor(ctx, Number(author.id))
        answer = JSON.parse(ctx.response.body as string)
        assertEquals(answer.status, "ready")

        await db.close();
        await client.end();
        Batcher.kill()
    },
    sanitizeResources: false,
    sanitizeOps: false,
})
