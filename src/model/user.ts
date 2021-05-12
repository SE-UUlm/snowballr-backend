import { DataTypes, Model } from "https://deno.land/x/denodb/mod.ts";

export class User extends Model {
    static table = 'user';
    static timestamps = true;

    static fields = {
        id:{primaryKey: true, autoIncrement: true},
        fullyRegistered: {type:DataTypes.BOOLEAN,allowNull: false},
        isAdmin: {type: DataTypes.BOOLEAN, allowNull: false},
        password: {type: DataTypes.STRING, allowNull: false},
        lastName: DataTypes.STRING,
        firstName: DataTypes.STRING,
        eMail: {type: DataTypes.STRING, allowNull: false}
    };

    static defaults = {
        isAdmin: false,
        fullyRegistered: false
    }
}
