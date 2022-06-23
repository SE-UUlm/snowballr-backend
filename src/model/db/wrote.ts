import { DataTypes, Model } from "https://deno.land/x/denodb@v1.0.39/mod.ts";
import { Paper } from "./paper.ts";
import { Author } from "./author.ts";


export class Wrote extends Model {
	static table = 'wrote';
	static timestamps = true;

	static fields = {
		id: { primaryKey: true, autoIncrement: true, type: DataTypes.INTEGER },
	}


	static author() {
		return this.hasOne(Author);
	}


	static paper() {
		return this.hasOne(Paper);
	}
}

