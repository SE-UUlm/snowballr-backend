import {DataTypes, Model} from "https://deno.land/x/denodb/mod.ts";
import {User} from "./user.ts";
import {Project} from "./project.ts";


export class UserIsPartOfProject extends Model {
    static table = 'ispartof';
    static timestamps = true;

    static fields = {
        id: {primaryKey: true, autoIncrement: true, type: DataTypes.INTEGER},
        isOwner: DataTypes.BOOLEAN,

    }

    static defaults = {
        isOwner: false,
    }

    static user() {
        return this.hasMany(User);
    }

    static project() {
        return this.hasMany(Project);
    }
}

