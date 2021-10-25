import { DataTypes, Model } from "https://deno.land/x/denodb/mod.ts";
import { Review } from "./review.ts";
import { ReadingList } from "./readingList.ts";
import { PaperScopeForStage } from "./paperScopeForStage.ts";
import { Wrote } from "./wrote.ts";
import { PaperHasID } from "./paperHasID.ts";
import { Pdf } from "./pdf.ts";


export class Paper extends Model {
    static table = 'paper';
    static timestamps = true;

    static fields = {
        // !!!!!! ID has to stay as first value !!!!!
        id: { primaryKey: true, autoIncrement: true, type: DataTypes.INTEGER },
        doi: { type: DataTypes.STRING, allowNull: true },
        title: { type: DataTypes.STRING, allowNull: true, length: 1024 },
        abstract: { type: DataTypes.STRING, allowNull: true, length: 20480 },
        year: { type: DataTypes.INTEGER, allowNull: true },
        publisher: { type: DataTypes.STRING, allowNull: true, length: 1024 },
        type: { type: DataTypes.STRING, allowNull: true, length: 1024 },
        scope: { type: DataTypes.STRING, allowNull: true, length: 1024 },
        scopeName: { type: DataTypes.STRING, allowNull: true, length: 1024 },


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

    static paperid() {
        return this.hasMany(PaperHasID);
    }

    static pdf() {
        return this.hasMany(Pdf)
    }
}



