import { Project } from "../../model/db/project.ts";
import {
  ProjectMessage,
  ProjectMessageItem,
} from "../../model/messages/project.message.ts";
import { getAllStagesFromProject } from "../../controller/databaseFetcher/stage.ts";
import { Stage } from "../../model/db/stage.ts";
import { StageMessage } from "../../model/messages/stage.message.ts";

export const convertProjectToProjectMessage = async (
  projects: Project[],
): Promise<ProjectMessage> => {
  const project: ProjectMessage = { projects: [] };
  for (const item of projects) {
    console.log(item);
    const projectItem: ProjectMessageItem = await makeProjectMessage(item);
    project.projects.push(projectItem);
  }
  return project;
};

export const makeProjectMessage = async (
  project: ProjectMessageItem,
): Promise<ProjectMessageItem> => {
  const stages = await getAllStagesFromProject(Number(project.id));
  return {
    id: Number(project.id),
    name: String(project.name),
    minCountReviewers: Number(project.minCountReviewers),
    countDecisiveReviewers: Number(project.countDecisiveReviewers),
    combinationOfReviewers: String(project.combinationOfReviewers),
    type: String(project.type),
    evaluationFormula: String(project.evaluationFormula),
    mergeThreshold: Number(project.mergeThreshold),
    stages: convertStages(stages),
  };
};

const convertStages = (stages: Stage[]): StageMessage[] => {
  const stageMessage: StageMessage[] = [];
  stages.forEach((item) => {
    stageMessage.push({
      id: Number(item.id),
      name: String(item.name),
      number: Number(item.number),
    });
  });

  return stageMessage;
};
