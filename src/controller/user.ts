import {Context} from 'https://deno.land/x/oak/mod.ts';
import {checkAdmin} from "./validation.ts";
import {insertUserForRegistration} from "./databaseFetcher/user.ts";
import {User} from "../model/db/user.ts";
import {convertUserToUserProfile} from "../helper/userConverter.ts";

export const createUser = async (ctx: Context) => {
    if(!await checkAdmin(ctx)){ //TODO add check for projectowner
        const requestParameter = await ctx.request.body({type: "json"}).value;

        if (!requestParameter.email) {
            ctx.response.status = 401;
            ctx.response.body = {error: "no email provided"}
            return;
        }

        await insertUserForRegistration(requestParameter.email);
        ctx.response.status = 201;
    }
}

export const getUsers = async(ctx: Context) =>{
    if(await  checkAdmin(ctx)){
        let users = await User.all();
        let userProfile = users.map(user => convertUserToUserProfile(user));
        ctx.response.body = JSON.stringify(userProfile);
    }
}