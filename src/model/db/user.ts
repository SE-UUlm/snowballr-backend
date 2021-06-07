import {DataTypes, Model} from "https://deno.land/x/denodb/mod.ts";
import {Token} from "./token.ts";
import {UserIsPartOfProject} from "./userIsPartOfProject.ts";

export class User extends Model {
    static table = 'user';
    static timestamps = true;

    static fields = {
        id: {primaryKey: true, autoIncrement: true, type: DataTypes.INTEGER},
        isAdmin: {type: DataTypes.BOOLEAN},
        password: {type: DataTypes.STRING, allowNull: true},
        lastName: {type: DataTypes.STRING, allowNull: true},
        firstName: DataTypes.STRING,
        eMail: {type: DataTypes.STRING},
        status: {type: DataTypes.STRING},
        loginBlock: {type: DataTypes.BOOLEAN}
    };

    static defaults = {
        isAdmin: false,
        loginBlock: false,
        status: "unregistered"
    }

    static token() {
        return this.hasMany(Token);
    }

    static project() {
        return this.hasMany(UserIsPartOfProject);
    }
}
