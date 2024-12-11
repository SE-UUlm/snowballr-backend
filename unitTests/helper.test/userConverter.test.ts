import { User } from "../../src/model/db/user.ts";
import { convertCtxBodyToUser, convertUserToUserProfile } from "../../src/helper/converter/userConverter.ts";
import { assertEquals } from "https://deno.land/std@0.150.0/testing/asserts.ts";
import { createMockApp } from "../mockObjects/oak/mockApp.test.ts";
import { createMockContext } from "../mockObjects/oak/mockContext.test.ts";

Deno.test({
    name: "testNoPasswordDelivery",
    fn(): void | Promise<void> {
        const user: User = new User();
        const firstName = "Martin"
        const lastName = "Tester"
        user.password = "124";
        user.firstName = firstName;
        user.lastName = lastName;

        const userProfile: any = convertUserToUserProfile(user);

        assertEquals(userProfile.password, undefined);
        assertEquals(userProfile.firstName, firstName);
        assertEquals(userProfile.lastName, lastName);
    }

})

Deno.test({
    name: "testConvertCtxBoxyToUser",
    async fn(): Promise<void> {
        const email = "martin.tester@test.de"
        const firstName = "Martin"
        const lastName = "Tester"
        const password = "124";
        const status = "registered"
        const isAdmin = true;

        const app = await createMockApp();
        const ctx = await createMockContext(app, `{"email": "${email}", "password":"${password}", "firstName":"${firstName}", "lastName": "${lastName}", "status": "${status}", "isAdmin": ${isAdmin}}`);
        const user = await convertCtxBodyToUser(ctx);

        assertEquals(user.email, email)
        assertEquals(user.firstName, firstName)
        assertEquals(user.lastName, lastName)
        assertEquals(user.password, password)
        assertEquals(user.status, status)
        assertEquals(user.isAdmin, isAdmin)

    }

})
