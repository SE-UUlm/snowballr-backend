import {DataTypes, Model} from "https://deno.land/x/denodb/mod.ts";
import {Project} from "./project.ts";
import {PaperScopeForStage} from "./paperScopeForStage.ts";
import {Review} from "./review.ts";


export class Stage extends Model {
    static table = 'stage';
    static timestamps = true;

    static fields = {
        id: {primaryKey: true, autoIncrement: true, type: DataTypes.INTEGER},
        name: DataTypes.STRING,
        number: DataTypes.INTEGER,

    }

    static project() {
        return this.hasOne(Project);
    }

    static inScopeFor() {
        return this.hasMany(PaperScopeForStage);
    }

    static review() {
        return this.hasMany(Review)
    }
}

