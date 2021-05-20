import { assertNotEquals } from "https://deno.land/std@0.97.0/testing/asserts.ts";
import {hashPassword} from "../../helper/passwordHasher.ts";
import {createHash} from "https://deno.land/std@0.95.0/hash/mod.ts"

Deno.test({
    name: "testSalt",
    fn(): void | Promise<void> {
        let password = "test";

        const hash = createHash("sha3-512");
        hash.update(password)

        let hashedPassword = hashPassword(password)
        let hashedPassword2 = hash.toString();

        assertNotEquals(hashedPassword, hashedPassword2);
    }

})