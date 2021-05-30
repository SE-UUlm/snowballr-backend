export interface UserBasic {
    id?: number,
    firstName?: string,
    lastName?: string,
    email?: string,

}

export interface UserProfile extends UserBasic{
    status?: string,
    isAdmin?: boolean
}

export interface UserFields extends UserProfile{
    password?: string
}