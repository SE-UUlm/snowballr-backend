import {insertUser, returnUserByEmail} from "../controller/databaseFetcher/user.ts";
import {User} from "../model/db/user.ts";
import {Invitation} from "../model/db/invitation.ts";
import {Relationships} from 'https://deno.land/x/denodb/mod.ts';
import {db} from "../controller/database.ts";
import {Token} from "../model/db/token.ts";

export const setup = async (dropDatabase: boolean) => {

    Relationships.belongsTo(Invitation, User);
    Relationships.belongsTo(Token, User);
    db.link([User, Invitation, Token]);
    await db.sync({drop: dropDatabase}).catch(err => {
        //TODO fix for https://github.com/eveningkid/denodb/issues/258
        console.log("Entering workaround for: https://github.com/eveningkid/denodb/issues/258")
    });
    let admin = await returnUserByEmail(String(Deno.env.get("ADMIN_EMAIL")));
    if (!admin) {
        await insertUser(String(Deno.env.get("ADMIN_EMAIL")), String(Deno.env.get("ADMIN_PASSWORD")), true, "admin", "admin", "fully registered");
    }
}