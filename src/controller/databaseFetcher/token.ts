import {User} from "../../model/db/user.ts";
import {Token} from "../../model/db/token.ts";

export const insertToken = async (user: User, token: string) => {
    await Token.create({token: token, userId:  Number(user.id)}).catch(error => console.log(error))
}

export const getToken = async(userId: number, token: string) => {
    let foundToken = await Token.where({userId: userId, token: token}).get()
    if (Array.isArray(foundToken)) {
        return foundToken[0];
    }
    return undefined;
}