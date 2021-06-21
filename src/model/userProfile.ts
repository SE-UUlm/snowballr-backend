export interface UserBasic {
    id?: number,
    firstName?: string,
    lastName?: string,
    email?: string,

}

export interface UserProfile extends UserBasic {
    status?: string,
    isAdmin?: boolean,
    isProjectOwner?: boolean
}

export interface UserParameters extends UserProfile {
    password?: string
}