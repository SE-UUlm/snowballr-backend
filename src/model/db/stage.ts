import { DataTypes, Model } from "https://deno.land/x/denodb@v1.0.39/mod.ts";
import { Project } from "./project.ts";
import { PaperScopeForStage } from "./paperScopeForStage.ts";
import { Review } from "./review.ts";


export class Stage extends Model {
	static table = 'stage';
	static timestamps = true;

	static fields = {
		id: { primaryKey: true, autoIncrement: true, type: DataTypes.INTEGER },
		name: DataTypes.STRING,
		number: DataTypes.INTEGER,

	}

	static project() {
		return this.hasOne(Project);
	}

	static inScopeFor() {
		return this.hasMany(PaperScopeForStage);
	}

	static review() {
		return this.hasMany(Review)
	}
}

// SELECT review.id FROM review JOIN stage ON review.stage_id = stage.id WHERE stage.project_id = 1


// SELECT review.id FROM review JOIN paper ON 

// 10.1109/models-c.2019.00063