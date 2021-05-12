import {returnUserByEmailAndPassword} from "./databaseFetcher/user.ts";
import {RouterContext} from 'https://deno.land/x/oak/mod.ts';
import {createToken} from "./validation.ts";

export const login = async (ctx: RouterContext) => {
    if(!ctx.params.username || !ctx.params.password){
        return false;
    }
    let user = await returnUserByEmailAndPassword(ctx.params.username, ctx.params.password);
    if (user) {
        let jwt = await createToken(user);
        ctx.cookies.set("token",jwt, {expires: new Date(Date.now() + 10000), httpOnly: true});
        ctx.response.redirect("/");
    } else{
        return
    }
}