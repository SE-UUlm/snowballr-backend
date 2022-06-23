import { DataTypes, Model } from "https://deno.land/x/denodb@v1.0.39/mod.ts";
import { Wrote } from "./wrote.ts";
import { AuthorHasID } from "./authorHasID.ts";
import { Paper } from "./paper.ts";


export class Pdf extends Model {
	static table = 'pdf';
	static timestamps = true;

	static fields = {
		id: { primaryKey: true, autoIncrement: true, type: DataTypes.INTEGER },
		url: { type: DataTypes.STRING, allowNull: true, length: 1024 },
	}

	static paper() {
		return this.hasOne(Paper);
	}
}
