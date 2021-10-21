import { CriteriaEvaluation } from "../db/criteriaEval.ts";

export interface ReviewMessage {
    id: number,
    finished: boolean,
    overallEvaluation?: string,
    finishDate?: Date,
    userId?: number,
    criteriaEvaluations: CriteriaEvaluation[]
}
