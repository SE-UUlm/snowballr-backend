import {DataTypes, Model} from "https://deno.land/x/denodb/mod.ts";
import {User} from "./user.ts";

export class Token extends Model {
    static table = 'token';
    static timestamps = true;

    static fields = {
        id: {primaryKey: true, autoIncrement: true, type: DataTypes.INTEGER},
        token: DataTypes.STRING
    }

    static user() {
        return this.hasOne(User);
    }
}
