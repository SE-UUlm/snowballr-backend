import {User} from "../../src/model/db/user.ts";
import {convertCtxBodyToUser, convertUserToUserProfile} from "../../src/helper/converter/userConverter.ts";
import { assertEquals } from "https://deno.land/std@0.97.0/testing/asserts.ts";
import {createMockApp} from "../mockObjects/oak/mockApp.test.ts";
import {createMockContext} from "../mockObjects/oak/mockContext.test.ts";
import {validateContentType} from "../../src/controller/validation.ts";
import {emptyAsyncFunctionTest} from "../mockObjects/emptyAsyncFunction.test.ts";

Deno.test({
    name: "testNoPasswordDelivery",
    fn(): void | Promise<void> {
        let user: User = new User();
        let firstName = "Martin"
        let lastName = "Tester"
        user.password = "124";
        user.firstName = firstName;
        user.lastName = lastName;

        let userProfile: any = convertUserToUserProfile(user);

        assertEquals(userProfile.password, undefined);
        assertEquals(userProfile.firstName, firstName);
        assertEquals(userProfile.lastName, lastName);
    }

})

Deno.test({
    name: "testConvertCtxBoxyToUser",
    async fn(): Promise<void> {
        let email = "martin.tester@test.de"
        let firstName = "Martin"
        let lastName = "Tester"
        let password = "124";
        let status = "registered"
        let isAdmin = true;

        let app = await createMockApp();
        let ctx = await createMockContext(app,`{"email": "${email}", "password":"${password}", "firstName":"${firstName}", "lastName": "${lastName}", "status": "${status}", "isAdmin": ${isAdmin}}`);
        let user = await convertCtxBodyToUser(ctx);

        assertEquals(user.email, email)
        assertEquals(user.firstName, firstName)
        assertEquals(user.lastName, lastName)
        assertEquals(user.password, password)
        assertEquals(user.status, status)
        assertEquals(user.isAdmin, isAdmin)

    }

})