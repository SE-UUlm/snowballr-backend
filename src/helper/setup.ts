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

export const setup = async (dropDatabase: boolean) => {

    Relationships.belongsTo(Invitation, User);
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
    db.link([User, Invitation, Paper, Token, ReferencedBy, CitedBy, Author, Wrote, Project, Stage, PaperScopeForStage, SearchApi, ReadingList, Criteria, Review, CriteriaEvaluation, UserIsPartOfProject, ProjectUsesApi]);
    await db.sync({drop: dropDatabase}).catch(err => {
        //TODO fix for https://github.com/eveningkid/denodb/issues/258
        console.log(err)
        console.log("Entering workaround for: https://github.com/eveningkid/denodb/issues/258")
    });
    let admin = await returnUserByEmail(String(Deno.env.get("ADMIN_EMAIL")));
    if (!admin) {
        await insertUser(String(Deno.env.get("ADMIN_EMAIL")), String(Deno.env.get("ADMIN_PASSWORD")), true, "admin", "admin", "fully registered");
    }
}