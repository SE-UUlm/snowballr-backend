import { DataTypes, Model } from "https://deno.land/x/denodb/mod.ts";
import {db} from "../controller/database.ts"
import {createTerminationDate} from "../helper/dateHelper.ts";
import {User} from "./user.ts";
import { Relationships } from 'https://deno.land/x/denodb/mod.ts';

export class Invitation extends Model {
    static table = 'invitation';
    static timestamps = true;

    static fields = {
        token: { primaryKey: true, type: DataTypes.STRING},
        validUntil: {type: DataTypes.DATE, allowNull: false},
    }

    static user() {
        return this.hasOne(User);
    }

    static defaults = {
        validUntil: createTerminationDate()
    }
}
Relationships.belongsTo(Invitation, User);

