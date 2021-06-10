import {Context} from 'https://deno.land/x/oak/mod.ts';
import {checkAdmin, checkPO, createJWT, getUserID} from "./validation.ts";
import {insertUserForRegistration, returnUserByEmail} from "./databaseFetcher/user.ts";
import {User} from "../model/db/user.ts";
import {convertCtxBodyToUser, convertUserToUserProfile} from "../helper/userConverter.ts";
import {hashPassword} from "../helper/passwordHasher.ts";
import {getToken, insertToken} from "./databaseFetcher/token.ts";
import {EMailClient} from "../model/eMailClient.ts";
import {makeErrorMessage} from "../helper/error.ts";
import {urlSanitizer} from "../helper/url.ts";

const adminMail = Deno.env.get("ADMIN_EMAIL");
const url = Deno.env.get("URL");

export const createUser = async (ctx: Context, client: EMailClient) => {
    if (await checkAdmin(ctx) || await checkPO(ctx)) {
        const requestParameter = await ctx.request.body({type: "json"}).value;

        if (!requestParameter.email) {
            makeErrorMessage(ctx, 422, "no email provided")
            return;
        }

        let user = await insertUserForRegistration(requestParameter.email);
        let jwt = await createJWT(user)
        await insertToken(user, jwt);
        let linkText = "snowballR"

        if (url && adminMail) {
            await sendInvitationMail(jwt, linkText, url, requestParameter, Number(user.id), client);
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

export const resetPassword = async (ctx: Context, client: EMailClient) => {
    const requestParameter = await ctx.request.body({type: "json"}).value;

    if (!requestParameter.email) {
        makeErrorMessage(ctx, 422, "no email provided")
        return;
    }

    let user = await returnUserByEmail(requestParameter.email)
    if (user && url) {
        let jwt = await createJWT(user)
        await insertToken(user, jwt);
        let linkText = "snowballR"
        await sendResetMail(jwt, linkText, url, requestParameter, Number(user.id), client);
        ctx.response.status = 201;
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

//TODO PO && others
export const getUser = async (ctx: Context, id: string | undefined) => {
    if (id && (await checkAdmin(ctx) || await getUserID(ctx) === id)) {
        let user = await User.find(id);
        let userProfile = convertUserToUserProfile(user);
        ctx.response.body = JSON.stringify(userProfile);
        ctx.response.status = 200;

    }
}

export const patchUser = async (ctx: Context, id: number | undefined) => {
    let isSameUser = (await getUserID(ctx)) === id;
    let isAdmin = await checkAdmin(ctx);
    let isPO = await checkPO(ctx);
    let invitationToken = ctx.request.headers.get("invitationToken");
    let resetToken = ctx.request.headers.get("resetPasswordToken")
    let invitationTokenValid = false;
    let resetTokenValid = false;
    let userData = await convertCtxBodyToUser(ctx);
    if (invitationToken && id) {
        if (userData.password && userData.firstName) {
            invitationTokenValid = await checkToken(id, invitationToken, ctx);
        } else {
            makeErrorMessage(ctx, 400, "no password and/or firstName provided")
        }
    }
    if (resetToken && id) {
        if (userData.password) {
            resetTokenValid = await checkToken(id, resetToken, ctx);
        } else {
            makeErrorMessage(ctx, 400, "no password provided")
        }
    }
    if (id && (isSameUser || isAdmin || isPO || invitationTokenValid || resetTokenValid)) {
        let user = await User.find(id);

        if (isSameUser || invitationTokenValid || resetTokenValid) {
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

const checkToken = async (id: number, invitationToken: string, ctx: Context) => {
    let token = await getToken(id, invitationToken)
    if (token) {
        token.delete()
        return true;
    } else {
        makeErrorMessage(ctx, 401, "not authorized")
        return false;
    }
}

const sendInvitationMail = async (jwt: string, linkText: string, url: string, requestParameter: { email: string }, userId: number, client: EMailClient) => {
    url = urlSanitizer(url);
    url += "/register/?id=" + userId + "&token=" + jwt;
    let finalText = linkText.link(url);
    const content = `Welcome, </br>
                to finalize your registration for snowballR, please visit: ${finalText}. </br>
                Best Regards, </br>
                Your SnowballR Team`
    const html = `<h3>Welcome, </h3>
        <p>to finalize your registration for snowballR, please visit <a href="${url}">snowballR</a></p>
        <p>Best Regards,</p>
        <p>your snowballR Team</p>`

    await sendMail(requestParameter.email, client, html, content)
}

const sendResetMail = async (jwt: string, linkText: string, url: string, requestParameter: { email: string }, userId: number, client: EMailClient) => {
    url = urlSanitizer(url);
    url += "/resetpassword/?id=" + userId + "&token=" + jwt;
    let finalText = linkText.link(url);
    const content = `Hello, </br>
                to reset your password for snowballR, please visit: ${finalText}. </br>
                Best Regards, </br>
                Your SnowballR Team`
    const html = `<h3>Welcome, </h3>
        <p> to reset your password for snowballR, please visit <a href="${url}">snowballR</a></p>
        <p>Best Regards,</p>
        <p>your snowballR Team</p>`

    await sendMail(requestParameter.email, client, html, content)
}

const sendMail = async (mailTo: string, client: EMailClient, html: string, content: string) => {

    await client.connect({
        hostname: "mail.uni-ulm.de",
        port: 25,
    });
    await client.send({
        from: adminMail,
        to: mailTo,
        subject: "Invitation to join SnowballR",
        content: content,
        html: html
    })

    await client.close();
}
