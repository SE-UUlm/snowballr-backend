import { assertEquals } from "https://deno.land/std@0.150.0/testing/asserts.ts";
import { IApiAuthor } from "../../src/api/iApiAuthor.ts";
import { IApiPaper } from "../../src/api/iApiPaper.ts";
import { idType } from "../../src/api/iApiUniqueId.ts";
import { IApiUniqueId } from "../../src/api/iApiUniqueId.ts";
import { client, db } from "../../src/controller/database.controller.ts";
import { getAllAuthorsFromPaper } from "../../src/controller/databaseFetcher/author.ts";
import { Batcher } from "../../src/controller/fetch.controller.ts";
import { authorCache, paperCache } from "../../src/controller/project.controller.ts";
import { convertIApiPaperToDBPaper } from "../../src/helper/converter/paperConverter.ts";
import { setup } from "../../src/helper/setup.ts";
import { Author } from "../../src/model/db/author.ts";

Deno.test({
    name: "testIapiPaperToDBPaper",
    async fn() {
        try {
            await setup(true)
            let title = ["a very good title"];
            let author: IApiAuthor[] = [{ rawString: ["This Author"], orcid: [], firstName: [], lastName: [] }, { rawString: ["Other Author"], orcid: [], firstName: [], lastName: [] }]
            let abstract: string[] = ["a good abstract"]
            let year: number[] = [2009];
            let publisher: string[] = ["IEEE"];
            let type: string[] = ["proceeding"]
            let scope: string[] = ["a scope"]
            let scopeName: string[] = ["scopje"]
            let pdf: string[] = ["www.randomurl.com"]
            let uniqueId: IApiUniqueId[] = [{ type: idType.DOI, value: "awr23r23tr2r//" }]

            const iApiPaper: IApiPaper = {
                title: title,
                author: author,
                abstract: abstract,
                year: year,
                publisher: publisher,
                type: type,
                scope: scope,
                scopeName: scopeName,
                pdf: pdf,
                uniqueId: uniqueId,
                numberOfCitations: [],
                numberOfReferences: [],
                source: [],
                raw: []
            }
            const dbPaper = await convertIApiPaperToDBPaper(iApiPaper)
            const authors = await getAllAuthorsFromPaper(Number(dbPaper.id))

            author = [{ rawString: ["This Author"], orcid: [], firstName: [], lastName: [] }, { rawString: ["Other Author"], orcid: [], firstName: [], lastName: [] }]
            uniqueId = [{ type: idType.DOI, value: "awr23r23tr2r//" }]
            title = ["a very good title"];
            abstract = ["a good abstract"]
            year = [2009];
            publisher = ["IEEE"];
            type = ["proceeding"]
            scope = ["a scope"]
            scopeName = ["scopje"]
            pdf = ["www.randomurl.com"]

            assertEquals(String(dbPaper.title), title[0]);
            assertEquals(String(dbPaper.abstract), abstract[0]);
            assertEquals(Number(dbPaper.year), year[0]);
            assertEquals(String(dbPaper.publisher), publisher[0]);
            assertEquals(String(dbPaper.type), type[0]);
            assertEquals(String(dbPaper.scope), scope[0]);
            assertEquals(String(dbPaper.scopeName), scopeName[0]);
            assertEquals(String(dbPaper.doi), uniqueId[0].value);
            assertEquals(String(authors[0].rawString), author[0].rawString[0]);
            assertEquals(String(authors[1].rawString), author[1].rawString[0]);
            paperCache.clear()
            authorCache.clear()
            await db.close()

            assertEquals(true, true)
        } catch (error) {
            console.log(error)
        }
    },

    sanitizeResources: false,
    sanitizeOps: false,

})

Deno.test({
    name: "testIapiPaperToDBPaperAuthoralreadyExists",
    async fn() {
        try {
            await setup(true)
            const oldAuthor = await Author.create({ orcid: "1234", rawString: "this author" })
            const size = (await Author.all()).length
            const title = ["a very good title"];
            let author: IApiAuthor[] = [{ rawString: ["This Author"], orcid: ["1234"], firstName: ["This"], lastName: ["Author"] }]
            const abstract: string[] = ["a good abstract"]
            const year: number[] = [2009];
            const publisher: string[] = ["IEEE"];
            const type: string[] = ["proceeding"]
            const scope: string[] = ["a scope"]
            const scopeName: string[] = ["scopje"]
            const pdf: string[] = ["www.randomurl.com"]
            const uniqueId: IApiUniqueId[] = [{ type: idType.DOI, value: "awr23r23tr2r//" }]

            const iApiPaper: IApiPaper = {
                title: title,
                author: author,
                abstract: abstract,
                year: year,
                publisher: publisher,
                type: type,
                scope: scope,
                scopeName: scopeName,
                pdf: pdf,
                uniqueId: uniqueId,
                numberOfCitations: [],
                numberOfReferences: [],
                source: [],
                raw: []
            }
            /*const dbPaper = */await convertIApiPaperToDBPaper(iApiPaper)
            const updatedAuthor = await Author.find(Number(oldAuthor.id))
            const newSize = (await Author.all()).length
            author = [{ rawString: ["This Author"], orcid: ["1234"], firstName: ["This"], lastName: ["Author"] }]
            assertEquals(String(updatedAuthor.rawString), "this author")
            assertEquals(String(updatedAuthor.firstName), author[0].firstName[0])
            assertEquals(String(updatedAuthor.lastName), author[0].lastName[0])
            assertEquals(newSize, size);

            Batcher.kill()
            paperCache.clear()
            authorCache.clear()
            await db.close()
            await client.end();
        } catch (e) {
            console.error("in catch")
            console.error(e)
        }
    },
    sanitizeResources: false,
    sanitizeOps: false,
})


