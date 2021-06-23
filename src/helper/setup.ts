import {insertUser, returnUserByEmail} from "../controller/databaseFetcher/user.ts";
import {User} from "../model/db/user.ts";
import {Invitation} from "../model/db/invitation.ts";
import {Relationships} from 'https://deno.land/x/denodb/mod.ts';
import {db} from "../controller/database.ts";
import {Token} from "../model/db/token.ts";
import {Project} from "../model/db/project.ts";
import {UserIsPartOfProject} from "../model/db/userIsPartOfProject.ts";
import {ProjectUsesApi} from "../model/db/projectUsesApi.ts";
import {SearchApi} from "../model/db/searchApi.ts";
import {Criteria} from "../model/db/criteria.ts";
import {CriteriaEvaluation} from "../model/db/criteriaEval.ts";
import {Review} from "../model/db/review.ts";
import {Stage} from "../model/db/stage.ts";
import {ReadingList} from "../model/db/readingList.ts";
import {PaperScopeForStage} from "../model/db/paperScopeForStage.ts";
import {Paper} from "../model/db/paper.ts";
import {Wrote} from "../model/db/wrote.ts";
import {Author} from "../model/db/author.ts";
import {CitedBy} from "../model/db/citedBy.ts";
import {ReferencedBy} from "../model/db/referencedBy.ts";
import {PaperHasID} from "../model/db/paperHasID.ts";
import {PaperID} from "../model/db/paperID.ts";
import {AuthorHasID} from "../model/db/authorHasID.ts";
import {AuthorID} from "../model/db/authorID.ts";
import {ResetToken} from "../model/db/resetToken.ts";

/**
 * Links all model files to the database and inserts the first admin, if he doesn't exist yet
 * @param dropDatabase dropsDatabase during the setup
 */
export const setup = async (dropDatabase: boolean) => {

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
    Relationships.belongsTo(Review, Paper)
    Relationships.belongsTo(CitedBy, Paper)
    Relationships.belongsTo(ReferencedBy, Paper)
    Relationships.belongsTo(ReadingList, Paper)
    Relationships.belongsTo(ReadingList, User)
    Relationships.belongsTo(PaperScopeForStage, Stage)
    Relationships.belongsTo(PaperScopeForStage, Paper)
    Relationships.belongsTo(Wrote, Paper)
    Relationships.belongsTo(Wrote, Author)
    Relationships.belongsTo(PaperHasID, Paper)
    Relationships.belongsTo(PaperHasID, PaperID)
    Relationships.belongsTo(AuthorHasID, Author)
    Relationships.belongsTo(AuthorHasID, AuthorID)
    db.link([User, Invitation, ResetToken, Paper, Token, ReferencedBy, CitedBy, Author, AuthorID, Wrote, Project, Stage, PaperScopeForStage, SearchApi, ReadingList, Criteria, Review, PaperID, CriteriaEvaluation, UserIsPartOfProject, ProjectUsesApi, PaperHasID, AuthorHasID]);
    await db.sync({drop: dropDatabase}).catch(err => {
        //TODO fix for https://github.com/eveningkid/denodb/issues/258
        console.log(err)
        console.log("Entering workaround for: https://github.com/eveningkid/denodb/issues/258")
    });
    let admin = await returnUserByEmail(String(Deno.env.get("ADMIN_EMAIL")));
    if (!admin) {
        admin = await insertUser(String(Deno.env.get("ADMIN_EMAIL")), String(Deno.env.get("ADMIN_PASSWORD")), true, "admin", "admin", "active");
        //TODO: only to showcase functionality, otherwise delete
        let project = await Project.create({name: "Test"})
        let userProject = await UserIsPartOfProject.create({
            isOwner: true,
            userId: Number(admin.id),
            projectId: Number(project.id)
        })
        await Stage.create({projectId: Number(project.id), name: "awesome Stage", number: 0})
        await Stage.create({projectId: Number(project.id), name: "the next Stage", number: 1})
    }

}