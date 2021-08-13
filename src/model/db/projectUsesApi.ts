import { DataTypes, Model } from "https://deno.land/x/denodb/mod.ts";
import { SearchApi } from "./searchApi.ts";
import { Project } from "./project.ts";


export class ProjectUsesApi extends Model {
    static table = 'projectusesapi';
    static timestamps = true;

    static fields = {
        id: { primaryKey: true, autoIncrement: true, type: DataTypes.INTEGER },
    }

    static api() {
        return this.hasOne(SearchApi);
    }

    static project() {
        return this.hasOne(Project)
    }
}

