import {User} from "../../src/model/user.ts";
import {convertUserToUserProfile} from "../../src/helper/userConverter.ts";
import { assertEquals } from "https://deno.land/std@0.97.0/testing/asserts.ts";

Deno.test({
    name: "testNoPasswordDelivery",
    fn(): void | Promise<void> {
        let user: User = new User();
        let firstName = "Martin"
        user.password = "124";
        user.firstName = firstName;

        let userProfile: any = convertUserToUserProfile(user);

        assertEquals(undefined, userProfile.password);
        assertEquals(firstName, userProfile.firstName);
    }

})