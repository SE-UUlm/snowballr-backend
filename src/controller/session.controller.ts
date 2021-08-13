import { createJWT } from "./validation.controller.ts";
import { insertToken } from "./databaseFetcher/token.ts";
import { User } from "../model/db/user.ts";

/**
 * starts a session by creating a JWT and inserting it into the database
 * @param user
 */
export const startSession = async (user: User) => {
    let jwt = await createJWT(user);
    await insertToken(user, jwt);
    return jwt;
}
