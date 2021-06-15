import {DataTypes, Model} from "https://deno.land/x/denodb/mod.ts";
import {CriteriaEvaluation} from "./criteriaEval.ts";
import {User} from "./user.ts";
import {Stage} from "./stage.ts";
import {Paper} from "./paper.ts";


export class Review extends Model {
    static table = 'review';
    static timestamps = true;

    static fields = {
        id: {primaryKey: true, autoIncrement: true, type: DataTypes.INTEGER},
        finished: DataTypes.BOOLEAN,
        overallEvaluation: DataTypes.STRING,
        finishDate: DataTypes.DATE

    }

    static criteriaEvaluation() {
        return this.hasMany(CriteriaEvaluation);
    }

    static user() {
        return this.hasOne(User)
    }

    static stage() {
        return this.hasOne(Stage)
    }


    static paper() {
        return this.hasOne(Paper)
    }
}

