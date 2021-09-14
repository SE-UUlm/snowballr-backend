import { IApiAuthor } from "../../api/iApiAuthor.ts";
import { authorCache } from "../../controller/project.controller.ts";
import { Author } from "../../model/db/author.ts"
import { AuthorMessage } from "../../model/messages/author.message.ts"
import { Status } from "../../model/messages/papersMessage.ts";
export const checkIApiAuthor = (author: { [index: string]: any }): boolean => {
    let check = true;
    for (let i in author) {
        if (!["id"].includes(i)) {
            if ((author[i] && author[i].length > 1) || (i === "raw" && author[i].length >0)) {
                check = false;
            } else {
                delete author[i]
            }
        } else {
            delete author.id
        }
    }
    return check
}

export const convertAuthorToAuthorMessage = (author: Author): AuthorMessage => {
    let authorMessage: AuthorMessage = {};
    Object.assign(authorMessage, author)
    if (authorCache.has(String(author.id))) {
        authorMessage.status = Status.unfinished
    } else {
        authorMessage.status = Status.ready
    }
    return authorMessage
}