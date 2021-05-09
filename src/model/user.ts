import { DataTypes, Model } from "https://deno.land/x/denodb/mod.ts";
import {db} from "../controller/database.ts"

export class User extends Model {
    static table = 'user';
    static timestamps = true;

    static fields = {
        fullyRegistered: DataTypes.BOOLEAN,
        isAdmin: DataTypes.BOOLEAN,
        password: DataTypes.STRING,
        lastName: DataTypes.STRING,
        firstName: DataTypes.STRING,
        eMail:
            {type: DataTypes.STRING,
                primaryKey: true,
            }

    };

    static defaults = {
        isAdmin: false,
        fullyRegistered: false
    }
}

db.link([User]);
