/**
 * Parameters which are queriable by most apis. TBI
 */

export interface IApiQuery {
    firstName?: string;
    lastName?: string;
    rawName: string;
    title: string;
    id: string;
    year?: number;
    publisher?: string;
    type?: string;
}