import {insertUser, returnUserByEmail} from "../controller/databaseFetcher/user.ts";
import {User} from "../model/user.ts";
import {Invitation} from "../model/invitation.ts";
import {Relationships} from 'https://deno.land/x/denodb/mod.ts';
import {db} from "../controller/database.ts";

export const setup = async (dropDatabase: boolean) => {

    Relationships.belongsTo(Invitation, User);
    db.link([User, Invitation]);
    await db.sync({drop: dropDatabase}).catch(err => {
        //TODO fix for https://github.com/eveningkid/denodb/issues/258
        console.log("Entering workaround for: https://github.com/eveningkid/denodb/issues/258")
    });
    let admin = await returnUserByEmail(String(Deno.env.get("ADMIN_EMAIL")));
    if (!admin) {
        await insertUser(String(Deno.env.get("ADMIN_EMAIL")), String(Deno.env.get("ADMIN_PASSWORD")), true, "admin", "admin", "fully registered");
    }
}