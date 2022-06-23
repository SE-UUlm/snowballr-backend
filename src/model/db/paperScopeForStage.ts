import { DataTypes, Model } from "https://deno.land/x/denodb/mod.ts";
import { Stage } from "./stage.ts";
import { Paper } from "./paper.ts";
import { Review } from "./review.ts";


export class PaperScopeForStage extends Model {
	static table = 'inscopefor';
	static timestamps = true;

	static fields = {
		id: { primaryKey: true, autoIncrement: true, type: DataTypes.INTEGER },
		additionDate: DataTypes.DATE,
		finalDecision: { type: DataTypes.STRING, allowNull: true },
	}

	static stage() {
		return this.hasOne(Stage);
	}


	static paper() {
		return this.hasOne(Paper);
	}

	static review() {
		return this.hasOne(Review);
	}


}

