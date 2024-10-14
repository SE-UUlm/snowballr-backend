import { DataTypes, Model } from "https://deno.land/x/denodb@v1.4.0/mod.ts";
import { User } from "./user.ts";


export class Invitation extends Model {
	static table = 'invitation';
	static timestamps = true;

	static fields = {
		id: { primaryKey: true, autoIncrement: true, type: DataTypes.INTEGER },
		token: { type: DataTypes.STRING, length: 512 },
	}

	static user() {
		return this.hasOne(User);
	}
}

