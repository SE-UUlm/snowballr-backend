import { Author } from "../db/author.ts";
import { AuthorMessage } from "./author.message.ts";

export interface PapersMessage {
    papers: PaperMessage[]
}

export interface PaperMessage {
    id: number,
    ppid?: number,
    doi?: string,
    title?: string,
    abstract?: string,
    year?: Date,
    publisher?: string,
    type?: string,
    scope?: string,
    scopeName?: string,
    createdAt?: Date,
    updatedAt?: Date
    status?: Status
    finalDecision?: string
    pdf: string[]
    authors: AuthorMessage[]

}

export enum Status {
    unfinished = "unfinished",
    ready = "ready",
    partiallyEvaluated = "partiallyEvaluated",
    evaluatedByMyself = "evaluatedByMyself",
    completelyEvaluated = "completlyEvaluated",
}
