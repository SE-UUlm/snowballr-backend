import { assertNotEquals } from "https://deno.land/std@0.150.0/testing/asserts.ts";
import { createHash } from "https://deno.land/std@0.150.0/hash/mod.ts";
import { hashPassword } from "../../src/helper/passwordHasher.ts";


Deno.test({
    name: "testSalt",
    fn(): void | Promise<void> {
        const password = "test";

        const hash = createHash("sha3-512");
        hash.update(password)

        const hashedPassword = hashPassword(password)
        const hashedPassword2 = hash.toString();

        assertNotEquals(hashedPassword, hashedPassword2);
    }

})
