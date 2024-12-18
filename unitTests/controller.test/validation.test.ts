import { createMockApp } from "../mockObjects/oak/mockApp.test.ts";
import { createMockContext } from "../mockObjects/oak/mockContext.test.ts";
import {
  checkPO,
  createJWT,
  getPayloadFromJWTHeader,
  validateContentType,
  validateJWTIfExists,
} from "../../src/controller/validation.controller.ts";
import { assertEquals } from "https://deno.land/std@0.150.0/testing/asserts.ts";
import { emptyAsyncFunctionTest } from "../mockObjects/emptyAsyncFunction.test.ts";
import { setup } from "../../src/helper/setup.ts";
import { insertUser } from "../../src/controller/databaseFetcher/user.ts";
import { Project } from "../../src/model/db/project.ts";
import { UserIsPartOfProject } from "../../src/model/db/userIsPartOfProject.ts";
import { client, db } from "../../src/controller/database.controller.ts";

Deno.test({
  name: "testCorrectContentTypeAndContent",
  async fn(): Promise<void> {
    const app = await createMockApp();
    const ctx = await createMockContext(
      app,
      `{"email": "test@test", "password":"ash"}`,
    );
    await validateContentType(ctx, emptyAsyncFunctionTest);

    assertEquals(ctx.response.status, 999);
  },
});

Deno.test({
  name: "testEmptyContent",
  async fn(): Promise<void> {
    const app = await createMockApp();
    const ctx = await createMockContext(app, undefined);
    await validateContentType(ctx, emptyAsyncFunctionTest);

    assertEquals(ctx.response.status, 999);
  },
});

Deno.test({
  name: "testCorrectContentTypeWrongContent",
  async fn(): Promise<void> {
    const app = await createMockApp();
    const ctx = await createMockContext(
      app,
      `{"email": "test@test", "password":"ash"`,
    );
    await validateContentType(ctx, emptyAsyncFunctionTest);

    assertEquals(ctx.response.status, 415);
  },
});

Deno.test({
  name: "testWrongContentType",
  async fn(): Promise<void> {
    const app = await createMockApp();
    const ctx = await createMockContext(
      app,
      `{"email": "test@test", "password":"ash"}`,
      [["Content-Type", "text"]],
    );
    await validateContentType(ctx, emptyAsyncFunctionTest);

    assertEquals(ctx.response.status, 415);
  },
});

Deno.test({
  name: "loginPageUnauthorizedAllowed",
  async fn(): Promise<void> {
    const app = await createMockApp();
    const ctx = await createMockContext(
      app,
      `{"email": "test@test", "password":"ash"}`,
      [["Content-Type", "text"]],
      "/login/",
    );
    await validateJWTIfExists(ctx, emptyAsyncFunctionTest);

    assertEquals(ctx.response.status, 200);
  },
});

Deno.test({
  name: "UnauthorizedRequest",
  async fn(): Promise<void> {
    const app = await createMockApp();
    const ctx = await createMockContext(
      app,
      `{"email": "test@test", "password":"ash"}`,
      [["Content-Type", "text"]],
    );
    await validateJWTIfExists(ctx, emptyAsyncFunctionTest);

    assertEquals(ctx.response.status, 401);
  },
});

Deno.test({
  name: "authorizedRequest",
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
      undefined,
      [["Content-Type", "text"]],
      "/",
      token,
    );
    await validateJWTIfExists(ctx, emptyAsyncFunctionTest);

    assertEquals(ctx.response.status, 999);

    await db.close();
    await client.end();
  },

  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "unAuthorizedRequestWithWrongToken",
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
      undefined,
      [["Content-Type", "text"]],
      "/",
      token + "a",
    );
    await validateJWTIfExists(ctx, emptyAsyncFunctionTest);

    assertEquals(ctx.response.status, 401);
    await db.close();
    await client.end();
  },
});

Deno.test({
  name: "isPO",
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
    const app = await createMockApp();
    const token = await createJWT(user);
    const project = await Project.create({
      name: "Test",
      minCountReviewers: 1,
      countDecisiveReviewers: 1,
      evaluationFormula: "",
      combinationOfReviewers: "",
      mergeThreshold: 0.8,
    });
    /*const userIsPartOfProject = */ await UserIsPartOfProject.create({
      isOwner: true,
      userId: Number(user.id),
      projectId: Number(project.id),
    });
    const ctx = await createMockContext(
      app,
      undefined,
      [["Content-Type", "text"]],
      "/",
      token,
    );
    const payLoad = await getPayloadFromJWTHeader(ctx);
    assertEquals(await checkPO(payLoad), true);
    await db.close();
    await client.end();
  },
});

Deno.test({
  name: "isNoPO",
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
    const app = await createMockApp();
    const token = await createJWT(user);
    const project = await Project.create({
      name: "Test",
      minCountReviewers: 1,
      countDecisiveReviewers: 1,
      evaluationFormula: "",
      combinationOfReviewers: "",
      mergeThreshold: 0.8,
    });
    /*const userIsPartOfProject = */ await UserIsPartOfProject.create({
      isOwner: false,
      userId: Number(user.id),
      projectId: Number(project.id),
    });
    const ctx = await createMockContext(
      app,
      undefined,
      [["Content-Type", "text"]],
      "/",
      token,
    );
    const payLoad = await getPayloadFromJWTHeader(ctx);
    assertEquals(await checkPO(payLoad), false);

    await db.close();
    await client.end();
  },
});
