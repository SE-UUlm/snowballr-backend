import {DataTypes, Model} from "https://deno.land/x/denodb/mod.ts";
import {Review} from "./review.ts";
import {ReadingList} from "./readingList.ts";
import {PaperScopeForStage} from "./paperScopeForStage.ts";
import {Wrote} from "./wrote.ts";
import {PaperHasID} from "./paperHasID.ts";


export class Paper extends Model {
    static table = 'paper';
    static timestamps = true;

    static fields = {
        id: {primaryKey: true, autoIncrement: true, type: DataTypes.INTEGER},
        doi: {type: DataTypes.STRING, allowNull: true},
        title: {type: DataTypes.STRING, allowNull: true},
        abstract: {type: DataTypes.STRING, allowNull: true},
        year: {type: DataTypes.DATE, allowNull: true},
        publisher: {type: DataTypes.STRING, allowNull: true},
        type: {type: DataTypes.STRING, allowNull: true},
        scope: {type: DataTypes.STRING, allowNull: true},
        scopeName: {type: DataTypes.STRING, allowNull: true},

    }

    static review() {
        return this.hasMany(Review);
    }

    static wrote() {
        return this.hasMany(Wrote);
    }

    static readingList() {
        return this.hasMany(ReadingList)
    }

    static inScopeFor() {
        return this.hasMany(PaperScopeForStage);
    }

    static paper() {
        return this.hasMany(PaperHasID);
    }
}



