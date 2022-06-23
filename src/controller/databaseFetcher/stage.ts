import {Stage} from "../../model/db/stage.ts";

export const getAllStagesFromProject = async (id: number) => {
    let stages = await Stage.where("projectId", id).get()
    if(Array.isArray(stages)){
        return stages;
    }
    return new Array<Stage>();
}