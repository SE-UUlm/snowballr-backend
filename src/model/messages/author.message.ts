import {Status} from "./papersMessage.ts"

export interface AuthorMessage{
    firstName?: string,
    lastName?: string,
    raw?: string,
    orcid?: string,
    status?: Status
}