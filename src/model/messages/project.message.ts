import { StageMessage } from "./stage.message.ts";

export interface ProjectMessage {
  projects: ProjectMessageItem[];
}

export interface ProjectMessageItem {
  id: number;
  name: string;
  minCountReviewers: number;
  countDecisiveReviewers: number;
  combinationOfReviewers: string;
  type: string;
  evaluationFormula: string;
  mergeThreshold: number;
  stages: StageMessage[];
}
