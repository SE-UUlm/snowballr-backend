/**
 * Payload saved in an JWT
 */
export interface PayloadJson {
    id: number
    firstName: string,
    lastName?: string,
    status: string,
    email: string,
    isAdmin: boolean
}