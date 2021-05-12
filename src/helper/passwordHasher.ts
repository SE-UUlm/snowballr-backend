import {createHash} from "https://deno.land/std@0.95.0/hash/mod.ts";

export const hashPassword = (password: string) => {
    let passwordWithSalt = Deno.env.get("SALT") + password;
    const hash = createHash("sha3-512");
    hash.update(passwordWithSalt);
    return hash.toString();
}