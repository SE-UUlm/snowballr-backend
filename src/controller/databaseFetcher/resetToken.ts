import { User } from "../../model/db/user.ts";
import { ResetToken } from "../../model/db/resetToken.ts";

export const insertResetToken = async (user: User, token: string) => {
  return ResetToken.create({ token: token, userId: Number(user.id) }).catch(
    (error) => console.log(error),
  );
};

export const getResetToken = async (userId: number, token: string) => {
  const foundResetToken = await ResetToken.where({
    userId: userId,
    token: token,
  }).get();
  if (Array.isArray(foundResetToken)) {
    return foundResetToken[0];
  }
  return undefined;
};

export const getResetTokens = async (userId: number) => {
  const foundResetToken = await ResetToken.where({ userId: userId }).get();
  if (Array.isArray(foundResetToken)) {
    return foundResetToken;
  }
  return undefined;
};
