import { DataTypes, Model } from "https://deno.land/x/denodb@v1.4.0/mod.ts";
import { Review } from "./review.ts";
import { PaperScopeForStage } from "./paperScopeForStage.ts";


export class ReviewToPaperScope extends Model {
	static table = 'reviewtopaperscope';
	static timestamps = true;

	static fields = {
		id: { primaryKey: true, autoIncrement: true, type: DataTypes.INTEGER },
	}


	static review() {
		return this.hasOne(Review);
	}


	static paperScope() {
		return this.hasOne(PaperScopeForStage);
	}
}

