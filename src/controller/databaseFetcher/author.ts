import { Paper } from "../../model/db/paper.ts";
import { Wrote } from "../../model/db/wrote.ts";

export const getAllAuthorsFromPaper = async (id: number) => {
    let wrote = await Wrote.where("paperId", id).get()

    if (Array.isArray(wrote)) {
        let paperPromises: Promise<Paper>[] = [];
        wrote.forEach((item: Wrote) => {
            paperPromises.push(Paper.find(Number(item.paperId)))
        })
        return Promise.all(paperPromises)
    }
    return new Array<Paper>();
}