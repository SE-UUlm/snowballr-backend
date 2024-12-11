import { DataTypes, Model } from "https://deno.land/x/denodb@v1.4.0/mod.ts";
import { AuthorID } from "./authorID.ts";

export class AuthorHasID extends Model {
  static table = "userhasid";
  static timestamps = true;

  static fields = {
    id: { primaryKey: true, autoIncrement: true, type: DataTypes.INTEGER },
  };

  static defaults = {
    isOwner: false,
  };

  static Author() {
    return this.hasOne(AuthorID);
  }

  static id() {
    return this.hasOne(AuthorID);
  }
}
