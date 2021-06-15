import {DataTypes, Model} from "https://deno.land/x/denodb/mod.ts";
import {AuthorHasID} from "./authorHasID.ts";


export class AuthorID extends Model {
    static table = 'userid';
    static timestamps = true;

    static fields = {
        id: {primaryKey: true, autoIncrement: true, type: DataTypes.INTEGER},
        type: DataTypes.STRING,
        value: DataTypes.STRING

    }

    static id() {
        return this.hasMany(AuthorHasID);
    }


}

