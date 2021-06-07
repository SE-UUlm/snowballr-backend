import {DataTypes, Model} from "https://deno.land/x/denodb/mod.ts";
import {UserIsPartOfProject} from "./userIsPartOfProject.ts";


export class Project extends Model {
    static table = 'project';
    static timestamps = true;

    static fields = {
        id: {primaryKey: true, autoIncrement: true, type: DataTypes.INTEGER},
        name: DataTypes.STRING,
        minCountReviewers: {type: DataTypes.INTEGER, allowNull: true},
        countDecisiveReviewers: {type: DataTypes.INTEGER, allowNull: true},
        evaluationFormula: {type: DataTypes.STRING, allowNull: true},
    }

    static user() {
        return this.hasMany(UserIsPartOfProject);
    }
}

