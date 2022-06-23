import { Author } from "../../model/db/author.ts";
import { Paper } from "../../model/db/paper.ts";
import { Wrote } from "../../model/db/wrote.ts";

export const getAllAuthorsFromPaper = async (id: number) => {
    let wrote = await Wrote.where("paperId", id).get()

    if (Array.isArray(wrote)) {
        let authors: Author[] = [];
        for (let item of wrote) {
            authors.push(await Author.find(Number(item.authorId)))
        }
        return authors
    }
    return new Array<Author>();
}