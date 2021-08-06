import { Author } from "../../model/db/author.ts";
import { Paper } from "../../model/db/paper.ts";
import { Wrote } from "../../model/db/wrote.ts";

export const getAllAuthorsFromPaper = async (id: number) => {
    let wrote = await Wrote.where("paperId", id).get()

    if (Array.isArray(wrote)) {
        let authorPromises: Promise<Author>[] = [];
        wrote.forEach((item: Wrote) => {
            authorPromises.push(Author.find(Number(item.authorId)))
        })
        return Promise.all(authorPromises)
    }
    return new Array<Paper>();
}