import {Context} from 'https://deno.land/x/oak/mod.ts';
import {checkAdmin, checkPO, createJWT, getPayloadFromJWT, getUserID, getUserName} from "./validation.ts";
import {insertUserForRegistration, returnUserByEmail} from "./databaseFetcher/user.ts";
import {User} from "../model/db/user.ts";
import {convertCtxBodyToUser, convertUserToUserProfile} from "../helper/userConverter.ts";
import {EMailClient} from "../model/eMailClient.ts";
import {makeErrorMessage} from "../helper/error.ts";
import {urlSanitizer} from "../helper/url.ts";
import {jsonBodyToObject} from "../helper/body.ts";
import {getInvitation, insertInvitation} from "./databaseFetcher/invitation.ts";
import {getResetToken, insertResetToken} from "./databaseFetcher/resetToken.ts";
import {getAllProjectsByUser} from "./databaseFetcher/userProject.ts";
import {hashPassword} from "../helper/passwordHasher.ts";

const adminMail = Deno.env.get("ADMIN_EMAIL");
const url = Deno.env.get("URL");

/**
 * Creates a user for registration
 *
 * @param ctx
 * @param client Only necessary to throw in an empty mock to not send mails during tests
 */
export const createUser = async (ctx: Context, client: EMailClient) => {
    const payloadJson = await getPayloadFromJWT(ctx);
    if (await checkAdmin(payloadJson) || await checkPO(payloadJson)) {
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
        await insertInvitation(user, jwt);
        let linkText = "snowballR"

        if (url && adminMail) {
            await sendInvitationMail(jwt, linkText, url, requestParameter.email, Number(user.id), client, await getUserName(payloadJson));
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

/**
 * Allows emailreset, if email is correct and provided
 * @param ctx
 * @param client
 */
export const resetPassword = async (ctx: Context, client: EMailClient) => {
    const requestParameter = await ctx.request.body({type: "json"}).value;

    if (!requestParameter.email) {
        makeErrorMessage(ctx, 422, "no email provided")
        return;
    }

    let user = await returnUserByEmail(requestParameter.email)
    if (user && url) {
        let jwt = await createJWT(user)
        await insertResetToken(user, jwt);
        let linkText = "snowballR"
        await sendResetMail(jwt, linkText, url, requestParameter.email, Number(user.id), client);
        ctx.response.status = 201;
    } else {
        makeErrorMessage(ctx, 400, "wrong email provided")
        return;
    }
}

//TODO: others
/**
 * Gets all user instances for admin and PO
 * @param ctx
 */
export const getUsers = async (ctx: Context) => {
    const payloadJson = await getPayloadFromJWT(ctx);
    if (await checkAdmin(payloadJson) || await checkPO(payloadJson)) {
        let users = await User.all();
        let userProfile = users.map(user => convertUserToUserProfile(user));
        ctx.response.body = JSON.stringify(userProfile);
        ctx.response.status = 200;
        return true;
    }
    return false;
}

//TODO: others
/**
 * Gets a single user profile for admin and PO
 * @param ctx
 * @param id
 */
export const getUser = async (ctx: Context, id: number | undefined) => {
    if (!id) {
        makeErrorMessage(ctx, 400, "no user id included")
        return
    }
    const payloadJson = await getPayloadFromJWT(ctx);
    if (await checkAdmin(payloadJson) || await getUserID(payloadJson) === id || await checkPO(payloadJson)) {
        let user = await User.find(id);
        let userProfile = convertUserToUserProfile(user);
        ctx.response.body = JSON.stringify(userProfile);
        ctx.response.status = 200;

    }
}

/**
 * Get all projects of a user
 * @param ctx
 * @param id
 */
export const getUserProjects = async (ctx: Context, id: number | undefined) => {
    if (!id) {
        makeErrorMessage(ctx, 400, "no user id included")
        return
    }
    const payloadJson = await getPayloadFromJWT(ctx);
    if (await checkAdmin(payloadJson) || await getUserID(payloadJson) === id) {
        let userProjects = await getAllProjectsByUser(id)
        ctx.response.body = `{projects: ${JSON.stringify(userProjects)}}`;
        ctx.response.status = 200;

    }
}

/**
 * Allows user patch for admins, POs, the user himself, new registered users and reset password requests.
 * Only allows to change values allowed by the project definition.
 * @param ctx
 * @param id
 */
export const patchUser = async (ctx: Context, id: number | undefined) => {
    if (!id) {
        makeErrorMessage(ctx, 400, "no user id included")
        return;
    }
    const payloadJson = await getPayloadFromJWT(ctx);
    let isSameUser = (await getUserID(payloadJson)) === id;
    let isAdmin = await checkAdmin(payloadJson);
    let isPO = await checkPO(payloadJson);
    let invitationToken = ctx.request.headers.get("invitationToken");
    let resetToken = ctx.request.headers.get("resetPasswordToken")
    let invitationTokenValid = false;
    let resetTokenValid = false;
    let userData = await convertCtxBodyToUser(ctx);
    if (invitationToken) {
        if (userData.password && userData.firstName) {
            invitationTokenValid = await checkInvitationToken(id, invitationToken, ctx);
        } else {
            makeErrorMessage(ctx, 400, "no password and/or firstName provided")
        }
    }
    if (resetToken) {
        if (userData.password) {
            resetTokenValid = await checkResetToken(id, resetToken, ctx);
        } else {
            makeErrorMessage(ctx, 400, "no password provided")
        }
    }
    if (isSameUser || isAdmin || isPO || invitationTokenValid || resetTokenValid) {
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

/**
 * Checks whether a token is valid for registering.
 * @param id id of the user belonging to the token
 * @param providedToken Token provided by the user
 * @param ctx
 */
const checkInvitationToken = async (id: number, providedToken: string, ctx: Context) => {
    let token = await getInvitation(id, providedToken)
    if (token) {
        token.delete()
        return true;
    } else {
        makeErrorMessage(ctx, 401, "not authorized")
        return false;
    }
}

/**
 * Checks whether a token is valid for registering.
 * @param id id of the user belonging to the token
 * @param providedToken Token provided by the user
 * @param ctx
 */
const checkResetToken = async (id: number, providedToken: string, ctx: Context) => {
    let token = await getResetToken(id, providedToken)
    if (token) {
        token.delete()
        return true;
    } else {
        makeErrorMessage(ctx, 401, "not authorized")
        return false;
    }
}

/**
 * Forms the invitation mail
 * @param jwt token the user has to have to be allowed to make a registering patch
 * @param linkText text displayed instead of the url
 * @param url
 * @param email email of the invited user
 * @param userId userID of the new user
 * @param client email client to send the email with
 * @param name name of the person who invited the new user
 */
const sendInvitationMail = async (jwt: string, linkText: string, url: string, email: string, userId: number, client: EMailClient, name?: string) => {
    url = urlSanitizer(url);
    url += "/register/?id=" + userId + "&token=" + jwt;
    let finalText = linkText.link(url);
    const content = `
        Welcome, </br>
        to
        finalize
        your
        registration
        for snowballR, please visit: ${finalText}.
        </br>
        Best
        Regards, </br>
        Your
        SnowballR
        Team`
    const html = ` < h3 > Welcome, </h3>
        < p > to
        finalize
        your
        registration
        for snowballR, please visit < a
        href = "${url}" > snowballR < /a></
        p >
        <p>Best
        Regards, </p>` +
        (name ? `<p>${name}</p>` : `<p>your snowballR Team</p>`)

    await sendMail(email, client, html, content, "Invitation to join SnowballR", name)
}

/**
 * Formats the email for resetting a password
 * @param jwt token needed to be allowed to patch a user object
 * @param linkText text displayed instead of the url
 * @param url
 * @param email email of the person wanting to reset his password
 * @param userId
 * @param client email client to send the email with
 */
const sendResetMail = async (jwt: string, linkText: string, url: string, email: string, userId: number, client: EMailClient) => {
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

    await sendMail(email, client, html, content, "Password reset for SnowballR")
}

/**
 * Sends the email
 * @param mailTo email that gets the send email
 * @param client email client to send the email with
 * @param html html body
 * @param content alternative content if html not allowed
 * @param header header of email
 * @param name name of the person sending the mail
 */
const sendMail = async (mailTo: string, client: EMailClient, html: string, content: string, header: string, name?: string) => {

    await client.connect({
        hostname: "mail.uni-ulm.de",
        port: 25,
    });
    await client.send({
        from: name ? `${name} <${adminMail}>` : adminMail,
        to: mailTo,
        subject: header,
        content: content,
        html: html
    })

    await client.close();
}
