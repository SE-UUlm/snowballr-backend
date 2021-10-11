import {DataTypes, Model} from "https://deno.land/x/denodb/mod.ts";
import {ProjectUsesApi} from "./projectUsesApi.ts";


export class SearchApi extends Model {
    static table = 'searchapi';
    static timestamps = true;

    static fields = {
        id: {primaryKey: true, autoIncrement: true, type: DataTypes.INTEGER},
        name: DataTypes.STRING,
        credentials: {type: DataTypes.STRING, allowNull: true},
    }

    static project() {
        return this.hasMany(ProjectUsesApi);
    }
}

