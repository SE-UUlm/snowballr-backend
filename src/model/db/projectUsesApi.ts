import { DataTypes, Model } from "https://deno.land/x/denodb@v1.4.0/mod.ts";
import { SearchApi } from "./searchApi.ts";
import { Project } from "./project.ts";

export class ProjectUsesApi extends Model {
  static table = "projectusesapi";
  static timestamps = true;

  static fields = {
    id: { primaryKey: true, autoIncrement: true, type: DataTypes.INTEGER },
    inUse: DataTypes.BOOLEAN,
  };

  static api() {
    return this.hasOne(SearchApi);
  }

  static project() {
    return this.hasOne(Project);
  }
}
