import {insertUser, returnUserByEmail} from "../controller/databaseFetcher/user.ts";
import {db} from "../controller/database.ts";
import {User} from "../model/user.ts";
import {Invitation} from "../model/invitation.ts";

export const setup = async () => {
    db.link([User]);
    await db.sync({drop: false});
    let admin = await returnUserByEmail(String(Deno.env.get("ADMIN_EMAIL")));
    if (!admin) {
       await insertUser(String(Deno.env.get("ADMIN_EMAIL")), String(Deno.env.get("ADMIN_PASSWORD")), true, true, "admin", "admin");
    }
}