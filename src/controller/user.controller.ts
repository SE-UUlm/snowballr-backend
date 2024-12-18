import { Context } from "https://deno.land/x/oak@v11.1.0/mod.ts";
import {
  checkAdmin,
  checkPO,
  createJWT,
  createRefreshJWT,
  getPayloadFromJWTHeader,
  getUserID,
  getUserName,
  UserStatus,
  validateUserEntry,
} from "./validation.controller.ts";
import {
  insertUserForRegistration,
  removeUser,
  returnUserByEmail,
} from "./databaseFetcher/user.ts";
import { User } from "../model/db/user.ts";
import {
  convertCtxBodyToUser,
  convertUserToUserProfile,
} from "../helper/converter/userConverter.ts";
import { EMailClient } from "../model/eMailClient.ts";
import { makeErrorMessage } from "../helper/error.ts";
import { urlSanitizer } from "../helper/url.ts";
import {
  getInvitation,
  insertInvitation,
} from "./databaseFetcher/invitation.ts";
import {
  getResetToken,
  insertResetToken,
} from "./databaseFetcher/resetToken.ts";
import { getAllProjectsByUser } from "./databaseFetcher/userProject.ts";
import { hashPassword } from "../helper/passwordHasher.ts";
import { UserParameters } from "../model/userProfile.ts";
import { convertProjectToProjectMessage } from "../helper/converter/projectConverter.ts";
import { UsersMessage } from "../model/messages/user.message.ts";
import { logger } from "../api/logger.ts";

const adminMail = Deno.env.get("ADMIN_EMAIL");
const URL = Deno.env.get("URL");

class MailingError extends Error {
  constructor(message: string) {
    super(message);
    this.name = "MailingError";
  }
}

Deno.errors.MailingError = MailingError;
Object.freeze(Deno.errors);

/**
 * Creates a user for registration
 *
 * @param ctx
 * @param client Only necessary to throw in an empty mock to not send mails during tests
 */
export const createUser = async (ctx: Context, client: EMailClient) => {
  const validate = await validateUserEntry(ctx, [], UserStatus.needsPO, -1, {
    needed: true,
    params: ["email", "sender"],
  });
  if (validate) {
    const payloadJson = await getPayloadFromJWTHeader(ctx);
    let user = null;
    try {
      user = await insertUserForRegistration(validate.email);
      const jwt = await createRefreshJWT(Number(user.id));
      await insertInvitation(user, jwt);
      const linkText = "snowballR";

      if (adminMail) {
        console.log(adminMail);
        await sendInvitationMail(
          jwt,
          linkText,
          validate.email,
          Number(user.id),
          client,
          await getUserName(payloadJson),
        );
        ctx.response.status = 201;
        ctx.response.body = JSON.stringify(convertUserToUserProfile(user));
      } else {
        console.error("no email in env!");

        makeErrorMessage(ctx, 401, "not authorized");
      }
    } catch (err) {
      console.log("-------- MAIL ERROR --------------------");
      //console.log(typeof err);
      //console.log(err);
      if (err instanceof Deno.errors.AddrNotAvailable) {
        if (user) removeUser(user.id);
        try {
          sendInvitationFailedMail(validate.email, validate.sender, client);
        } catch (e) {
          console.log(e);
          makeErrorMessage(
            ctx,
            424,
            "couldn't send mail and couldn't alert you via mail.",
          );
        }
        makeErrorMessage(ctx, 423, "couldn't send mail. might be invalid!");
      } else {
        makeErrorMessage(ctx, 422, "email already exists");
      }
    }
  }
};

/**
 * Allows emailreset, if email is correct and provided
 * @param ctx
 * @param client
 */
export const resetPassword = async (ctx: Context, client: EMailClient) => {
  const validate = await validateUserEntry(ctx, [], UserStatus.none, -1, {
    needed: true,
    params: ["email"],
  });
  if (validate) {
    const user = await returnUserByEmail(validate.email);
    if (user && URL) {
      const jwt = await createJWT(user);
      await insertResetToken(user, jwt);
      const linkText = "snowballR";
      await sendResetMail(jwt, linkText, validate.email, user, client);
      ctx.response.status = 200;
    } else {
      makeErrorMessage(ctx, 400, "wrong email provided");
    }
  }
};

/**
 * Gets all user instances for admin and PO
 * @param ctx
 */
export const getUsers = async (ctx: Context) => {
  const validate = await validateUserEntry(ctx, [], UserStatus.needsPO, -1, {
    needed: false,
    params: [],
  });
  if (validate) {
    const users = await User.all();
    const userProfile = users.map((user) => convertUserToUserProfile(user));
    const userMessage: UsersMessage = { users: userProfile };
    ctx.response.body = JSON.stringify(userMessage);
    ctx.response.status = 200;
  }
};

/**
 * Gets a single user profile for admin and PO
 * @param ctx
 * @param id
 */
export const getUser = async (ctx: Context, id: number) => {
  const validate = await validateUserEntry(
    ctx,
    [id],
    UserStatus.needsSameUserOrPO,
    -1,
    { needed: false, params: [] },
    id,
  );
  if (validate) {
    const user = await User.find(id);
    if (user) {
      const userProfile = convertUserToUserProfile(user);
      ctx.response.body = JSON.stringify(userProfile);
      ctx.response.status = 200;
    } else {
      makeErrorMessage(ctx, 404, "User not Found");
    }
  }
};

/**
 * Get all projects of a user
 * @param ctx
 * @param id
 */
export const getUserProjects = async (ctx: Context, id: number) => {
  const validate = await validateUserEntry(
    ctx,
    [id],
    UserStatus.needsSameUser,
    -1,
    { needed: false, params: [] },
    id,
  );
  if (validate) {
    const userProjects = await getAllProjectsByUser(id);
    if (userProjects) {
      ctx.response.body = JSON.stringify(
        await convertProjectToProjectMessage(userProjects),
      );
      ctx.response.status = 200;
    }
  }
};

/**
 * Allows user patch for admins, POs, the user himself, new registered users and reset password requests.
 * Only allows to change values allowed by the project definition.
 * @param ctx
 * @param id
 */
export const patchUser = async (ctx: Context, id: number | undefined) => {
  if (!id) {
    makeErrorMessage(ctx, 422, "no user id included");
    return;
  }
  const payloadJson = await getPayloadFromJWTHeader(ctx);
  const userData = await convertCtxBodyToUser(ctx);
  const isAdmin = await checkAdmin(payloadJson);
  const isPO = await checkPO(payloadJson);

  const checkedToken = await checkToken(id, ctx, userData);
  const isSameUser = (await getUserID(payloadJson)) === id ||
    checkedToken.valid;
  const register = checkedToken.kind === "invitation";
  if (isSameUser || isAdmin || isPO) {
    const user = await changeUserData(
      ctx,
      id,
      isSameUser,
      isAdmin,
      userData,
      register,
    );
    if (user) {
      const userProfile = convertUserToUserProfile(user);
      ctx.response.body = JSON.stringify(userProfile);
      ctx.response.status = 200;
    }
  } else {
    if (ctx.response.status !== 400) {
      makeErrorMessage(ctx, 401, "not authorized");
    }
  }
};

const changeUserData = async (
  ctx: Context,
  id: number,
  isSameUser: boolean,
  isAdmin: boolean,
  userData: UserParameters,
  register: boolean,
) => {
  const user = await User.find(id);

  if (!user) {
    makeErrorMessage(ctx, 404, "User not Found");
    return;
  }

  if (isSameUser) {
    if (userData.password) {
      user.password = hashPassword(userData.password);
    }
  }

  if (register) {
    user.status = "active";
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
    user.eMail = userData.email;
  }
  if (userData.firstName) {
    user.firstName = userData.firstName;
  }
  if (userData.lastName) {
    user.lastName = userData.lastName;
  }
  return user.update();
};
/**
 * Checks whether a resetToken or an invitationToken is set in the header and if it is valid
 *
 * @param id id of the user
 * @param ctx oak context
 * @param userData the data sent to change the user data
 */
const checkToken = async (
  id: number,
  ctx: Context,
  userData: UserParameters,
) => {
  let validToken = { valid: false, kind: "" };
  const invitationToken = ctx.request.headers.get("invitationToken");
  const resetToken = ctx.request.headers.get("resetToken");
  logger.error(`invitation: ${JSON.stringify(invitationToken)}`);
  if (invitationToken) {
    if (userData.password && userData.firstName) {
      validToken = {
        valid: await checkInvitationToken(id, invitationToken, ctx),
        kind: "invitation",
      };
    } else {
      makeErrorMessage(ctx, 400, "no password and/or firstName provided");
    }
  }
  if (resetToken) {
    if (userData.password) {
      validToken = {
        valid: await checkResetToken(id, resetToken, ctx),
        kind: "reset",
      };
    } else {
      makeErrorMessage(ctx, 400, "no password provided");
    }
  }

  return validToken;
};

/**
 * Checks whether a token is valid for registering.
 * @param id id of the user belonging to the token
 * @param providedToken Token provided by the user
 * @param ctx
 */
const checkInvitationToken = async (
  id: number,
  providedToken: string,
  ctx: Context,
) => {
  const token = await getInvitation(id, providedToken);
  if (token) {
    token.delete();
    return true;
  } else {
    makeErrorMessage(ctx, 401, "Invitation token not found");
    return false;
  }
};

/**
 * Checks whether a token is valid for registering.
 * @param id id of the user belonging to the token
 * @param providedToken Token provided by the user
 * @param ctx
 */
const checkResetToken = async (
  id: number,
  providedToken: string,
  ctx: Context,
) => {
  const token = await getResetToken(id, providedToken);
  if (token) {
    token.delete();
    return true;
  } else {
    makeErrorMessage(ctx, 401, "not authorized");
    return false;
  }
};

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
const sendInvitationMail = async (
  jwt: string,
  linkText: string,
  email: string,
  userId: number,
  client: EMailClient,
  name?: string,
) => {
  if (URL) {
    let url = urlSanitizer(URL);
    url += "/register?id=" + userId + "&token=" + jwt;
    // let finalText = linkText.link(url);
    const content = `Welcome, </br>
		you were invited to join snowballR by ${
      name ? name : `your snowballR Team`
    }</br>
        to finalize your registration for snowballR, please visit: ${url}.</br>
        Best Regards, </br>
        Your SnowballR Team`;
    const html = ` <h3> Welcome, </h3>
		<p> you were invited to join snowballR by ${
      name ? name : `your snowballR Team`
    } </p>
        <p> to finalize your registration for snowballR, please visit <a href = "${url}" > ${url} </a></p>
        <p>Best Regards, </p>` +
      (name ? `<p>${name}</p>` : `<p>your snowballR Team</p>`);
    try {
      await sendMail(
        email,
        client,
        html,
        content,
        "Invitation to join SnowballR",
        "SnowballR",
      );
    } catch (err) {
      throw new Deno.errors.AddrNotAvailable(err);
    }
  } else {
    console.error("no URL in env!");
  }
};

const sendInvitationFailedMail = async (
  invitationMail: string,
  senderMail: string,
  client: EMailClient,
  name?: string,
) => {
  if (URL) {
    //let url = urlSanitizer(URL);
    //url += "/register?id=" + userId + "&token=" + jwt;
    //let finalText = linkText.link(url);
    const content =
      `Your invitation of given e-mail ${invitationMail} failed!</br>
        Best Regards, </br>
        Your SnowballR Team`;
    const html =
      `<p>Your invitation of given e-mail ${invitationMail} failed!</p>
        <p>Best Regards, </p>` +
      (name ? `<p>${name}</p>` : `<p>your snowballR Team</p>`);

    await sendMail(
      senderMail,
      client,
      html,
      content,
      "Couldn't send InvitationFailed mail",
      "SnowballR",
    );
  } else {
    console.error("no URL in env!");
  }
};

/**
 * Formats the email for resetting a password
 * @param jwt token needed to be allowed to patch a user object
 * @param linkText text displayed instead of the url
 * @param url
 * @param email email of the person wanting to reset his password
 * @param userId
 * @param client email client to send the email with
 */
const sendResetMail = async (
  jwt: string,
  linkText: string,
  email: string,
  user: User,
  client: EMailClient,
) => {
  if (URL) {
    let url = urlSanitizer(URL);
    url += "/resetpassword?id=" + Number(user.id) + "&token=" + jwt;
    // let finalText = linkText.link(url);
    const content = `Hello ${String(user.firstName)} ${
      String(user.lastName)
    }, </br>
                to reset your password for snowballR, please visit: ${url}. </br>
                Best Regards, </br>
                Your SnowballR Team`;
    const html = `<h3>Hello ${String(user.firstName)} ${
      String(user.lastName)
    }, </h3>
        <p> to reset your password for snowballR, please visit <a href="${url}">${url}</a></p>
        <p>Best Regards,</p>
        <p>your snowballR Team</p>`;

    await sendMail(
      email,
      client,
      html,
      content,
      "Password reset for SnowballR",
      "SnowballR",
    );
  } else {
    console.error("no URL in env!");
  }
};

/**
 * Sends the email
 * @param mailTo email that gets the send email
 * @param client email client to send the email with
 * @param html html body
 * @param content alternative content if html not allowed
 * @param header header of email
 * @param name name of the person sending the mail
 */
const sendMail = async (
  mailTo: string,
  client: EMailClient,
  html: string,
  content: string,
  header: string,
  name?: string,
) => {
  await client.connect({
    hostname: "mail.uni-ulm.de",
    port: 25,
  });
  const response = await client.send({
    from: name ? `${name} <${adminMail}>` : adminMail,
    to: mailTo,
    subject: header,
    content: content,
    html: html,
  });
  console.log("---------RESPONSE---------");
  console.log(response);

  await client.close();
};
