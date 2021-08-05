import { assertEquals } from "https://deno.land/std@0.83.0/testing/asserts.ts";
import { IApiAuthor } from "../../src/api/iApiAuthor.ts";
import { getAuthor,getSourceAuthor, patchAuthor } from "../../src/controller/author.controller.ts";
import { db,client } from "../../src/controller/database.ts";
import { insertUser } from "../../src/controller/databaseFetcher/user.ts";
import { Batcher } from "../../src/controller/fetch.ts";
import { authorCache } from "../../src/controller/project.ts";
import { createJWT } from "../../src/controller/validation.ts";
import { setup } from "../../src/helper/setup.ts";
import { Author } from "../../src/model/db/author.ts";
import { createMockApp } from "../mockObjects/oak/mockApp.test.ts";
import { createMockContext } from "../mockObjects/oak/mockContext.test.ts";

Deno.test({
    name: "getAuthor",
    async fn(): Promise<void> {
        await setup(true);
        let user = await insertUser("test@test", "ash", true, "Test", "Tester", "active");

        let app = await createMockApp();
        let author = await Author.create({rawString: "test tester", firstName: "test", lastName: "tester", orcid: "fafadadf"})

        let token = await createJWT(user)
        let ctx = await createMockContext(app, `{}`, [["Content-Type", "application/json"]], "/", token);
        await getAuthor(ctx, Number(author.id))
        let answer = JSON.parse(ctx.response.body as string)
        assertEquals(ctx.response.status, 200)
        assertEquals(answer.rawString, "test tester")
        assertEquals(answer.firstName, "test")
        assertEquals(answer.lastName, "tester")
        assertEquals(answer.orcid,"fafadadf")
        await db.close();
        await client.end();
        Batcher.kill()
        authorCache.clear()
    },
    sanitizeResources: false,
    sanitizeOps: false,
})

Deno.test({
    name: "getAuthorNoId",
    async fn(): Promise<void> {
        await setup(true);
        let user = await insertUser("test@test", "ash", true, "Test", "Tester", "active");

        let app = await createMockApp();
        let author = await Author.create({rawString: "test tester", firstName: "test", lastName: "tester", orcid: "fafadadf"})

        let token = await createJWT(user)
        let ctx = await createMockContext(app, `{}`, [["Content-Type", "application/json"]], "/", token);
        await getAuthor(ctx, undefined)
        assertEquals(ctx.response.status, 422)
        Batcher.kill()
        await db.close();
        await client.end();
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
        let author = await Author.create({rawString: "test tester", firstName: "test", lastName: "tester", orcid: "fafadadf"})

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
        let author = await Author.create({rawString: "test tester", firstName: "test", lastName: "tester", orcid: "fafadadf"})
        let source: IApiAuthor =  {rawString:["test tester", "testing tester"]} as IApiAuthor
        await authorCache.add(String(author.id),source)

        let token = await createJWT(user)
        let ctx = await createMockContext(app, ``, [["Content-Type", "application/json"]], "/", token);
        getSourceAuthor(ctx, Number(author.id))
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
    name: "getAuthorSourceNoId",
    async fn(): Promise<void> {
        await setup(true);
        await authorCache.fileCache!.purge()
        let user = await insertUser("test@test", "ash", true, "Test", "Tester", "active");

        let app = await createMockApp();
        let author = await Author.create({rawString: "test tester", firstName: "test", lastName: "tester", orcid: "fafadadf"})
        let source: IApiAuthor =  {rawString:["test tester", "testing tester"]} as IApiAuthor
        await authorCache.add(String(author.id),source)

        let token = await createJWT(user)
        let ctx = await createMockContext(app, ``, [["Content-Type", "application/json"]], "/", token);
        getSourceAuthor(ctx, undefined)
        assertEquals(ctx.response.status, 422)


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
        let author = await Author.create({rawString: "test tester", firstName: "test", lastName: "tester", orcid: "fafadadf"})
        let source: IApiAuthor =  {rawString:["test tester", "testing tester"]} as IApiAuthor
        await authorCache.add(String(author.id),source)

        let token = await createJWT(user)
        let ctx = await createMockContext(app, ``, [["Content-Type", "application/json"]], "/", token);
        getSourceAuthor(ctx, Number(author.id)+1)
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
        let author = await Author.create({rawString: "test tester", firstName: "test", lastName: "tester", orcid: "fafadadf"})

        let token = await createJWT(user)
        let ctx = await createMockContext(app, `{"rawString":"Test Tester"}`, [["Content-Type", "application/json"]], "/", token);
        await patchAuthor(ctx, Number(author.id))
        assertEquals(ctx.response.status, 200)
        author = await Author.find(Number(author.id))
        assertEquals(String(author.rawString),"Test Tester")


        await db.close();
        await client.end();
        Batcher.kill()
    },
    sanitizeResources: false,
    sanitizeOps: false,
})

Deno.test({
    name: "PatchAuthorNoId",
    async fn(): Promise<void> {
        await setup(true);
        let user = await insertUser("test@test", "ash", true, "Test", "Tester", "active");

        let app = await createMockApp();
        let author = await Author.create({rawString: "test tester", firstName: "test", lastName: "tester", orcid: "fafadadf"})

        let token = await createJWT(user)
        let ctx = await createMockContext(app, `{"rawString":"Test Tester"}`, [["Content-Type", "application/json"]], "/", token);
        await patchAuthor(ctx, undefined)
        assertEquals(ctx.response.status, 422)

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
        let author = await Author.create({rawString: "test tester", firstName: "test", lastName: "tester", orcid: "fafadadf"})

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
        await authorCache.fileCache!.purge()
        let app = await createMockApp();
        let author = await Author.create({rawString: "test tester", firstName: "test", lastName: "tester", orcid: "fafadadf"})
        authorCache.add(String(author.id),{rawString: ["test tester", "testing tester"]} as IApiAuthor)
        let token = await createJWT(user)
        let ctx = await createMockContext(app, `{"rawString":"Test Tester"}`, [["Content-Type", "application/json"]], "/", token);
        await getAuthor(ctx, Number(author.id))
        let answer = JSON.parse(ctx.response.body as string)
        assertEquals(answer.status, "unfinished")
        await patchAuthor(ctx, Number(author.id))
        assertEquals(ctx.response.status, 200)
        author = await Author.find(Number(author.id))
        assertEquals(String(author.rawString),"Test Tester")
        let source = authorCache.get(String(author.id))
        assertEquals(source, undefined)
        await getAuthor(ctx, Number(author.id))
        answer = JSON.parse(ctx.response.body as string)
        assertEquals(answer.status, "finished")

        await db.close();
        await client.end();
        Batcher.kill()
    },
    sanitizeResources: false,
    sanitizeOps: false,
})
