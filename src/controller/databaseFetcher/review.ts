import { CriteriaEvaluation } from "../../model/db/criteriaEval.ts";
import { PaperScopeForStage } from "../../model/db/paperScopeForStage.ts";
import { Review } from "../../model/db/review.ts";
import { ReviewToPaperScope } from "../../model/db/reviewToPaperScope.ts";
import { ReviewMessage } from "../../model/messages/review.message.ts";

export const getAllReviewsFromProjectPaper = async (ppId: number): Promise<ReviewMessage[]> => {
	// let reviews = await Review.where(Review.field("paperscopeforstage_id"), ppId).get()
	console.log("HEREREREREREEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEE")
	let reviews: Review[] = [];
	try {
		let scope = await ReviewToPaperScope.where({ paperscopeforstageId: ppId }).get();
		console.log(JSON.stringify(scope, null, 2))
		if (Array.isArray(scope)) {
			for (let s of scope) {
				reviews.push(await Review.find(Number(s.reviewId)));
			}
		}
	} catch (err) {
		console.log(err)
	}
	return getReviews(reviews);
}

export const checkUserReviewOfProjectPaper = async (ppId: number, userId: number): Promise<boolean> => {

	//let reviews = await Review.where({ paperscopeforstageId: ppId, userId: userId }).get()
	let reviews: Review[] = [];
	try {
		let scope = await ReviewToPaperScope.where({ paperscopeforstageId: ppId }).get();

		if (Array.isArray(scope)) {
			for (let s of scope) {
				reviews.push(((await Review.where({ id: Number(s.reviewId), userId: userId }).get()) as Review[])[0]);
			}
		}
	} catch (err) {
		console.log(err)
	}
	if (Array.isArray(reviews) && reviews[0]) {
		return true;
	}
	return false;
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
			if (review.userId) { message.userId = Number(review.userId) }
			message = await getCriteriaEvalsOfCriteria(Number(review.id), message)
			arr.push(message)
		}
		return arr;
	}
	return new Array<ReviewMessage>();
}

const getCriteriaEvalsOfCriteria = async (reviewID: number, message: ReviewMessage) => {
	let ces = await CriteriaEvaluation.where(CriteriaEvaluation.field("review_id"), reviewID).get()

	if (Array.isArray(ces)) {
		for (let ce of ces) {
			message.criteriaEvaluations.push(ce)
		}
	}
	return message;
}