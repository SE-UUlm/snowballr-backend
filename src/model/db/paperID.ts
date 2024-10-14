import { DataTypes, Model } from "https://deno.land/x/denodb@v1.4.0/mod.ts";


export class PaperID extends Model {
	static table = 'paperid';
	static timestamps = true;

	static fields = {
		id: { primaryKey: true, autoIncrement: true, type: DataTypes.INTEGER },
		type: DataTypes.STRING,
		value: DataTypes.STRING

	}

	static paper() {
		return this.hasMany(PaperID);
	}


}

