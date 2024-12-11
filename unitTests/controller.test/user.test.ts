import { setup } from "../../src/helper/setup.ts";
import { insertUser } from "../../src/controller/databaseFetcher/user.ts";
import { createMockApp } from "../mockObjects/oak/mockApp.test.ts";
import { createJWT } from "../../src/controller/validation.controller.ts";
import { createMockContext } from "../mockObjects/oak/mockContext.test.ts";
import {
  assertEquals,
  assertNotEquals,
} from "https://deno.land/std@0.150.0/testing/asserts.ts";
import {
  createUser,
  getUser,
  getUserProjects,
  getUsers,
  patchUser,
  resetPassword,
} from "../../src/controller/user.controller.ts";
import { User } from "../../src/model/db/user.ts";
import { MockEmailClient } from "../mockObjects/mockEmailClient.test.ts";
import { getTokens } from "../../src/controller/databaseFetcher/token.ts";
import { getInvitations } from "../../src/controller/databaseFetcher/invitation.ts";
import { Project } from "../../src/model/db/project.ts";
import { UserIsPartOfProject } from "../../src/model/db/userIsPartOfProject.ts";
import { Stage } from "../../src/model/db/stage.ts";
import { client, db } from "../../src/controller/database.controller.ts";
import { getResetTokens } from "../../src/controller/databaseFetcher/resetToken.ts";

Deno.test({
  name: "insertUserForCreation",
  async fn(): Promise<void> {
    await setup(true);
    const user = await insertUser(
      "test@test",
      "ash",
      true,
      "Test",
      "Tester",
      "active",
    );

    const app = await createMockApp();
    const token = await createJWT(user);
    const ctx = await createMockContext(
      app,
      `{"email": "test2@test"}`,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );
    await createUser(ctx, new MockEmailClient());

    assertEquals(ctx.response.status, 201);
    const answer = JSON.parse(ctx.response.body as string);
    assertEquals(answer.email, "test2@test");
    assertNotEquals(answer.id, undefined);
    await db.close();
    await client.end();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "insertUserForCreationUnauth",
  async fn(): Promise<void> {
    await setup(true);
    const user = await insertUser(
      "testt43t34t@test",
      "ash",
      false,
      "Test",
      "Tester",
      "active",
    );

    const app = await createMockApp();
    const token = await createJWT(user);
    const ctx = await createMockContext(
      app,
      `{"email": "andreas.decker@uni-ulm.de"}`,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );
    await createUser(ctx, new MockEmailClient());

    assertEquals(ctx.response.status, 401);

    await db.close();
    await client.end();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "insertUserNoEmail",
  async fn(): Promise<void> {
    await setup(true);
    const user = await insertUser(
      "test1jgfh@test",
      "ash",
      true,
      "Test",
      "Tester",
      "active",
    );

    const app = await createMockApp();
    const token = await createJWT(user);
    const ctx = await createMockContext(
      app,
      `{}`,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );
    await createUser(ctx, new MockEmailClient());

    assertEquals(ctx.response.status, 422);

    await db.close();
    await client.end();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "insertUserEmailAlreadyExists",
  async fn(): Promise<void> {
    await setup(true);
    const user = await insertUser(
      "test@test",
      "ash",
      true,
      "Test",
      "Tester",
      "active",
    );

    const app = await createMockApp();
    const token = await createJWT(user);
    const ctx = await createMockContext(
      app,
      `{"email": "test@test"}`,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );
    await createUser(ctx, new MockEmailClient());

    assertEquals(ctx.response.status, 422);

    await db.close();
    await client.end();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "getAllUsersAsAdmin",
  async fn(): Promise<void> {
    await setup(true);
    const countOld = (await User.all()).length;
    const user = await insertUser(
      "test@test",
      "ash",
      true,
      "Test",
      "Tester",
      "active",
    );
    await insertUser("tes2t@test", "ash", true, "Test", "Tester", "active");
    const app = await createMockApp();
    const token = await createJWT(user);
    const ctx = await createMockContext(
      app,
      undefined,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );
    await getUsers(ctx);
    assertEquals(ctx.response.status, 200);
    const answer = JSON.parse(ctx.response.body as string);
    assertEquals(answer.users.length, countOld + 2);

    await db.close();
    await client.end();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "getAllUsersUnauthorized",
  async fn(): Promise<void> {
    await setup(true);
    const user = await insertUser(
      "test@test",
      "ash",
      false,
      "Test",
      "Tester",
      "active",
    );
    await insertUser("test2@test", "ash", true, "Test", "Tester", "active");
    const app = await createMockApp();
    const token = await createJWT(user);
    const ctx = await createMockContext(
      app,
      undefined,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );
    await getUsers(ctx);
    assertEquals(ctx.response.status, 401);

    await db.close();
    await client.end();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "getOneUserAsAdmin",
  fn: async function (): Promise<void> {
    await setup(true);
    const user = await insertUser(
      "test@test",
      "ash",
      true,
      "Test",
      "Tester",
      "active",
    );
    const user2 = await insertUser(
      "test2@test",
      "ash",
      true,
      "Testing",
      "Testerer",
      "active",
    );
    const app = await createMockApp();
    const token = await createJWT(user);
    const ctx = await createMockContext(
      app,
      undefined,
      [["Content-Type", "application/json"]],
      "/users/1",
      token,
    );
    await getUser(ctx, Number(user2.id));
    assertEquals(ctx.response.status, 200);
    const answer = JSON.parse(ctx.response.body as string);
    assertEquals(answer.email, "test2@test");
    assertEquals(answer.isAdmin, true);
    assertEquals(answer.firstName, "Testing");
    assertEquals(answer.lastName, "Testerer");
    assertEquals(answer.status, "active");
    assertEquals(answer.password, undefined);
    await db.close();
    await client.end();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "getOneUserUnAuthorized",
  fn: async function (): Promise<void> {
    await setup(true);
    const user = await insertUser(
      "test2@test",
      "ash",
      false,
      "Test",
      "Tester",
      "active",
    );
    const user2 = await insertUser(
      "test@test",
      "ash",
      true,
      "Test",
      "Tester",
      "active",
    );
    const app = await createMockApp();
    const token = await createJWT(user);
    const ctx = await createMockContext(
      app,
      undefined,
      [["Content-Type", "application/json"]],
      "/users/1",
      token,
    );
    await getUser(ctx, Number(user2.id));
    assertEquals(ctx.response.status, 401);

    await db.close();
    await client.end();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "ResetPassword",
  fn: async function (): Promise<void> {
    await setup(true);
    const user = await insertUser(
      "test@test",
      "ash",
      false,
      "Test",
      "Tester",
      "active",
    );
    const app = await createMockApp();
    const token = await createJWT(user);
    const ctx = await createMockContext(
      app,
      '{"email":"test@test" }',
      [["Content-Type", "application/json"]],
      "/users/1",
      token,
    );
    await resetPassword(ctx, new MockEmailClient());
    assertEquals(ctx.response.status, 200);

    await db.close();
    await client.end();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "ResetPasswordWrongMail",
  fn: async function (): Promise<void> {
    await setup(true);
    const user = await insertUser(
      "test@test",
      "ash",
      false,
      "Test",
      "Tester",
      "active",
    );
    const app = await createMockApp();
    const token = await createJWT(user);
    const ctx = await createMockContext(
      app,
      '{"email":"test@" }',
      [["Content-Type", "application/json"]],
      "/users/1",
      token,
    );
    await resetPassword(ctx, new MockEmailClient());
    assertEquals(ctx.response.status, 400);

    await db.close();
    await client.end();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "ResetPasswordNoMail",
  fn: async function (): Promise<void> {
    await setup(true);
    const user = await insertUser(
      "test@test",
      "ash",
      false,
      "Test",
      "Tester",
      "active",
    );
    const app = await createMockApp();
    const token = await createJWT(user);
    const ctx = await createMockContext(
      app,
      "{}",
      [["Content-Type", "application/json"]],
      "/users/1",
      token,
    );
    await resetPassword(ctx, new MockEmailClient());
    assertEquals(ctx.response.status, 422);

    await db.close();
    await client.end();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "getUsersProjects",
  fn: async function (): Promise<void> {
    await setup(true);
    const user = await insertUser(
      "test@test",
      "ash",
      true,
      "Test",
      "Tester",
      "active",
    );
    const project = await Project.create({
      name: "Test",
      minCountReviewers: 1,
      countDecisiveReviewers: 1,
      evaluationFormula: "",
      combinationOfReviewers: "",
      mergeThreshold: 0.8,
    });
    /*const userProject = */ await UserIsPartOfProject.create({
      isOwner: true,
      userId: Number(user.id),
      projectId: Number(project.id),
    });
    await Stage.create({
      projectId: Number(project.id),
      name: "awesome Stage",
      number: 0,
    });
    await Stage.create({
      projectId: Number(project.id),
      name: "the next Stage",
      number: 1,
    });
    const app = await createMockApp();
    const token = await createJWT(user);
    const ctx = await createMockContext(
      app,
      "{}",
      [["Content-Type", "application/json"]],
      "/users/1",
      token,
    );
    await getUserProjects(ctx, 1);
    assertEquals(ctx.response.status, 200);
    const answer = JSON.parse(ctx.response.body as string);
    assertEquals(1, answer.projects.length);
    assertEquals("Test", answer.projects[0].name);
    assertEquals(2, answer.projects[0].stages.length);
    assertEquals("the next Stage", answer.projects[0].stages[1].name);
    assertEquals(1, answer.projects[0].stages[1].number);
    await db.close();
    await client.end();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "getUsersProjectsUnAuthorized",
  fn: async function (): Promise<void> {
    await setup(true);
    const user = await insertUser(
      "test@test",
      "ash",
      false,
      "Test",
      "Tester",
      "active",
    );
    const project = await Project.create({
      name: "Test",
      minCountReviewers: 1,
      countDecisiveReviewers: 1,
      evaluationFormula: "",
      combinationOfReviewers: "",
      mergeThreshold: 0.8,
    });
    /*const userProject = */ await UserIsPartOfProject.create({
      isOwner: true,
      userId: Number(user.id),
      projectId: Number(project.id),
    });
    await Stage.create({
      projectId: Number(project.id),
      name: "awesome Stage",
      number: 0,
    });
    await Stage.create({
      projectId: Number(project.id),
      name: "the next Stage",
      number: 1,
    });
    const app = await createMockApp();
    const token = await createJWT(user);
    const ctx = await createMockContext(
      app,
      "{}",
      [["Content-Type", "application/json"]],
      "/users/1",
      token,
    );
    await getUserProjects(ctx, 1);
    assertEquals(ctx.response.status, 401);

    await db.close();
    await client.end();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "PatchUserAdmin",
  async fn(): Promise<void> {
    await setup(true);
    const user = await insertUser(
      "test@test",
      "ash",
      true,
      "Test",
      "Tester",
      "active",
    );
    let userToChange = await insertUser(
      "theo@eo",
      "leo",
      false,
      "Theo",
      "Eo",
      "unregistered",
    );
    const email = String(userToChange.eMail);
    const password = String(userToChange.password);
    const firstName = String(userToChange.firstName);
    const lastName = String(userToChange.lastName);
    const isAdmin = Boolean(userToChange.isAdmin);
    const status = String(userToChange.status);
    const app = await createMockApp();
    const token = await createJWT(user);
    const ctx = await createMockContext(
      app,
      `{"email":"hey@hey.to", "password":"meow","firstName":"Thomas","lastName":"Schmiddy","isAdmin":true,"status":"active"}`,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );
    await patchUser(ctx, Number(userToChange.id));
    userToChange = await User.find(Number(userToChange.id));
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

    await db.close();
    await client.end();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "PatchUserHimself",
  async fn(): Promise<void> {
    await setup(true);
    let user = await insertUser(
      "test@test",
      "ash",
      false,
      "Test",
      "Tester",
      "registered",
    );
    const email = String(user.eMail);
    const password = String(user.password);
    const firstName = String(user.firstName);
    const lastName = String(user.lastName);
    const isAdmin = Boolean(user.isAdmin);
    const status = String(user.status);
    const app = await createMockApp();
    const token = await createJWT(user);
    const ctx = await createMockContext(
      app,
      `{"email":"hey@hey.to", "password":"meow","firstName":"Thomas","lastName":"Schmiddy","isAdmin":true,"status":"blaaaaa"}`,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );
    await patchUser(ctx, Number(user.id));
    user = await User.find(Number(user.id));
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
    await db.close();
    await client.end();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "PatchUserInvitation",
  async fn(): Promise<void> {
    await setup(true);
    const app = await createMockApp();
    const user = await insertUser(
      "test@test",
      "ash",
      true,
      "Test",
      "Tester",
      "registered",
    );
    const token = await createJWT(user);
    let ctx = await createMockContext(
      app,
      `{"email": "testing@test"}`,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );
    await createUser(ctx, new MockEmailClient());
    let userToChange = JSON.parse(ctx.response.body as string);
    if (userToChange) {
      const email = String(userToChange.eMail);
      const password = String(userToChange.password);
      const firstName = String(userToChange.firstName);
      const lastName = String(userToChange.lastName);
      const isAdmin = Boolean(userToChange.isAdmin);
      const status = String(userToChange.status);

      const testToken = await getInvitations(Number(userToChange.id));
      assertNotEquals(testToken, undefined);
      if (testToken) {
        ctx = await createMockContext(
          app,
          `{"email":"hey@hey.to", "password":"meow","firstName":"Thomas","lastName":"Schmiddy","isAdmin":false,"status":"registered"}`,
          [["Content-Type", "application/json"], [
            "invitationToken",
            String(testToken[0].token),
          ]],
          "/",
        );
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
        assertNotEquals(firstName, String(userToChange.firstName));
        assertNotEquals(lastName, String(userToChange.lastName));
        assertEquals(isAdmin, Boolean(userToChange.isAdmin));
        assertEquals(status, String(userToChange.status));
      }
    }

    await db.close();
    await client.end();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "PatchUserInvitationNoPassword",
  async fn(): Promise<void> {
    await setup(true);
    const app = await createMockApp();
    const user = await insertUser(
      "test@test",
      "ash",
      true,
      "Test",
      "Tester",
      "registered",
    );
    const token = await createJWT(user);
    let ctx = await createMockContext(
      app,
      `{"email": "testing@test"}`,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );
    await createUser(ctx, new MockEmailClient());
    let userToChange = JSON.parse(ctx.response.body as string);
    if (userToChange) {
      // const email = String(userToChange.eMail);
      // const password = String(userToChange.password);
      // const firstName = String(userToChange.firstName);
      // const lastName = String(userToChange.lastName)
      // const isAdmin = Boolean(userToChange.isAdmin)
      // const status = String(userToChange.status)

      const testToken = await getInvitations(Number(userToChange.id));
      assertNotEquals(testToken, undefined);
      if (testToken) {
        ctx = await createMockContext(
          app,
          `{"email":"hey@hey.to","firstName":"Thomas","lastName":"Schmiddy","isAdmin":false,"status":"registered"}`,
          [["Content-Type", "application/json"], [
            "invitationToken",
            String(testToken[0].token),
          ]],
          "/",
        );
        await patchUser(ctx, Number(userToChange.id));
        userToChange = await User.find(Number(userToChange.id));

        assertEquals(ctx.response.status, 400);
      }
    }

    await db.close();
    await client.end();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "PatchUserResetToken",
  async fn(): Promise<void> {
    await setup(true);
    const app = await createMockApp();
    let user = await insertUser(
      "tester@test",
      "ash",
      false,
      "Test",
      "Tester",
      "registered",
    );
    const token = await createJWT(user);
    let ctx = await createMockContext(
      app,
      `{"email":"tester@test"}`,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );
    await resetPassword(ctx, new MockEmailClient());

    const email = String(user.eMail);
    const password = String(user.password);
    const firstName = String(user.firstName);
    const lastName = String(user.lastName);
    const isAdmin = Boolean(user.isAdmin);

    const testToken = await getResetTokens(Number(user.id));
    assertNotEquals(testToken, undefined);
    if (testToken) {
      ctx = await createMockContext(
        app,
        `{"email":"hey@hey.to", "password":"meow","firstName":"Thomas","lastName":"Schmiddy","isAdmin":true,"status":"registered"}`,
        [["Content-Type", "application/json"], [
          "resetToken",
          String(testToken[0].token),
        ]],
        "/",
      );
      await patchUser(ctx, Number(user.id));
      assertEquals(ctx.response.status, 200);
      user = await User.find(Number(user.id));
      assertNotEquals("undefined", String(user.eMail));
      assertNotEquals("undefined", String(user.password));
      assertNotEquals("undefined", String(user.firstName));
      assertNotEquals("undefined", String(user.lastName));
      assertNotEquals(undefined, Boolean(user.isAdmin));
      assertNotEquals("undefined", String(user.status));
      assertNotEquals(email, String(user.eMail));
      assertNotEquals(password, String(user.password));
      assertNotEquals(firstName, String(user.firstName));
      assertNotEquals(lastName, String(user.lastName));
      assertEquals(isAdmin, Boolean(user.isAdmin));
    }

    await db.close();
    await client.end();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "PatchUserFakeInvitation",
  async fn(): Promise<void> {
    await setup(true);
    const app = await createMockApp();
    const user = await insertUser(
      "test@test",
      "ash",
      true,
      "Test",
      "Tester",
      "registered",
    );
    const token = await createJWT(user);
    let ctx = await createMockContext(
      app,
      `{"email": "testing@test"}`,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );
    await createUser(ctx, new MockEmailClient());
    const userToChange = JSON.parse(ctx.response.body as string);
    if (userToChange) {
      const testToken = await getTokens(Number(userToChange.id));
      assertNotEquals(testToken, undefined);
      if (testToken) {
        ctx = await createMockContext(
          app,
          `{"email":"hey@hey.to", "password":"meow","firstName":"Thomas","lastName":"Schmiddy","isAdmin":false,"status":"registered"}`,
          [["Content-Type", "application/json"], [
            "invitationToken",
            "1234rtg",
          ]],
          "/",
        );
        await patchUser(ctx, Number(userToChange.id));
        assertEquals(ctx.response.status, 401);
      }
    }

    await db.close();
    await client.end();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "PatchUserNoId",
  async fn(): Promise<void> {
    await setup(true);
    const app = await createMockApp();
    const user = await insertUser(
      "test@test",
      "ash",
      true,
      "Test",
      "Tester",
      "registered",
    );
    const token = await createJWT(user);
    const ctx = await createMockContext(
      app,
      `{"email": "testing@test"}`,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );

    await patchUser(ctx, undefined);
    assertEquals(ctx.response.status, 422);
    await db.close();
    await client.end();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "TryPatchWithoutContent",
  async fn(): Promise<void> {
    await setup(true);
    const app = await createMockApp();
    const user = await insertUser(
      "test@test",
      "ash",
      true,
      "Test",
      "Tester",
      "registered",
    );
    const token = await createJWT(user);
    const ctx = await createMockContext(
      app,
      undefined,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );
    await createUser(ctx, new MockEmailClient());

    assertEquals(ctx.response.status, 422);

    await db.close();
    await client.end();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});
