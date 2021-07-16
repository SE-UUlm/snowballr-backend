import {DataTypes, Model} from "https://deno.land/x/denodb/mod.ts";
import {Stage} from "./stage.ts";
import {Paper} from "./paper.ts";


export class PaperScopeForStage extends Model {
    static table = 'inscopefor';
    static timestamps = true;

    static fields = {
        id: {primaryKey: true, autoIncrement: true, type: DataTypes.INTEGER},
        additionDate: DataTypes.DATE
    }

    static stage() {
        return this.hasOne(Stage);
    }


    static paper() {
        return this.hasOne(Paper);
    }


}

