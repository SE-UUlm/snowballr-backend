import { DataTypes, Model } from "https://deno.land/x/denodb@v1.4.0/mod.ts";
import { CriteriaEvaluation } from "./criteriaEval.ts";
import { User } from "./user.ts";
import { Stage } from "./stage.ts";
import { Paper } from "./paper.ts";
import { PaperScopeForStage } from "./paperScopeForStage.ts";
import { ReviewToPaperScope } from "./reviewToPaperScope.ts";


export class Review extends Model {
	static table = 'review';
	static timestamps = true;

	static fields = {
		id: { primaryKey: true, autoIncrement: true, type: DataTypes.INTEGER },
		finished: { type: DataTypes.BOOLEAN },
		overallEvaluation: { type: DataTypes.STRING, allowNull: true },
		finishDate: { type: DataTypes.DATE, allowNull: true },
		comment: { type: DataTypes.STRING, allowNull: true },

	}
	static defaults = {
		finished: false,
	}

	static criteriaEvaluation() {
		return this.hasMany(CriteriaEvaluation);
	}

	static user() {
		return this.hasOne(User)
	}

	// static stage() {
	// 	return this.hasOne(Stage)
	// }

	static reviewToPaperScope() {
		return this.hasMany(ReviewToPaperScope)
	}
}

