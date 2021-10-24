import { insertUser, returnUserByEmail } from "../controller/databaseFetcher/user.ts";
import { User } from "../model/db/user.ts";
import { Invitation } from "../model/db/invitation.ts";
import { Relationships } from 'https://deno.land/x/denodb/mod.ts';
import { client, db } from "../controller/database.controller.ts";
import { Token } from "../model/db/token.ts";
import { Project } from "../model/db/project.ts";
import { UserIsPartOfProject } from "../model/db/userIsPartOfProject.ts";
import { ProjectUsesApi } from "../model/db/projectUsesApi.ts";
import { SearchApi } from "../model/db/searchApi.ts";
import { Criteria } from "../model/db/criteria.ts";
import { CriteriaEvaluation } from "../model/db/criteriaEval.ts";
import { Review } from "../model/db/review.ts";
import { Stage } from "../model/db/stage.ts";
import { ReadingList } from "../model/db/readingList.ts";
import { PaperScopeForStage } from "../model/db/paperScopeForStage.ts";
import { Paper } from "../model/db/paper.ts";
import { Wrote } from "../model/db/wrote.ts";
import { Author } from "../model/db/author.ts";
import { PaperHasID } from "../model/db/paperHasID.ts";
import { PaperID } from "../model/db/paperID.ts";
import { AuthorHasID } from "../model/db/authorHasID.ts";
import { AuthorID } from "../model/db/authorID.ts";
import { ResetToken } from "../model/db/resetToken.ts";
import { Pdf } from "../model/db/pdf.ts";
import { authorCache, paperCache } from "../controller/project.controller.ts";
import { Batcher } from "../controller/fetch.controller.ts";

/**
 * Links all model files to the database and inserts the first admin, if he doesn't exist yet
 * @param dropDatabase dropsDatabase during the setup
 */
export const setup = async (dropDatabase: boolean) => {
    await client.connect();
    if (dropDatabase) {
        await client.queryArray("DROP TABLE IF EXISTS citedby")
        await client.queryArray("DROP TABLE IF EXISTS referencedby")
        paperCache.fileCache!.purge()
        authorCache.fileCache!.purge()
        Batcher.purge()
    }
    Relationships.belongsTo(Invitation, User);
    Relationships.belongsTo(ResetToken, User);
    Relationships.belongsTo(Token, User);
    Relationships.belongsTo(UserIsPartOfProject, User)
    Relationships.belongsTo(UserIsPartOfProject, Project)
    Relationships.belongsTo(ProjectUsesApi, SearchApi)
    Relationships.belongsTo(ProjectUsesApi, Project)
    Relationships.belongsTo(Criteria, Project)
    Relationships.belongsTo(CriteriaEvaluation, Criteria)
    Relationships.belongsTo(CriteriaEvaluation, Review)
    Relationships.belongsTo(Review, User)
    Relationships.belongsTo(Stage, Project)
    Relationships.belongsTo(Review, Stage)
    Relationships.belongsTo(Review, PaperScopeForStage)
    Relationships.belongsTo(ReadingList, Paper)
    Relationships.belongsTo(ReadingList, User)
    Relationships.belongsTo(PaperScopeForStage, Stage)
    Relationships.belongsTo(PaperScopeForStage, Paper)
    Relationships.belongsTo(Wrote, Paper)
    Relationships.belongsTo(Wrote, Author)
    Relationships.belongsTo(Pdf, Paper)
    Relationships.belongsTo(PaperHasID, Paper)
    Relationships.belongsTo(PaperHasID, PaperID)
    Relationships.belongsTo(AuthorHasID, Author)
    Relationships.belongsTo(AuthorHasID, AuthorID)
    db.link([User, Invitation, ResetToken, Paper, Pdf, Token, Author, AuthorID, Wrote, Project, Stage, PaperScopeForStage, SearchApi, ReadingList, Criteria, Review, PaperID, CriteriaEvaluation, UserIsPartOfProject, ProjectUsesApi, PaperHasID, AuthorHasID]);
    await db.sync({ drop: dropDatabase }).catch(err => {
        //TODO fix for https://github.com/eveningkid/denodb/issues/258
        console.log(err)
        console.log("Entering workaround for: https://github.com/eveningkid/denodb/issues/258")
    });
    await client.queryArray(`CREATE TABLE IF NOT EXISTS citedby(
                                                        id SERIAL,
                                                        papercitedid int NOT NULL,
                                                        papercitingid int NOT NULL,
                                                        FOREIGN KEY (papercitedid) REFERENCES paper (id),
                                                        FOREIGN KEY (papercitingid) REFERENCES paper (id),
                                                        PRIMARY KEY (id))`);
    await client.queryArray(`CREATE TABLE IF NOT EXISTS referencedby(
                                                        id SERIAL,
                                                        paperreferencedid int NOT NULL,
                                                        paperreferencingid int NOT NULL,
                                                        FOREIGN KEY (paperreferencedid) REFERENCES paper (id),
                                                        FOREIGN KEY (paperreferencingid) REFERENCES paper (id),
                                                        PRIMARY KEY (id))`);
    let admin = await returnUserByEmail(String(Deno.env.get("ADMIN_EMAIL")));
    if (!admin) {
        admin = await insertUser(String(Deno.env.get("ADMIN_EMAIL")), String(Deno.env.get("ADMIN_PASSWORD")), true, "admin", "admin", "active");

        await SearchApi.create({ name: "crossRef", id: IDOfApi.crossRef, credentials: "luca999@web.de" })
        await SearchApi.create({ name: "openCitations", id: IDOfApi.openCitations })
        await SearchApi.create({ name: "semanticScholar", id: IDOfApi.semanticScholar })
        await SearchApi.create({ name: "IEEE", id: IDOfApi.IEEE, credentials: "4yk5d9an52ejynjsmzqxe62r" })
        await SearchApi.create({ name: "googleScholar", id: IDOfApi.googleScholar })
        await SearchApi.create({ name: "microsoftAcademic", id: IDOfApi.microsoftAcademic, credentials: "9a02225751354cd29397eba3f5382101" })


        //TODO: only to showcase functionality, otherwise delete
        await insertUser("ad@test", "1234", false, "admin2", "admin2", "active");
        let project = await Project.create({
            name: "Test", minCountReviewers: 0, countDecisiveReviewers: 5, combinationOfReviewers: 0,
            type: "",
            evaluationFormula: "",
            mergeThreshold: 0.8
        })
        await ProjectUsesApi.create({ projectId: Number(project.id), searchapiId: IDOfApi.crossRef, inUse: true })
        await ProjectUsesApi.create({ projectId: Number(project.id), searchapiId: IDOfApi.openCitations, inUse: true })
        await ProjectUsesApi.create({ projectId: Number(project.id), searchapiId: IDOfApi.googleScholar, inUse: true })
        await ProjectUsesApi.create({ projectId: Number(project.id), searchapiId: IDOfApi.IEEE, inUse: true })
        await ProjectUsesApi.create({ projectId: Number(project.id), searchapiId: IDOfApi.semanticScholar, inUse: true })
        await ProjectUsesApi.create({ projectId: Number(project.id), searchapiId: IDOfApi.microsoftAcademic, inUse: true })

        await UserIsPartOfProject.create({
            isOwner: true,
            userId: Number(admin.id),
            projectId: Number(project.id)
        })
        let stage = await Stage.create({ projectId: Number(project.id), name: "awesome Stage", number: 0 })
        await Stage.create({ projectId: Number(project.id), name: "the next Stage", number: 1 })
        let paper01 = await Paper.create({ title: "paper01" })
        let paper02 = await Paper.create({ title: "paper02" })
        let paper03 = await Paper.create({ title: "paper03" })
        let author = await Author.create({ rawString: "Author01" })
        let author2 = await Author.create({ rawString: "Author02" })
        await Wrote.create({ paperId: Number(paper01.id), authorId: Number(author.id) })
        await Wrote.create({ paperId: Number(paper02.id), authorId: Number(author2.id) })
        await Paper.create({ title: "paper04" })
        let paper05 = await Paper.create({ title: "paper05" })
        let pp01 = await PaperScopeForStage.create({ paperId: Number(paper01.id), stageId: Number(stage.id), finalDecision: "YES" })
        let pp02 = await PaperScopeForStage.create({ paperId: Number(paper02.id), stageId: Number(stage.id), finalDecision: "NO" })
        await PaperScopeForStage.create({ paperId: Number(paper05.id), stageId: Number(stage.id) })
        await Review.create({ finished: true, overallEvaluation: "YES", userId: Number(admin.id), paperId: Number(pp01.id), stageId: Number(stage.id) })
        await Review.create({ finished: true, overallEvaluation: "YES", userId: Number(admin.id), paperId: Number(pp01.id), stageId: Number(stage.id) })
        await Review.create({ finished: true, overallEvaluation: "YES", userId: Number(admin.id), paperId: Number(pp01.id), stageId: Number(stage.id) })
        await Review.create({ finished: true, overallEvaluation: "MAYBE", userId: Number(admin.id), paperId: Number(pp02.id), stageId: Number(stage.id) })
        await Review.create({ finished: true, overallEvaluation: "MAYBE", userId: Number(admin.id), paperId: Number(pp02.id), stageId: Number(stage.id) })
        await Review.create({ finished: true, overallEvaluation: "MAYBE", userId: Number(admin.id), paperId: Number(pp02.id), stageId: Number(stage.id) })
        await Review.create({ finished: true, overallEvaluation: "MAYBE", userId: Number(admin.id), paperId: Number(pp02.id), stageId: Number(stage.id) })
        await Review.create({ finished: true, overallEvaluation: "NO", userId: Number(admin.id), paperId: Number(pp02.id), stageId: Number(stage.id) })
        await client.queryArray(`INSERT INTO citedby (papercitedid, papercitingid)
                VALUES (${Number(paper01.id)}, ${Number(paper02.id)}),
                        (${Number(paper01.id)}, ${Number(paper03.id)})`)
    }


}

export enum IDOfApi {
    crossRef = 1,
    openCitations = 2,
    googleScholar = 3,
    IEEE = 4,
    semanticScholar = 5,
    microsoftAcademic = 6,
}