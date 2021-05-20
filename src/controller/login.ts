import {returnUserByEmailAndPassword} from "./databaseFetcher/user.ts";
import {Context} from 'https://deno.land/x/oak/mod.ts';
import {createJWT} from "./validation.ts";
import {createTerminationDate} from "../helper/dateHelper.ts";
import {User} from "../model/user.ts";
import {convertUserToUserProfile} from "../helper/userConverter.ts";

export const login = async (ctx: Context): Promise<boolean> => {
  //  const requestParameter = JSON.parse(ctx.request.body.toString());

    const requestParameter = await ctx.request.body({type: "json"}).value;

    if (!requestParameter.email || !requestParameter.password) {
        ctx.response.status = 401;
        ctx.response.body = {error: "no username or password provided"}
        return false;
    }

    let user = await returnUserByEmailAndPassword(requestParameter.email, requestParameter.password);
    if (user instanceof User) {
        ctx.response.body = JSON.stringify(convertUserToUserProfile(user));
        let jwt = await createJWT(user);
        ctx.cookies.set("token", jwt, {expires: createTerminationDate(), httpOnly: true});
        return true;
    } else {
        ctx.response.status = 401;
        ctx.response.body = {error: "wrong username or password provided"}
        return false;
    }

}