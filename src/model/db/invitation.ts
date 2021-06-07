import {DataTypes, Model} from "https://deno.land/x/denodb/mod.ts";
import {User} from "./user.ts";


export class Invitation extends Model {
    static table = 'invitation';
    static timestamps = true;

    static fields = {
        id: {primaryKey: true, autoIncrement: true, type: DataTypes.INTEGER},
        token: {type: DataTypes.STRING},
        validUntil: {type: DataTypes.DATE, allowNull: false},
    }

    static user() {
        return this.hasOne(User);
    }
}

