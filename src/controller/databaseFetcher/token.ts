import { User } from "../../model/db/user.ts";
import { Token } from "../../model/db/token.ts";

export const insertToken = async (user: User, token: string) => {
  return Token.create({ token: token, userId: Number(user.id) }).catch(
    (error) => console.log(error),
  );
};

export const getToken = async (userId: number, token: string) => {
  const foundToken = await Token.where({ userId: userId, token: token }).get();
  if (Array.isArray(foundToken)) {
    return foundToken[0];
  }
  return undefined;
};

export const getTokens = async (userId: number) => {
  const foundToken = await Token.where({ userId: userId }).get();
  if (Array.isArray(foundToken)) {
    return foundToken;
  }
  return undefined;
};
