import { DataTypes, Model } from "https://deno.land/x/denodb@v1.0.39/mod.ts";
import { User } from "./user.ts";
import { Paper } from "./paper.ts";


export class ReadingList extends Model {
	static table = 'readinglist';
	static timestamps = true;

	static fields = {
		id: { primaryKey: true, autoIncrement: true, type: DataTypes.INTEGER },
	}

	static user() {
		return this.hasOne(User);
	}


	static paper() {
		return this.hasOne(Paper);
	}


}

