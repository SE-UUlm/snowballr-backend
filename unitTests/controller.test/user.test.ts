import {setup} from "../../src/helper/setup.ts";
import {insertUser} from "../../src/controller/databaseFetcher/user.ts";
import {createMockApp} from "../mockObjects/oak/mockApp.test.ts";
import {createJWT} from "../../src/controller/validation.ts";
import {createMockContext} from "../mockObjects/oak/mockContext.test.ts";
import {assertEquals, assertNotEquals} from "https://deno.land/std/testing/asserts.ts"
import {createUser, getUser, getUserProjects, getUsers, patchUser, resetPassword} from "../../src/controller/user.ts";
import {User} from "../../src/model/db/user.ts";
import {MockEmailClient} from "../mockObjects/mockEmailClient.test.ts";
import {getTokens} from "../../src/controller/databaseFetcher/token.ts";
import {getInvitations} from "../../src/controller/databaseFetcher/invitation.ts";
import {Project} from "../../src/model/db/project.ts";
import {UserIsPartOfProject} from "../../src/model/db/userIsPartOfProject.ts";
import {Stage} from "../../src/model/db/stage.ts";

Deno.test({
    name: "insertUserForCreation",
    async fn(): Promise<void> {
        await setup(true);
        let user = await insertUser("test@test", "ash", true, "Test", "Tester", "active");

        let app = await createMockApp();
        let token = await createJWT(user)
        let ctx = await createMockContext(app, `{"email": "andreas.decker@uni-ulm.de"}`, [["Content-Type", "application/json"]], "/", token);
        await createUser(ctx, new MockEmailClient())

        assertEquals(ctx.response.status, 201)
    },
    sanitizeResources: false,
})

Deno.test({
    name: "insertUserForCreation",
    async fn(): Promise<void> {
        await setup(true);
        let user = await insertUser("test@test", "ash", false, "Test", "Tester", "active");

        let app = await createMockApp();
        let token = await createJWT(user)
        let ctx = await createMockContext(app, `{"email": "andreas.decker@uni-ulm.de"}`, [["Content-Type", "application/json"]], "/", token);
        await createUser(ctx, new MockEmailClient())

        assertEquals(ctx.response.status, 401)
    },
    sanitizeResources: false,
})

Deno.test({
    name: "insertUserNoEmail",
    async fn(): Promise<void> {
        await setup(true);
        let user = await insertUser("test@test", "ash", true, "Test", "Tester", "active");

        let app = await createMockApp();
        let token = await createJWT(user)
        let ctx = await createMockContext(app, `{}`, [["Content-Type", "application/json"]], "/", token);
        await createUser(ctx, new MockEmailClient())

        assertEquals(ctx.response.status, 422)
    },
    sanitizeResources: false,
})

Deno.test({
    name: "getAllUsersAsAdmin",
    async fn(): Promise<void> {
        await setup(true);
        let user = await insertUser("test@test", "ash", true, "Test", "Tester", "active");
        await insertUser("test@test", "ash", true, "Test", "Tester", "active");
        let app = await createMockApp();
        let token = await createJWT(user)
        let ctx = await createMockContext(app, undefined, [["Content-Type", "application/json"]], "/", token);
        await getUsers(ctx)
        assertEquals(ctx.response.status, 200);


    },
    sanitizeResources: false,
})

Deno.test({
    name: "getAllUsersUnauthorized",
    async fn(): Promise<void> {
        await setup(true);
        let user = await insertUser("test@test", "ash", false, "Test", "Tester", "active");
        await insertUser("test@test", "ash", true, "Test", "Tester", "active");
        let app = await createMockApp();
        let token = await createJWT(user)
        let ctx = await createMockContext(app, undefined, [["Content-Type", "application/json"]], "/", token);
        await getUsers(ctx)
        assertEquals(ctx.response.status, 401);


    },
    sanitizeResources: false,
})

Deno.test({
    name: "getOneUserAsAdmin",
    fn: async function (): Promise<void> {
        await setup(true);
        let user = await insertUser("test@test", "ash", true, "Test", "Tester", "active");
        await insertUser("test@test", "ash", true, "Test", "Tester", "active");
        let app = await createMockApp();
        let token = await createJWT(user)
        let ctx = await createMockContext(app, undefined, [["Content-Type", "application/json"]], "/users/1", token);
        await getUser(ctx, 2);
        assertEquals(ctx.response.status, 200)

    },
    sanitizeResources: false,
})

Deno.test({
    name: "getOneUserNoId",
    fn: async function (): Promise<void> {
        await setup(true);
        let user = await insertUser("test@test", "ash", true, "Test", "Tester", "active");
        await insertUser("test@test", "ash", true, "Test", "Tester", "active");
        let app = await createMockApp();
        let token = await createJWT(user)
        let ctx = await createMockContext(app, undefined, [["Content-Type", "application/json"]], "/users/1", token);
        await getUser(ctx, undefined);
        assertEquals(ctx.response.status, 422)

    },
    sanitizeResources: false,
})

Deno.test({
    name: "getOneUserUnAuthorized",
    fn: async function (): Promise<void> {
        await setup(true);
        let user = await insertUser("test@test", "ash", false, "Test", "Tester", "active");
        await insertUser("test@test", "ash", true, "Test", "Tester", "active");
        let app = await createMockApp();
        let token = await createJWT(user)
        let ctx = await createMockContext(app, undefined, [["Content-Type", "application/json"]], "/users/1", token);
        await getUser(ctx, 3);
        assertEquals(ctx.response.status, 401)

    },
    sanitizeResources: false,
})

Deno.test({
    name: "ResetPassword",
    fn: async function (): Promise<void> {
        await setup(true);
        let user = await insertUser("test@test", "ash", false, "Test", "Tester", "active");
        let app = await createMockApp();
        let token = await createJWT(user)
        let ctx = await createMockContext(app, '{"email":"test@test" }', [["Content-Type", "application/json"]], "/users/1", token);
        await resetPassword(ctx, new MockEmailClient());
        assertEquals(ctx.response.status, 200)

    },
    sanitizeResources: false,
})

Deno.test({
    name: "ResetPasswordWrongMail",
    fn: async function (): Promise<void> {
        await setup(true);
        let user = await insertUser("test@test", "ash", false, "Test", "Tester", "active");
        let app = await createMockApp();
        let token = await createJWT(user)
        let ctx = await createMockContext(app, '{"email":"test@" }', [["Content-Type", "application/json"]], "/users/1", token);
        await resetPassword(ctx, new MockEmailClient());
        assertEquals(ctx.response.status, 400)

    },
    sanitizeResources: false,
})

Deno.test({
    name: "ResetPasswordNoMail",
    fn: async function (): Promise<void> {
        await setup(true);
        let user = await insertUser("test@test", "ash", false, "Test", "Tester", "active");
        let app = await createMockApp();
        let token = await createJWT(user)
        let ctx = await createMockContext(app, "{}", [["Content-Type", "application/json"]], "/users/1", token);
        await resetPassword(ctx, new MockEmailClient());
        assertEquals(ctx.response.status, 422)

    },
    sanitizeResources: false,
})

Deno.test({
    name: "getUsersProjects",
    fn: async function (): Promise<void> {
        await setup(true);
        let user = await insertUser("test@test", "ash", true, "Test", "Tester", "active");
        let project = await Project.create({name: "Test", minCountReviewers: 1, countDecisiveReviewers: 1})
        let userProject = await UserIsPartOfProject.create({
            isOwner: true,
            userId: Number(user.id),
            projectId: Number(project.id)
        })
        await Stage.create({projectId: Number(project.id), name: "awesome Stage", number: 0})
        await Stage.create({projectId: Number(project.id), name: "the next Stage", number: 1})
        let app = await createMockApp();
        let token = await createJWT(user)
        let ctx = await createMockContext(app, "{}", [["Content-Type", "application/json"]], "/users/1", token);
        await getUserProjects(ctx, 1)
        assertEquals(ctx.response.status, 200)
    },
    sanitizeResources: false,
})

Deno.test({
    name: "getUsersProjectsNoId",
    fn: async function (): Promise<void> {
        await setup(true);
        let user = await insertUser("test@test", "ash", true, "Test", "Tester", "active");
        let project = await Project.create({name: "Test", minCountReviewers: 0, countDecisiveReviewers: 0})
        let userProject = await UserIsPartOfProject.create({
            isOwner: true,
            userId: Number(user.id),
            projectId: Number(project.id)
        })
        await Stage.create({projectId: Number(project.id), name: "awesome Stage", number: 0})
        await Stage.create({projectId: Number(project.id), name: "the next Stage", number: 1})
        let app = await createMockApp();
        let token = await createJWT(user)
        let ctx = await createMockContext(app, "{}", [["Content-Type", "application/json"]], "/users/1", token);
        await getUserProjects(ctx, undefined)
        assertEquals(ctx.response.status, 422)
    },
    sanitizeResources: false,
})

Deno.test({
    name: "getUsersProjectsUnAuthorized",
    fn: async function (): Promise<void> {
        await setup(true);
        let user = await insertUser("test@test", "ash", false, "Test", "Tester", "active");
        let project = await Project.create({name: "Test", minCountReviewers: 1, countDecisiveReviewers: 1})
        let userProject = await UserIsPartOfProject.create({
            isOwner: true,
            userId: Number(user.id),
            projectId: Number(project.id)
        })
        await Stage.create({projectId: Number(project.id), name: "awesome Stage", number: 0})
        await Stage.create({projectId: Number(project.id), name: "the next Stage", number: 1})
        let app = await createMockApp();
        let token = await createJWT(user)
        let ctx = await createMockContext(app, "{}", [["Content-Type", "application/json"]], "/users/1", token);
        await getUserProjects(ctx, 1)
        assertEquals(ctx.response.status, 401)
    },
    sanitizeResources: false,
})

Deno.test({
    name: "PatchUserAdmin",
    async fn(): Promise<void> {
        await setup(true);
        let user = await insertUser("test@test", "ash", true, "Test", "Tester", "active");
        let userToChange = await insertUser("theo@eo", "leo", false, "Theo", "Eo", "unregistered");
        let email = String(userToChange.eMail);
        let password = String(userToChange.password);
        let firstName = String(userToChange.firstName);
        let lastName = String(userToChange.lastName)
        let isAdmin = Boolean(userToChange.isAdmin)
        let status = String(userToChange.status)
        let app = await createMockApp();
        let token = await createJWT(user)
        let ctx = await createMockContext(app, `{"email":"hey@hey.to", "password":"meow","firstName":"Thomas","lastName":"Schmiddy","isAdmin":true,"status":"active"}`, [["Content-Type", "application/json"]], "/", token);
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
    name: "PatchUserHimself",
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

            let testToken = await getInvitations(Number(userToChange.id));
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

Deno.test({
    name: "PatchUserNoId",
    async fn(): Promise<void> {
        await setup(true);
        let app = await createMockApp();
        let user = await insertUser("test@test", "ash", true, "Test", "Tester", "registered");
        let token = await createJWT(user)
        let ctx = await createMockContext(app, `{"email": "testing@test"}`, [["Content-Type", "application/json"]], "/", token);

        await patchUser(ctx, undefined);
        assertEquals(ctx.response.status, 422)


    },
    sanitizeResources: false,
})

Deno.test({
    name: "TryPatchWithoutContent",
    async fn(): Promise<void> {
        await setup(true);
        let app = await createMockApp();
        let user = await insertUser("test@test", "ash", true, "Test", "Tester", "registered");
        let token = await createJWT(user)
        let ctx = await createMockContext(app, undefined, [["Content-Type", "application/json"]], "/", token);
        await createUser(ctx, new MockEmailClient())

        assertEquals(ctx.response.status, 401)
    }

})