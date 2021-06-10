import {DataTypes, Model} from "https://deno.land/x/denodb/mod.ts";
import {Review} from "./review.ts";
import {ReadingList} from "./readingList.ts";
import {PaperScopeForStage} from "./paperScopeForStage.ts";
import {Wrote} from "./wrote.ts";
import {ReferencedBy} from "./referencedBy.ts";
import {CitedBy} from "./citedBy.ts";


export class Paper extends Model {
    static table = 'paper';
    static timestamps = true;

    static fields = {
        id: {primaryKey: true, autoIncrement: true, type: DataTypes.INTEGER},
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


    static citedBy() {
        return this.hasMany(CitedBy)
    }

    static referencedBy() {
        return this.hasMany(ReferencedBy)
    }

    static inScopeFor() {
        return this.hasMany(PaperScopeForStage);
    }
}



