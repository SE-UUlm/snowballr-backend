import {User} from "../../model/db/user.ts";
import {Invitation} from "../../model/db/invitation.ts";

export const insertInvitation = async (user: User, token: string) => {
    return Invitation.create({token: token, userId: Number(user.id)}).catch(error => console.log(error))
}

export const getInvitation = async (userId: number, token: string) => {
    let foundInvitation = await Invitation.where({userId: userId, token: token}).get()
    if (Array.isArray(foundInvitation)) {
        return foundInvitation[0];
    }
    return undefined;
}

export const getInvitations = async (userId: number) => {
    let foundInvitations = await Invitation.where({userId: userId}).get()
    if (Array.isArray(foundInvitations)) {
        return foundInvitations;
    }
    return undefined;
}