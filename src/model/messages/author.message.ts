import {Status} from "./papersMessage.ts"

export interface AuthorMessage{
    firstName?: string,
    lastName?: string,
    rawString?: string,
    orcid?: string,
    status?: Status
}