import {UserIsPartOfProject} from "../../model/db/userIsPartOfProject.ts";
import {Model} from "https://deno.land/x/denodb/mod.ts";
import {Project} from "../../model/db/project.ts";
import {User} from "../../model/db/user.ts";
import {UserProfile} from "../../model/userProfile.ts";
import {convertUserToUserProfile} from "../../helper/converter/userConverter.ts";

export const getAllProjectsByUser = async (id: number) => {
    let userProjects = await UserIsPartOfProject.where("userId", id).get()
    let projects: Project[] = [];
    for (const element of <Model[]>userProjects) {
        let project = await Project.find(Number(element.projectId));
        projects.push(project)
    }
    return projects;
}

export const getAllMembersOfProject = async (id: number) => {
    let userProjects = await UserIsPartOfProject.where("projectId", id).get()
    let users: UserProfile[] = [];
    for (const element of <Model[]>userProjects) {
        let user = await User.find(Number(element.userId));
        let userProfile = convertUserToUserProfile(user);
        userProfile.isProjectOwner = Boolean(element.isOwner)
        users.push(userProfile)

    }
    return users;
}
