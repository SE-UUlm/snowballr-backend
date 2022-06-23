import { insertUser, returnUserByEmail } from "../controller/databaseFetcher/user.ts";
import { User } from "../model/db/user.ts";
import { Invitation } from "../model/db/invitation.ts";
import { Relationships } from 'https://deno.land/x/denodb@v1.0.39/mod.ts';
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
		await client.queryArray("DROP SCHEMA public CASCADE")
		await client.queryArray("CREATE SCHEMA public")
		//await client.queryArray("GRANT ALL ON SCHEMA public TO postgres")
		await client.queryArray("GRANT ALL ON SCHEMA public TO public")
		await client.queryArray(`COMMENT ON SCHEMA public IS 'standard public schema'`)

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
	Relationships.belongsTo(PaperScopeForStage, Review)
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
	db.link([User, Invitation, ResetToken, Paper, Pdf, Token, Author, AuthorID, Wrote, Project, Stage, SearchApi, ReadingList, Criteria, Review, PaperScopeForStage, PaperID, CriteriaEvaluation, UserIsPartOfProject, ProjectUsesApi, PaperHasID, AuthorHasID]);
	//console.log("1111111111111111111111111111111")
	await db.sync({ drop: dropDatabase }).catch(err => {
		//TODO fix for https://github.com/eveningkid/denodb/issues/258
		console.log(err)
		console.log("Entering workaround for: https://github.com/eveningkid/denodb/issues/258")
	});
	//console.log("222222222222222222222222222222")
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
	//console.log("--------------------ADM;IN---------------------------")
	//console.log(admin);
	if (!admin) {
		admin = await insertUser(String(Deno.env.get("ADMIN_EMAIL")), String(Deno.env.get("ADMIN_PASSWORD")), true, "admin", "admin", "active");

		await SearchApi.create({ name: "crossRef", id: IDOfApi.crossRef, credentials: "luca999@web.de" })
		await SearchApi.create({ name: "openCitations", id: IDOfApi.openCitations })
		await SearchApi.create({ name: "semanticScholar", id: IDOfApi.semanticScholar })
		await SearchApi.create({ name: "IEEE", id: IDOfApi.IEEE, credentials: "4yk5d9an52ejynjsmzqxe62r" })
		await SearchApi.create({ name: "googleScholar", id: IDOfApi.googleScholar })
		await SearchApi.create({ name: "microsoftAcademic", id: IDOfApi.microsoftAcademic, credentials: "9a02225751354cd29397eba3f5382101" })


		//TODO: only to showcase functionality, otherwise delete
		//await insertUser("test@test", "1234", true, "test", "user", "active");
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