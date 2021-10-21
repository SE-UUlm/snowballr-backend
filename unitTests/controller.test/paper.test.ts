import { assertEquals, assertNotEquals } from "https://deno.land/std/testing/asserts.ts";
import { IApiPaper } from "../../src/api/iApiPaper.ts";
import { db, client, saveChildren } from "../../src/controller/database.controller.ts";
import { insertUser } from "../../src/controller/databaseFetcher/user.ts";
import { Batcher } from "../../src/controller/fetch.controller.ts";
import { addAuthorToPaper, deleteAuthorOfPaper, getAuthors, getPaper, getPaperCitations, getPaperReferences, getPapers, getSourcePaper, patchPaper, postPaper, postPaperCitation, postPaperReference } from "../../src/controller/paper.controller.ts";
import { paperCache } from "../../src/controller/project.controller.ts";
import { createJWT } from "../../src/controller/validation.controller.ts";
import { setup } from "../../src/helper/setup.ts";
import { Author } from "../../src/model/db/author.ts";
import { Paper } from "../../src/model/db/paper.ts";
import { Wrote } from "../../src/model/db/wrote.ts";
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
    name: "postPaper",
    async fn(): Promise<void> {
        await setup(true);
        let user = await insertUser("test@test", "ash", true, "Test", "Tester", "active");
        let app = await createMockApp();
        let token = await createJWT(user)
        let ctx = await createMockContext(app, `{
            "doi": "1234",
            "title": "a title",
            "abstract": "an abstract",
            "year": 2009,
            "publisher": "uni ulm",
            "type": "proceeding",
            "scope": "a scope",
            "scopeName": "a scopeName"
        }`, [["Content-Type", "application/json"]], "/", token);
        await postPaper(ctx)
        let answer = JSON.parse(ctx.response.body as string)
        assertEquals(ctx.response.status, 200)
        let paper = await Paper.find(answer.id)
        assertEquals(String(paper.doi), "1234")
        assertEquals(String(paper.title), "a title")
        assertEquals(String(paper.abstract), "an abstract")
        assertEquals(Number(paper.year), 2009)
        assertEquals(String(paper.publisher), "uni ulm")
        assertEquals(String(paper.type), "proceeding")
        assertEquals(String(paper.scope), "a scope")
        assertEquals(String(paper.scopeName), "a scopeName")
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
        let paper = await Paper.create({ title: "Hello there", abstract: "General Kenobi" })

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
    name: "getPaperWrongId",
    async fn(): Promise<void> {
        await setup(true);
        let user = await insertUser("test@test", "ash", true, "Test", "Tester", "active");
        let app = await createMockApp();
        let paper = await Paper.create({ title: "Hello there", abstract: "General Kenobi" })

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
        let paper = await Paper.create({ title: "Hello there", abstract: "General Kenobi" })
        let paper1 = await Paper.create({ title: "Hi", abstract: "this abstract" })
        let paper2 = await Paper.create({ title: "mlem", abstract: "mlem mlem" })
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
    name: "getPaperCitations",
    async fn(): Promise<void> {
        await setup(true);
        let user = await insertUser("test@test", "ash", true, "Test", "Tester", "active");

        let app = await createMockApp();
        let paper = await Paper.create({ title: "Hello there", abstract: "General Kenobi" })
        let paper1 = await Paper.create({ title: "Hi", abstract: "this abstract" })
        let paper2 = await Paper.create({ title: "mlem", abstract: "mlem mlem" })
        await saveChildren("citedby", "papercitingid", "papercitedid", Number(paper.id), Number(paper1.id))
        await saveChildren("citedby", "papercitingid", "papercitedid", Number(paper.id), Number(paper2.id))
        let token = await createJWT(user)
        let ctx = await createMockContext(app, ``, [["Content-Type", "application/json"]], "/", token);
        await getPaperCitations(ctx, Number(paper.id))
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
    name: "postPaperCitations",
    async fn(): Promise<void> {
        await setup(true);
        let user = await insertUser("test@test", "ash", true, "Test", "Tester", "active");

        let app = await createMockApp();
        let token = await createJWT(user)
        let ctx = await createMockContext(app, ``, [["Content-Type", "application/json"]], "/", token);
        let paper = await Paper.create({ title: "Hello there", abstract: "General Kenobi" })
        let paper1 = await Paper.create({ title: "Hi", abstract: "this abstract" })
        let paper2 = await Paper.create({ title: "mlem", abstract: "mlem mlem" })
        await getPaperCitations(ctx, Number(paper.id))
        let answer = JSON.parse(ctx.response.body as string)
        let length = answer.papers.length
        ctx = await createMockContext(app, `{"id": ${paper1.id}}`, [["Content-Type", "application/json"]], "/", token);
        await postPaperCitation(ctx, Number(paper.id))

        await getPaperCitations(ctx, Number(paper.id))
        answer = JSON.parse(ctx.response.body as string)
        assertEquals(ctx.response.status, 200)
        assertEquals(answer.papers.length, length + 1)
        assertEquals(answer.papers[0].title, "Hi")
        assertEquals(answer.papers[0].abstract, "this abstract")
        assertEquals(answer.papers[0].id, Number(paper1.id))
        await db.close();
        await client.end();
        Batcher.kill()
    },
    sanitizeResources: false,
    sanitizeOps: false,
})

Deno.test({
    name: "postPaperReferences",
    async fn(): Promise<void> {
        await setup(true);
        let user = await insertUser("test@test", "ash", true, "Test", "Tester", "active");

        let app = await createMockApp();
        let token = await createJWT(user)
        let ctx = await createMockContext(app, ``, [["Content-Type", "application/json"]], "/", token);
        let paper = await Paper.create({ title: "Hello there", abstract: "General Kenobi" })
        let paper1 = await Paper.create({ title: "Hi", abstract: "this abstract" })
        let paper2 = await Paper.create({ title: "mlem", abstract: "mlem mlem" })
        await getPaperReferences(ctx, Number(paper.id))
        let answer = JSON.parse(ctx.response.body as string)
        let length = answer.papers.length
        ctx = await createMockContext(app, `{"id": ${paper1.id}}`, [["Content-Type", "application/json"]], "/", token);
        await postPaperReference(ctx, Number(paper.id))

        await getPaperReferences(ctx, Number(paper.id))
        answer = JSON.parse(ctx.response.body as string)
        assertEquals(ctx.response.status, 200)
        assertEquals(answer.papers.length, length + 1)
        assertEquals(answer.papers[0].title, "Hi")
        assertEquals(answer.papers[0].abstract, "this abstract")
        assertEquals(answer.papers[0].id, Number(paper1.id))
        await db.close();
        await client.end();
        Batcher.kill()
    },
    sanitizeResources: false,
    sanitizeOps: false,
})

Deno.test({
    name: "getPaperSource",
    async fn(): Promise<void> {
        await setup(true);
        await paperCache.fileCache!.purge()
        let user = await insertUser("test@test", "ash", true, "Test", "Tester", "active");

        let app = await createMockApp();
        let paper = await Paper.create({ title: "Hello there", abstract: "General Kenobi" })
        let source: IApiPaper = { title: ["Hello there", "Hi There"] } as IApiPaper
        await paperCache.add(String(paper.id), source)

        let token = await createJWT(user)
        let ctx = await createMockContext(app, ``, [["Content-Type", "application/json"]], "/", token);
        await getSourcePaper(ctx, Number(paper.id))
        let answer = JSON.parse(ctx.response.body as string)
        assertEquals(ctx.response.status, 200)
        assertEquals(answer.title, ["Hello there", "Hi There"])

        await db.close();
        await client.end();
        Batcher.kill()
        paperCache.clear()
    },
    sanitizeResources: false,
    sanitizeOps: false,
})

Deno.test({
    name: "getPaperSourceWrongId",
    async fn(): Promise<void> {
        await setup(true);
        await paperCache.fileCache!.purge()
        let user = await insertUser("test@test", "ash", true, "Test", "Tester", "active");

        let app = await createMockApp();
        let paper = await Paper.create({ title: "Hello there", abstract: "General Kenobi" })
        let source: IApiPaper = { title: ["Hello there, Hi There"] } as IApiPaper
        paperCache.add(String(paper.id), source)

        let token = await createJWT(user)
        let ctx = await createMockContext(app, ``, [["Content-Type", "application/json"]], "/", token);
        await getSourcePaper(ctx, Number(paper.id) + 1)
        assertEquals(ctx.response.status, 404)


        await db.close();
        await client.end();
        Batcher.kill()
        paperCache.clear()
    },
    sanitizeResources: false,
    sanitizeOps: false,
})

Deno.test({
    name: "PatchPaper",
    async fn(): Promise<void> {
        await setup(true);
        let user = await insertUser("test@test", "ash", true, "Test", "Tester", "active");

        let app = await createMockApp();
        let paper = await Paper.create({ title: "Hello there", abstract: "General Kenobi" })

        let token = await createJWT(user)
        let ctx = await createMockContext(app, `{"title":"Hello There"}`, [["Content-Type", "application/json"]], "/", token);
        await patchPaper(ctx, Number(paper.id))
        assertEquals(ctx.response.status, 200)
        paper = await Paper.find(Number(paper.id))
        assertEquals(String(paper.title), "Hello There")


        await db.close();
        await client.end();
        Batcher.kill()
    },
    sanitizeResources: false,
    sanitizeOps: false,
})

Deno.test({
    name: "PatchPaperWrongId",
    async fn(): Promise<void> {
        await setup(true);
        let user = await insertUser("test@test", "ash", true, "Test", "Tester", "active");

        let app = await createMockApp();
        let paper = await Paper.create({ title: "Hello there", abstract: "General Kenobi" })

        let token = await createJWT(user)
        let ctx = await createMockContext(app, `{"title":"Hello There}`, [["Content-Type", "application/json"]], "/", token);
        await patchPaper(ctx, Number(paper.id) + 1)
        assertEquals(ctx.response.status, 404)
        paper = await Paper.find(Number(paper.id))
        assertEquals(String(paper.title), "Hello there")


        await db.close();
        await client.end();
        Batcher.kill()
    },
    sanitizeResources: false,
    sanitizeOps: false,
})

Deno.test({
    name: "PatchPaperPaperCache",
    async fn(): Promise<void> {
        await setup(true);
        let user = await insertUser("test@test", "ash", true, "Test", "Tester", "active");
        let app = await createMockApp();
        let paper = await Paper.create({ title: "Hello there", abstract: "General Kenobi" })
        await paperCache.add(String(paper.id), { title: ["Hello there", "Hi There"], titleSource: ["BA", "MA"] } as IApiPaper)
        let token = await createJWT(user)
        let ctx = await createMockContext(app, `{"title":"Hello There"}`, [["Content-Type", "application/json"]], "/", token);
        await getPaper(ctx, Number(paper.id))
        let answer = JSON.parse(ctx.response.body as string)
        assertEquals(answer.status, "unfinished")
        await patchPaper(ctx, Number(paper.id))
        assertEquals(ctx.response.status, 200)
        paper = await Paper.find(Number(paper.id))
        assertEquals(String(paper.title), "Hello There")
        let source = paperCache.get(String(paper.id))
        assertEquals(source, undefined)
        await getPaper(ctx, Number(paper.id))
        answer = JSON.parse(ctx.response.body as string)
        assertEquals(answer.status, "ready")

        await db.close();
        await client.end();
        Batcher.kill()
    },
    sanitizeResources: false,
    sanitizeOps: false,
})

Deno.test({
    name: "getAuthorsOfPaper",
    async fn(): Promise<void> {
        await setup(true);
        let user = await insertUser("test@test", "ash", true, "Test", "Tester", "active");

        let app = await createMockApp();
        let paper = await Paper.create({ title: "Hello there", abstract: "General Kenobi" })
        let author1 = await Author.create({ firstName: "Hi" })
        let author2 = await Author.create({ lastName: "Hellu" })
        await Wrote.create({ authorId: Number(author1.id), paperId: Number(paper.id) })
        await Wrote.create({ authorId: Number(author2.id), paperId: Number(paper.id) })
        let token = await createJWT(user)
        let ctx = await createMockContext(app, ``, [["Content-Type", "application/json"]], "/", token);
        await getAuthors(ctx, Number(paper.id))
        let answer = JSON.parse(ctx.response.body as string)
        assertEquals(ctx.response.status, 200)
        assertEquals(answer.authors.length, 2)
        assertEquals(answer.authors[0].firstName, "Hi")
        assertEquals(answer.authors[1].lastName, "Hellu")
        await db.close();
        await client.end();
        Batcher.kill()
    },
    sanitizeResources: false,
    sanitizeOps: false,
})

Deno.test({
    name: "addAuthorToPaper",
    async fn(): Promise<void> {
        await setup(true);
        let user = await insertUser("test@test", "ash", true, "Test", "Tester", "active");

        let app = await createMockApp();
        let paper = await Paper.create({ title: "Hello there", abstract: "General Kenobi" })
        let author1 = await Author.create({ firstName: "Hi" })
        let token = await createJWT(user)
        let ctx = await createMockContext(app, `{"id": ${Number(author1.id)}}`, [["Content-Type", "application/json"]], "/", token);
        await addAuthorToPaper(ctx, Number(paper.id))
        assertEquals(ctx.response.status, 200)
        let wrote = await Wrote.where({ paperId: Number(paper.id), authorId: Number(author1.id) }).get()
        assertNotEquals(wrote, undefined)
        await db.close();
        await client.end();
        Batcher.kill()
    },
    sanitizeResources: false,
    sanitizeOps: false,
})

Deno.test({
    name: "addAuthorToPaperWrongId",
    async fn(): Promise<void> {
        await setup(true);
        let user = await insertUser("test@test", "ash", true, "Test", "Tester", "active");

        let app = await createMockApp();
        let paper = await Paper.create({ title: "Hello there", abstract: "General Kenobi" })
        let author1 = await Author.create({ firstName: "Hi" })
        let token = await createJWT(user)
        let ctx = await createMockContext(app, `{"id": ${Number(author1.id) + 1}}`, [["Content-Type", "application/json"]], "/", token);
        await addAuthorToPaper(ctx, Number(paper.id))
        assertEquals(ctx.response.status, 404)
        let wrote = await Wrote.where({ paperId: Number(paper.id), authorId: Number(author1.id) }).get()
        if (Array.isArray(wrote)) {
            assertEquals(wrote[0], undefined)
        } else {
            assertEquals(wrote, "database now doesn't always return array")
        }

        await db.close();
        await client.end();
        Batcher.kill()
    },
    sanitizeResources: false,
    sanitizeOps: false,
})


Deno.test({
    name: "deleteAuthorOfPaper",
    async fn(): Promise<void> {
        await setup(true);
        let user = await insertUser("test@test", "ash", true, "Test", "Tester", "active");

        let app = await createMockApp();
        let paper = await Paper.create({ title: "Hello there", abstract: "General Kenobi" })
        let author1 = await Author.create({ firstName: "Hi" })
        let wrote = await Wrote.create({ authorId: Number(author1.id), paperId: Number(paper.id) })
        let token = await createJWT(user)
        let ctx = await createMockContext(app, ``, [["Content-Type", "application/json"]], "/", token);
        await deleteAuthorOfPaper(ctx, Number(paper.id), Number(author1.id))
        assertEquals(ctx.response.status, 200)
        wrote = await Wrote.find(Number(wrote.id))
        assertEquals(wrote, undefined)
        await db.close();
        await client.end();
        Batcher.kill()
    },
    sanitizeResources: false,
    sanitizeOps: false,
})
