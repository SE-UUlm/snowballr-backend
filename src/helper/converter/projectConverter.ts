import { User } from "../../model/db/user.ts";
import { UserProfile } from "../../model/userProfile.ts";
import { Project } from "../../model/db/project.ts";
import { ProjectMessage, ProjectMessageItem } from "../../model/messages/project.message.ts";
import { getAllStagesFromProject } from "../../controller/databaseFetcher/stage.ts";
import { Stage } from "../../model/db/stage.ts";
import { StageMessage } from "../../model/messages/stage.message.ts";

export const convertProjectToProjectMessage = async (projects: Project[]) => {
    let project: ProjectMessage = { projects: [] };
    for (const item of projects) {
        let stages = await getAllStagesFromProject(Number(item.id))
        let projectItem: ProjectMessageItem = {
            id: Number(item.id),
            name: String(item.name),
            stages: convertStages(stages)
        }

        project.projects.push(projectItem)

    }
    return project;

}

const convertStages = (stages: Stage[]): StageMessage[] => {
    let stageMessage: StageMessage[] = [];
    stages.forEach(item => {
        stageMessage.push({ id: Number(item.id), name: String(item.name), number: Number(item.number) })
    })

    return stageMessage;
}