import {User} from "../model/user.ts";
import {UserProfile} from "../model/userProfile.ts";

export const convertUserToUserProfile = (user: User) => {
    let userProfile: UserProfile = {
        id: Number(user.id),
        firstName: String(user.firstName),
        lastName: String(user.lastName),
        email: String(user.lastName),
        isAdmin: Boolean(user.isAdmin),
        status: String(user.status)
    }
    return userProfile;
}