import {DataTypes, Model} from "https://deno.land/x/denodb/mod.ts";
import {Wrote} from "./wrote.ts";


export class Author extends Model {
    static table = 'author';
    static timestamps = true;

    static fields = {
        id: {primaryKey: true, autoIncrement: true, type: DataTypes.INTEGER},
        raw: {type: DataTypes.STRING, allowNull: true},
        lastName: {type: DataTypes.STRING, allowNull: true},
        firstName: {type: DataTypes.STRING, allowNull: true},


    }

    static wrote() {
        return this.hasMany(Wrote);
    }


}


