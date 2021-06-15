import {User} from "../model/db/user.ts";
import {UserParameters, UserProfile} from "../model/userProfile.ts";
import {Context} from 'https://deno.land/x/oak/mod.ts';

/**
 * Makes sure the password isn't send while delivering a user object
 * @param user
 */
export const convertUserToUserProfile = (user: User) => {
    let userProfile: UserProfile = {
        id: Number(user.id),
        email: String(user.lastName),
        isAdmin: Boolean(user.isAdmin),
        status: String(user.status)
    }
    if (user.firstName) {
        userProfile.firstName = String(user.firstName);
    }

    if (user.lastName) {
        userProfile.lastName = String(user.lastName);
    }

    return userProfile;
}

export const convertCtxBodyToUser = async (ctx: Context): Promise<UserParameters> => {
    const bodyJson = await ctx.request.body({type: "json"}).value;
    let userData: UserParameters = {};
    if (bodyJson.email) {
        userData.email = bodyJson.email
    }
    if (bodyJson.firstName) {
        userData.firstName = bodyJson.firstName
    }
    if (bodyJson.lastName) {
        userData.lastName = bodyJson.lastName
    }
    if (bodyJson.password) {
        userData.password = bodyJson.password
    }
    if (bodyJson.status) {
        userData.status = bodyJson.status
    }
    if (bodyJson.isAdmin !== undefined) {
        userData.isAdmin = bodyJson.isAdmin
    }
    return userData;
}