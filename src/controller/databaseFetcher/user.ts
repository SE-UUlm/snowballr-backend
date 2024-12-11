import { User } from "../../model/db/user.ts";
import { hashPassword } from "../../helper/passwordHasher.ts";

export const returnUserByEmailAndPassword = async (
  eMail: string,
  password: string,
): Promise<User | undefined> => {
  const user = await User.where({
    eMail: eMail,
    password: hashPassword(password),
  }).get();
  if (Array.isArray(user)) {
    return user[0];
  }
  return undefined;
};

export const returnUserByEmail = async (
  eMail: string,
): Promise<User | undefined> => {
  const user = await User.where({ eMail: eMail }).get();

  if (Array.isArray(user)) {
    return user[0];
  }
  return undefined;
};

export const removeUser = async (id: any) => {
  return await User.deleteById(id);
};

export const insertUser = async (
  eMail: string,
  password: string,
  isAdmin: boolean,
  firstName: string,
  lastName: string,
  status: string,
): Promise<User> => {
  return User.create({
    eMail: eMail,
    password: hashPassword(password),
    isAdmin: isAdmin,
    firstName: firstName,
    lastName: lastName,
    status: status,
  });
};

export const insertUserForRegistration = async (
  eMail: string,
): Promise<User> => {
  return User.create({ eMail: eMail });
};
