import {DataTypes, Model} from "https://deno.land/x/denodb/mod.ts";
import {Paper} from "./paper.ts";


export class ReferencedBy extends Model {
    static table = 'referencedby';
    static timestamps = true;

    static fields = {
        id: {primaryKey: true, autoIncrement: true, type: DataTypes.INTEGER},

    }


    static paperReferenced() {
        return this.hasOne(Paper);
    }

    static paperReferencing() {
        return this.hasOne(Paper);
    }
}

