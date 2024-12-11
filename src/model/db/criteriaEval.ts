import { DataTypes, Model } from "https://deno.land/x/denodb@v1.4.0/mod.ts";
import { Criteria } from "./criteria.ts";
import { Review } from "./review.ts";

export class CriteriaEvaluation extends Model {
  static table = "criteria-evaluation";
  static timestamps = true;

  static fields = {
    id: { primaryKey: true, autoIncrement: true, type: DataTypes.INTEGER },
    value: DataTypes.STRING,
  };

  static criteria() {
    return this.hasOne(Criteria);
  }

  static review() {
    return this.hasOne(Review);
  }
}
