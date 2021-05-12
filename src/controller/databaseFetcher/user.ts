import {User} from "../../model/user.ts";
import {hashPassword} from '../../helper/passwordHasher.ts';


export const returnUserByEmailAndPassword = async (eMail: string, password: string): Promise<User | undefined> => {
    let user = await User.where({eMail: eMail, password: hashPassword(password)}).get();
    if(Array.isArray(user)){
        return user[0];
    } else if(user instanceof User){
        return user;
    }
    return undefined;
}

export const returnUserByEmail = async (eMail: string): Promise<User | undefined> => {
    let user = await User.where({eMail: eMail}).get()

    if(Array.isArray(user)){
        return user[0];
    } else if(user instanceof User){
        return user;
    }
    return undefined;
}



export const insertUser = async (eMail: string, password: string, isAdmin: boolean, fullyRegistered: boolean, firstName: string, lastName: string): Promise<User> => {
    return await User.create({
        eMail: eMail,
        password: hashPassword(password),
        isAdmin: isAdmin,
        fullyRegistered: fullyRegistered,
        firstName: firstName,
        lastName: lastName
    });
}

export const insertUserForRegistration = async (eMail: string, password: string): Promise<User> => {
    return await User.create({eMail: eMail, password: hashPassword(password)});
}
