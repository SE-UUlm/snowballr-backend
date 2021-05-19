import {DataTypes, Model} from "https://deno.land/x/denodb/mod.ts";

export class User extends Model {
    static table = 'user';
    static timestamps = true;

    static fields = {
        id: {primaryKey: true, autoIncrement: true, type: DataTypes.INTEGER},
        isAdmin: {type: DataTypes.BOOLEAN, allowNull: false},
        password: {type: DataTypes.STRING, allowNull: false},
        lastName: DataTypes.STRING,
        firstName: DataTypes.STRING,
        eMail: {type: DataTypes.STRING, allowNull: false},
        status: {type: DataTypes.STRING, allowNull: false},
        loginBlock: {type: DataTypes.BOOLEAN, allowNull: false}
    };

    static defaults = {
        isAdmin: false,
        loginBlock: false,
        status: "unregistered"
    }
}
