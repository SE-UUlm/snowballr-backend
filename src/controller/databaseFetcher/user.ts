import {User} from "../../model/user.ts";


export const returnUserByEmailAndPassword = async (eMail: string, password: string) => {
    let user = await User.where({eMail: eMail, password: password}).take(0).get();
    if(user){
        return user;
    } else {
        return undefined;
    }
}