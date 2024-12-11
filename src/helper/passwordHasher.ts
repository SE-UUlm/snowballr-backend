import { createHash } from "https://deno.land/std@0.150.0/hash/mod.ts";

/**
 * Adds a salt to a users password and hashes it afterwards
 * @param password
 */
export const hashPassword = (password: string) => {
  const passwordWithSalt = Deno.env.get("SALT") + password;
  const hash = createHash("sha3-512");
  hash.update(passwordWithSalt);
  return hash.toString();
};
