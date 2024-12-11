import { DataTypes, Model } from "https://deno.land/x/denodb@v1.4.0/mod.ts";
import { Token } from "./token.ts";
import { UserIsPartOfProject } from "./userIsPartOfProject.ts";
import { Review } from "./review.ts";
import { ReadingList } from "./readingList.ts";

export class User extends Model {
  static table = "user";
  static timestamps = true;

  static fields = {
    id: { primaryKey: true, autoIncrement: true, type: DataTypes.INTEGER },
    isAdmin: { type: DataTypes.BOOLEAN },
    password: { type: DataTypes.STRING, allowNull: true },
    lastName: { type: DataTypes.STRING, allowNull: true },
    firstName: DataTypes.STRING,
    eMail: { type: DataTypes.STRING, unique: true },
    status: { type: DataTypes.STRING },
    loginBlock: { type: DataTypes.BOOLEAN },
  };

  static defaults = {
    isAdmin: false,
    loginBlock: false,
    status: "not-fully-registered",
  };

  static token() {
    return this.hasMany(Token);
  }

  static project() {
    return this.hasMany(UserIsPartOfProject);
  }

  static reviews() {
    return this.hasMany(Review);
  }

  static readingList() {
    return this.hasMany(ReadingList);
  }
}
