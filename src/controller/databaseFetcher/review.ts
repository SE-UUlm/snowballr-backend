import { CriteriaEvaluation } from "../../model/db/criteriaEval.ts";
import { PaperScopeForStage } from "../../model/db/paperScopeForStage.ts";
import { Review } from "../../model/db/review.ts";
import { ReviewMessage } from "../../model/messages/review.message.ts";

export const getAllReviewsFromProjectPaper = async (ppId: number): Promise<ReviewMessage[]> => {

    let reviews = await Review.where(Review.field("paper_id"), ppId).get()
    return getReviews(reviews)
}

export const getReview = async (reviewId: number): Promise<ReviewMessage[]> => {
    let review = await Review.find(reviewId)
    return getReviews([review])
}

export const getReviews = async (reviews: Review | Review[]) => {
    if (Array.isArray(reviews)) {
        let arr = new Array<ReviewMessage>();
        for (let review of reviews) {
            let message: ReviewMessage = { id: Number(review.id), criteriaEvaluations: [], finished: Boolean(review.finished) }
            if (review.overallEvaluation) { message.overallEvaluation = String(review.overallEvaluation) }
            if (review.finishDate) { message.finishDate = new Date(String(review.finishDate)) }
            let ces = await CriteriaEvaluation.where(CriteriaEvaluation.field("review_id"), Number(review.id)).get()

            if (Array.isArray(ces)) {
                for (let ce of ces) {
                    message.criteriaEvaluations.push(ce)
                }
            }
            arr.push(message)
        }
        return arr;
    }
    return new Array<ReviewMessage>();
}