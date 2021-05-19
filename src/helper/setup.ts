import {insertUser, returnUserByEmail} from "../controller/databaseFetcher/user.ts";
import {User} from "../model/user.ts";
import {Invitation} from "../model/invitation.ts";
import {Relationships, Database} from 'https://deno.land/x/denodb/mod.ts';

export const setup = async (db: Database) => {

    Relationships.belongsTo(Invitation, User);
    db.link([User, Invitation]);
    await db.sync({drop: true});
    let admin = await returnUserByEmail(String(Deno.env.get("ADMIN_EMAIL")));
    if (!admin) {
        await insertUser(String(Deno.env.get("ADMIN_EMAIL")), String(Deno.env.get("ADMIN_PASSWORD")), true, "admin", "admin", "fully registered");
    }
}