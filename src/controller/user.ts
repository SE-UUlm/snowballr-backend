import {Context} from 'https://deno.land/x/oak/mod.ts';
import {checkAdmin, getUserID} from "./validation.ts";
import {insertUserForRegistration} from "./databaseFetcher/user.ts";
import {User} from "../model/db/user.ts";
import {convertCtxBodyToUser, convertUserToUserProfile} from "../helper/userConverter.ts";
import {hashPassword} from "../helper/passwordHasher.ts";

export const createUser = async (ctx: Context) => {
    if (await checkAdmin(ctx)) { //TODO add check for projectowner
        const requestParameter = await ctx.request.body({type: "json"}).value;

        if (!requestParameter.email) {
            ctx.response.status = 422;
            ctx.response.body = {error: "no email provided"}
            return;
        }

        await insertUserForRegistration(requestParameter.email);
        ctx.response.status = 201;
    } else {
        ctx.response.status = 401;
    }
}

export const getUsers = async (ctx: Context) => {
    if (await checkAdmin(ctx)) {
        let users = await User.all();
        let userProfile = users.map(user => convertUserToUserProfile(user));
        ctx.response.body = JSON.stringify(userProfile);
        ctx.response.status = 200;
        return true;
    }
    return false;
}

//TODO user himself && PO && others
export const getUser = async (ctx: Context, id: string | undefined) => {
    if (id) {
        if (await checkAdmin(ctx)) {
            let user = await User.find(id);
            let userProfile = convertUserToUserProfile(user);
            ctx.response.body = JSON.stringify(userProfile);
            ctx.response.status = 200;
        }
    }
}

export const patchUser = async (ctx: Context, id: number | undefined) => {
    let isSameUser = (await getUserID(ctx)) === id;
    let isAdmin = await checkAdmin(ctx);
    if (id && (isSameUser || isAdmin)) {
        let userData = await convertCtxBodyToUser(ctx);
        let user = await User.find(id);
        if (isSameUser) {
            if (userData.password) {
                user.password = hashPassword(userData.password)
            }
        }
        if (isAdmin) {
            if (userData.status) {
                user.status = userData.status;
            }
            if (userData.isAdmin !== undefined) {
                user.isAdmin = userData.isAdmin;
            }

        }
        if (userData.email) {
            user.eMail = userData.email
        }
        if (userData.firstName) {
            user.firstName = userData.firstName;
        }
        if (userData.lastName) {
            user.lastName = userData.lastName;
        }
        user = await user.update();
        let userProfile = convertUserToUserProfile(user);
        ctx.response.body = JSON.stringify(userProfile);
        ctx.response.status = 200;
    } else {
        ctx.response.status = 401;
    }
}

