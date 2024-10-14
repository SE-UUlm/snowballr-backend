import { DataTypes, Model } from "https://deno.land/x/denodb@v1.4.0/mod.ts";
import { UserIsPartOfProject } from "./userIsPartOfProject.ts";
import { SearchApi } from "./searchApi.ts";
import { Criteria } from "./criteria.ts";
import { Review } from "./review.ts";
import { Stage } from "./stage.ts";


export class Project extends Model {
	static table = 'project';
	static timestamps = true;

	static fields = {
		id: { primaryKey: true, autoIncrement: true, type: DataTypes.INTEGER },
		name: DataTypes.STRING,
		minCountReviewers: { type: DataTypes.INTEGER },
		countDecisiveReviewers: { type: DataTypes.INTEGER },
		combinationOfReviewers: DataTypes.STRING,
		type: DataTypes.STRING,
		evaluationFormula: { type: DataTypes.STRING },
		mergeThreshold: DataTypes.DECIMAL
	}

	static user() {
		return this.hasMany(UserIsPartOfProject);
	}

	static api() {
		return this.hasMany(SearchApi)
	}

	static criteria() {
		return this.hasMany(Criteria)
	}


	static stage() {
		return this.hasMany(Stage)
	}


	static review() {
		return this.hasMany(Review)
	}
}

