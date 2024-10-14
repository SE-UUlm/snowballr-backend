import { DataTypes, Model } from "https://deno.land/x/denodb@v1.4.0/mod.ts";
import { Paper } from "./paper.ts";
import { PaperID } from "./paperID.ts";


export class PaperHasID extends Model {
	static table = 'paperhasid';
	static timestamps = true;

	static fields = {
		id: { primaryKey: true, autoIncrement: true, type: DataTypes.INTEGER },
	}

	static defaults = {
		isOwner: false,
	}

	static paper() {
		return this.hasOne(Paper);
	}

	static id() {
		return this.hasOne(PaperID);
	}
}

