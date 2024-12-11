import {UserIsPartOfProject} from "../../model/db/userIsPartOfProject.ts";
import {Project} from "../../model/db/project.ts";
import {User} from "../../model/db/user.ts";
import {convertUserToUserProfile} from "../../helper/converter/userConverter.ts";
import {UserProfile} from "../../model/userProfile.ts";

export const getAllProjectsByUser = async (id: number) => {
    const userProjects = await UserIsPartOfProject.where("userId", id).get()
    if (Array.isArray(userProjects)) {
        const projects: Promise<Project>[] = [];
        userProjects.forEach((item: UserIsPartOfProject) => {
            const project = Project.find(Number(item.projectId));
            projects.push(project)
        })

        return Promise.all(projects);
    }
    return new Array<Project>()
}

export const getAllMembersOfProject = async (id: number): Promise<UserProfile[]> => {
    const userProjects = await UserIsPartOfProject.where("projectId", id).get()
    if (Array.isArray(userProjects)) {
        const users: Promise<User>[] = [];
        userProjects.forEach((item: UserIsPartOfProject) => {
            const user = User.find(Number(item.userId));
            users.push(user)
        })

        return (await Promise.all(users)).map(item => {
            return convertUserToUserProfile(item);
        })
    }
    return new Array<UserProfile>()
}
