import {returnUserByEmailAndPassword} from "./databaseFetcher/user.ts";
import {RouterContext} from 'https://deno.land/x/oak/mod.ts';
import {createToken} from "./validation.ts";
import {createTerminationDate} from "../helper/dateHelper.ts";

export const login = async (ctx: RouterContext) => {
    if(!ctx.params.username || !ctx.params.password){
        return false;
    }
    let user = await returnUserByEmailAndPassword(ctx.params.username, ctx.params.password);
    if (user) {
        let jwt = await createToken(user);
        ctx.cookies.set("token",jwt, {expires: createTerminationDate(), httpOnly: true});
        ctx.response.redirect("/");
    } else{
        return
    }
}