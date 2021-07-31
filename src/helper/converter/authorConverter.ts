import { IApiAuthor } from "../../api/iApiAuthor.ts";

export const checkIApiAuthor = (author: { [index: string]: any }): boolean => {
    let check = true;
    for (let i in author) {
        if (!["id"].includes(i)) {
            if (author[i] && author[i].length > 1) {
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