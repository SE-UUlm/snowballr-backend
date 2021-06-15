import {UserIsPartOfProject} from "../../model/db/userIsPartOfProject.ts";
import {Model} from "https://deno.land/x/denodb/mod.ts";
import {Project} from "../../model/db/project.ts";

export const getAllProjectsByUser = async (id: number) => {
    let userProjects = await UserIsPartOfProject.where("userId", id).get()
    let projects: Project[] = [];
    for (const element of <Model[]>userProjects) {
        let project = await Project.find(Number(element.projectId));
        projects.push(project)
    }
    return projects;
}