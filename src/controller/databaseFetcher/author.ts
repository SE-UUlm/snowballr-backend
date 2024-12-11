import { Author } from "../../model/db/author.ts";
import { Wrote } from "../../model/db/wrote.ts";

export const getAllAuthorsFromPaper = async (id: number) => {
    const wrote = await Wrote.where("paperId", id).get()

    if (Array.isArray(wrote)) {
        const authors: Author[] = [];
        for (const item of wrote) {
            authors.push(await Author.find(Number(item.authorId)))
        }
        return authors
    }
    return new Array<Author>();
}