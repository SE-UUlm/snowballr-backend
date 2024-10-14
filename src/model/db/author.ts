import { DataTypes, Model } from "https://deno.land/x/denodb@v1.4.0/mod.ts";
import { Wrote } from "./wrote.ts";
import { AuthorHasID } from "./authorHasID.ts";


export class Author extends Model {
	static table = 'author';
	static timestamps = true;

	static fields = {
		id: { primaryKey: true, autoIncrement: true, type: DataTypes.INTEGER },
		rawString: { type: DataTypes.STRING, allowNull: true },
		orcid: { type: DataTypes.STRING, allowNull: true },
		lastName: { type: DataTypes.STRING, allowNull: true },
		firstName: { type: DataTypes.STRING, allowNull: true },


	}

	static wrote() {
		return this.hasMany(Wrote);
	}

	static id() {
		return this.hasMany(AuthorHasID);
	}
}


