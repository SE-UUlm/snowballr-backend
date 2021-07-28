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
    status?: PaperStatus
    pdf?: string[]

}

export enum PaperStatus {
    finished,
    unfinished
}