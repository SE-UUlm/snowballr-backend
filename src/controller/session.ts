import {createJWT} from "./validation.ts";
import {insertToken} from "./databaseFetcher/token.ts";
import {User} from "../model/db/user.ts";
import {Context} from 'https://deno.land/x/oak/mod.ts';


export const startSession = async (user: User, ctx: Context) =>{
    let jwt = await createJWT(user);
    await insertToken(user, jwt);
    ctx.cookies.set("token", jwt, {httpOnly: true});
}
