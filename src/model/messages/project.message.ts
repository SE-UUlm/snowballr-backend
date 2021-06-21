import {StageMessage} from "./stage.message.ts";

export interface ProjectMessage {
    projects: ProjectMessageItem[]
}

export interface ProjectMessageItem {
    id: number,
    name: string,
    stages: StageMessage[]
}