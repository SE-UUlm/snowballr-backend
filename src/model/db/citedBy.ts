import {DataTypes, Model} from "https://deno.land/x/denodb/mod.ts";
import {Paper} from "./paper.ts";


export class CitedBy extends Model {
    static table = 'citedby';
    static timestamps = true;

    static fields = {
        id: {primaryKey: true, autoIncrement: true, type: DataTypes.INTEGER},

    }

    static paperCited() {
        return this.hasOne(Paper);
    }

    static paperCiting() {
        return this.hasOne(Paper);
    }


}

