import {DataTypes, Model} from "https://deno.land/x/denodb/mod.ts";
import {Project} from "./project.ts";
import {CriteriaEvaluation} from "./criteriaEval.ts";


export class Criteria extends Model {
    static table = 'criteria';
    static timestamps = true;

    static fields = {
        id: {primaryKey: true, autoIncrement: true, type: DataTypes.INTEGER},
        short: DataTypes.STRING,
        description: DataTypes.STRING,
        abbreviation: DataTypes.STRING,
        inclusionExclusion: DataTypes.STRING,
        Weight: DataTypes.STRING

    }

    static project() {
        return this.hasOne(Project);
    }

    static criterias() {
        return this.hasMany(CriteriaEvaluation)
    }
}
