import {Context} from 'https://deno.land/x/oak/mod.ts';
import {checkAdmin, createJWT, getUserID} from "./validation.ts";
import {insertUserForRegistration} from "./databaseFetcher/user.ts";
import {User} from "../model/db/user.ts";
import {convertCtxBodyToUser, convertUserToUserProfile} from "../helper/userConverter.ts";
import {hashPassword} from "../helper/passwordHasher.ts";
import {getToken, insertToken} from "./databaseFetcher/token.ts";
import {EMailClient} from "../model/eMailClient.ts";
import {makeErrorMessage} from "../helper/error.ts";
import {urlSanitizer} from "../helper/url.ts";
import {UserParameters} from "../model/userProfile.ts";
import {jsonBodyToObject} from "../helper/body.ts";


export const createUser = async (ctx: Context, client: EMailClient) => {
    if (await checkAdmin(ctx)) { //TODO add check for projectowner
        const requestParameter = await jsonBodyToObject(ctx)
        if (!requestParameter) {
            return
        }

        if (!requestParameter.email) {
            makeErrorMessage(ctx, 422, "no email provided")
            return;
        }

        let user = await insertUserForRegistration(requestParameter.email);
        let jwt = await createJWT(user)
        await insertToken(user, jwt);
        let linkText = "snowballR"
        let url = Deno.env.get("URL");
        let adminMail = Deno.env.get("ADMIN_EMAIL");
        if (url && adminMail) {
            await sendInvitationMail(jwt, linkText, url, adminMail, requestParameter, Number(user.id), client);
            ctx.response.status = 201;
            return user;
        } else {
            console.error("no url and/or no email in env!")
            makeErrorMessage(ctx, 401, "not authorized")
        }
    } else {
        makeErrorMessage(ctx, 401, "not authorized")
    }
    return undefined;
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
    if (id && await checkAdmin(ctx)) {
        let user = await User.find(id);
        let userProfile = convertUserToUserProfile(user);
        ctx.response.body = JSON.stringify(userProfile);
        ctx.response.status = 200;

    }
}

export const patchUser = async (ctx: Context, id: number | undefined) => {
    let isSameUser = (await getUserID(ctx)) === id;
    let isAdmin = await checkAdmin(ctx);
    let invitationToken = ctx.request.headers.get("invitationToken");
    let invitationTokenValid = false;
    let userData = await convertCtxBodyToUser(ctx);
    if (invitationToken && id) {
        invitationTokenValid = await checkToken(id, invitationToken, userData, ctx);
    }
    if (id && (isSameUser || isAdmin || invitationTokenValid)) {
        let user = await User.find(id);

        if (isSameUser || invitationTokenValid) {
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
        makeErrorMessage(ctx, 401, "not authorized");
    }
}

const checkToken = async (id: number, invitationToken: string, userData: UserParameters, ctx: Context) => {
    let token = await getToken(id, invitationToken)
    if (token) {
        if (userData.password && userData.firstName) {
            token.delete()
            return true;
        } else {
            makeErrorMessage(ctx, 400, "no password and/or firstName provided")
            return false;
        }
    } else {
        makeErrorMessage(ctx, 401, "not authorized")
        return false;
    }
}

const sendInvitationMail = async (jwt: string, linkText: string, url: string, adminMail: string, requestParameter: { email: string }, userId: number, client: EMailClient) => {
    url = urlSanitizer(url);
    url += "/register/?id=" + userId + "&token=" + jwt;
    let finalText = linkText.link(url);
    await client.connect({
        hostname: "mail.uni-ulm.de",
        port: 25,
    });
    await client.send({
        from: adminMail,
        to: requestParameter.email,
        subject: "Invitation to join SnowballR",
        content: `Welcome, </br>
                to finalize your registration for snowballR, please visit: ${finalText}. </br>
                Best Regards, </br>
                Your SnowballR Team`,
        html: `<h3>Welcome, </h3>
        <p>to finalize your registration for snowballR, please visit <a href="${url}">snowballR</a></p>
        <p>Best Regards,</p>
        <p>your snowballR Team</p>`
    })

    await client.close();
}
