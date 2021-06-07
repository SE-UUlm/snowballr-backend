import {setup} from "../../src/helper/setup.ts";
import {insertUser} from "../../src/controller/databaseFetcher/user.ts";
import {createMockApp} from "../mockObjects/oak/mockApp.test.ts";
import {createJWT} from "../../src/controller/validation.ts";
import {createMockContext} from "../mockObjects/oak/mockContext.test.ts";
import {assertEquals, assertNotEquals} from "https://deno.land/std@0.97.0/testing/asserts.ts"
import {createUser, getUsers, patchUser} from "../../src/controller/user.ts";
import {User} from "../../src/model/db/user.ts";
import {MockEmailClient} from "../mockObjects/mockEmailClient.test.ts";
import {getTokens} from "../../src/controller/databaseFetcher/token.ts";

Deno.test({
    name: "insertUserForCreation",
    async fn(): Promise<void> {
        await setup(true);
        let user = await insertUser("test@test", "ash", true, "Test", "Tester", "registered");

        let app = await createMockApp();
        let token = await createJWT(user)
        let ctx = await createMockContext(app, `{"email": "andreas.decker@uni-ulm.de"}`, [["Content-Type", "application/json"]], "/", token);
        await createUser(ctx, new MockEmailClient())

        assertEquals(ctx.response.status, 201)
    },
    sanitizeResources: false,
})

Deno.test({
    name: "insertUserNoEmail",
    async fn(): Promise<void> {
        await setup(true);
        let user = await insertUser("test@test", "ash", true, "Test", "Tester", "registered");

        let app = await createMockApp();
        let token = await createJWT(user)
        let ctx = await createMockContext(app, `{}`, [["Content-Type", "application/json"]], "/", token);
        await createUser(ctx, new MockEmailClient())

        assertEquals(ctx.response.status, 422)
    },
    sanitizeResources: false,
})

Deno.test({
    name: "getAllUsers",
    async fn(): Promise<void> {
        await setup(true);
        let user = await insertUser("test@test", "ash", true, "Test", "Tester", "registered");
        let user2 = await insertUser("test@test", "ash", true, "Test", "Tester", "registered");
        let app = await createMockApp();
        let token = await createJWT(user)
        let ctx = await createMockContext(app, undefined, [["Content-Type", "application/json"]], "/", token);
        assertEquals(true, await getUsers(ctx));


    },
    sanitizeResources: false,
})

Deno.test({
    name: "PatchUserAdmin",
    async fn(): Promise<void> {
        await setup(true);
        let user = await insertUser("test@test", "ash", true, "Test", "Tester", "registered");
        let userToChange = await insertUser("theo@eo", "leo", false, "Theo", "Eo", "unregistered");
        let email = String(userToChange.eMail);
        let password = String(userToChange.password);
        let firstName = String(userToChange.firstName);
        let lastName = String(userToChange.lastName)
        let isAdmin = Boolean(userToChange.isAdmin)
        let status = String(userToChange.status)
        let app = await createMockApp();
        let token = await createJWT(user)
        let ctx = await createMockContext(app, `{"email":"hey@hey.to", "password":"meow","firstName":"Thomas","lastName":"Schmiddy","isAdmin":true,"status":"registered"}`, [["Content-Type", "application/json"]], "/", token);
        await patchUser(ctx, 3);
        userToChange = await User.find("3");
        assertNotEquals("undefined", String(userToChange.eMail));
        assertNotEquals("undefined", String(userToChange.password));
        assertNotEquals("undefined", String(userToChange.firstName));
        assertNotEquals("undefined", String(userToChange.lastName));
        assertNotEquals(undefined, Boolean(userToChange.isAdmin));
        assertNotEquals("undefined", String(userToChange.status));
        assertNotEquals(email, String(userToChange.eMail));
        assertEquals(password, String(userToChange.password));
        assertNotEquals(firstName, String(userToChange.fistName));
        assertNotEquals(lastName, String(userToChange.lastName));
        assertNotEquals(isAdmin, Boolean(userToChange.isAdmin));
        assertNotEquals(status, String(userToChange.status));
    },
    sanitizeResources: false,
})

Deno.test({
    name: "PatchUserUserHimself",
    async fn(): Promise<void> {
        await setup(true);
        let user = await insertUser("test@test", "ash", false, "Test", "Tester", "registered");
        let email = String(user.eMail);
        let password = String(user.password);
        let firstName = String(user.firstName);
        let lastName = String(user.lastName)
        let isAdmin = Boolean(user.isAdmin)
        let status = String(user.status)
        let app = await createMockApp();
        let token = await createJWT(user)
        let ctx = await createMockContext(app, `{"email":"hey@hey.to", "password":"meow","firstName":"Thomas","lastName":"Schmiddy","isAdmin":true,"status":"blaaaaa"}`, [["Content-Type", "application/json"]], "/", token);
        await patchUser(ctx, 2);
        user = await User.find("2");
        assertNotEquals("undefined", String(user.eMail));
        assertNotEquals("undefined", String(user.password));
        assertNotEquals("undefined", String(user.firstName));
        assertNotEquals("undefined", String(user.lastName));
        assertNotEquals(undefined, Boolean(user.isAdmin));
        assertNotEquals("undefined", String(user.status));
        assertNotEquals(email, String(user.eMail));
        assertNotEquals(password, String(user.password));
        assertNotEquals(firstName, String(user.fistName));
        assertNotEquals(lastName, String(user.lastName));
        assertEquals(isAdmin, Boolean(user.isAdmin));
        assertEquals(status, String(user.status));
    },
    sanitizeResources: false,
})

Deno.test({
    name: "PatchUserInvitation",
    async fn(): Promise<void> {
        await setup(true);
        let app = await createMockApp();
        let user = await insertUser("test@test", "ash", true, "Test", "Tester", "registered");
        let token = await createJWT(user)
        let ctx = await createMockContext(app, `{"email": "testing@test"}`, [["Content-Type", "application/json"]], "/", token);
        let userToChange = await createUser(ctx, new MockEmailClient())
        if (userToChange) {
            let email = String(userToChange.eMail);
            let password = String(userToChange.password);
            let firstName = String(userToChange.firstName);
            let lastName = String(userToChange.lastName)
            let isAdmin = Boolean(userToChange.isAdmin)
            let status = String(userToChange.status)

            let testToken = await getTokens(Number(userToChange.id));
            assertNotEquals(testToken, undefined)
            if (testToken) {
                ctx = await createMockContext(app, `{"email":"hey@hey.to", "password":"meow","firstName":"Thomas","lastName":"Schmiddy","isAdmin":false,"status":"registered"}`, [["Content-Type", "application/json"], ["invitationToken", String(testToken[0].token)]], "/");
                await patchUser(ctx, Number(userToChange.id));
                userToChange = await User.find(Number(userToChange.id));
                assertNotEquals("undefined", String(userToChange.eMail));
                assertNotEquals("undefined", String(userToChange.password));
                assertNotEquals("undefined", String(userToChange.firstName));
                assertNotEquals("undefined", String(userToChange.lastName));
                assertNotEquals(undefined, Boolean(userToChange.isAdmin));
                assertNotEquals("undefined", String(userToChange.status));
                assertNotEquals(email, String(userToChange.eMail));
                assertNotEquals(password, String(userToChange.password));
                assertNotEquals(firstName, String(userToChange.fistName));
                assertNotEquals(lastName, String(userToChange.lastName));
                assertEquals(isAdmin, Boolean(userToChange.isAdmin));
                assertEquals(status, String(userToChange.status));
            }
        }
    },
    sanitizeResources: false,
})

Deno.test({
    name: "PatchUserFakeInvitation",
    async fn(): Promise<void> {
        await setup(true);
        let app = await createMockApp();
        let user = await insertUser("test@test", "ash", true, "Test", "Tester", "registered");
        let token = await createJWT(user)
        let ctx = await createMockContext(app, `{"email": "testing@test"}`, [["Content-Type", "application/json"]], "/", token);
        let userToChange = await createUser(ctx, new MockEmailClient())
        if (userToChange) {
            let testToken = await getTokens(Number(userToChange.id));
            assertNotEquals(testToken, undefined)
            if (testToken) {
                ctx = await createMockContext(app, `{"email":"hey@hey.to", "password":"meow","firstName":"Thomas","lastName":"Schmiddy","isAdmin":false,"status":"registered"}`, [["Content-Type", "application/json"], ["invitationToken", "1234rtg"]], "/");
                await patchUser(ctx, Number(userToChange.id));
                assertEquals(ctx.response.status, 401)
            }
        }
    },
    sanitizeResources: false,
})