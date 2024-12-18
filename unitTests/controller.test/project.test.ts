import { IDOfApi, setup } from "../../src/helper/setup.ts";
import { insertUser } from "../../src/controller/databaseFetcher/user.ts";
import { createMockApp } from "../mockObjects/oak/mockApp.test.ts";
import {
  checkMemberOfProject,
  createJWT,
} from "../../src/controller/validation.controller.ts";
import { createMockContext } from "../mockObjects/oak/mockContext.test.ts";
import {
  addCriteriaToProject,
  addCrtieriaEvalToReview,
  addMemberToProject,
  addPaperToProjectStage,
  addReviewToPaper,
  addStageToProject,
  authorCache,
  createProject,
  deleteCriteriaOfProject,
  deleteCritieriaEvalOfReview,
  deletePaperOfProjectStage,
  deleteReviewOfPaper,
  getCitationsOfProjectPaper,
  getCriteriaEvalsOfCriteria,
  getCriteriaOfProject,
  getCriteriasOfProject,
  getCritieriaEvalOfReview,
  getCrtieriaEvalsOfReview,
  getMembersOfProject,
  getPaperOfProjectStage,
  getPapersOfProjectStage,
  getProjects,
  getReferencesOfProjectPaper,
  getReviewOfPaper,
  getReviewsOfPaper,
  getStageCsv,
  makeRefCiteCsv,
  paperCache,
  patchCriteriaOfProject,
  patchCritieriaEvalOfReview,
  patchPaperOfProjectStage,
  patchReviewOfPaper,
  postCiteProject,
  postRefProject,
  removeMemberOfProject,
} from "../../src/controller/project.controller.ts";
import { Project } from "../../src/model/db/project.ts";
import {
  assertEquals,
  assertNotEquals,
} from "https://deno.land/std@0.150.0/testing/asserts.ts";
import { UserIsPartOfProject } from "../../src/model/db/userIsPartOfProject.ts";
import {
  client,
  db,
  saveChildren,
} from "../../src/controller/database.controller.ts";
import { Stage } from "../../src/model/db/stage.ts";
import { Batcher } from "../../src/controller/fetch.controller.ts";
import { getPaperCitations } from "../../src/controller/paper.controller.ts";
import { Paper } from "../../src/model/db/paper.ts";
import { Author } from "../../src/model/db/author.ts";
import { Wrote } from "../../src/model/db/wrote.ts";
import { PaperScopeForStage } from "../../src/model/db/paperScopeForStage.ts";
import { Criteria } from "../../src/model/db/criteria.ts";
import { CriteriaEvaluation } from "../../src/model/db/criteriaEval.ts";
import { Review } from "../../src/model/db/review.ts";
import { ProjectUsesApi } from "../../src/model/db/projectUsesApi.ts";

Deno.test({
  name: "createProjectUnauth",
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
    const ctx = await createMockContext(
      app,
      `{"name":"3", "minCountReviewers": 5, "countDecisiveReviewers":8}`,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );
    await createProject(ctx);
    assertEquals(ctx.response.status, 401);
    await db.close();
    await client.end();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "createProject",
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

    let projects = await Project.all();

    const size = projects.length;
    const name = "nameee";
    const minCount = 3;
    const countDecRev = 5;
    const comb = "3,8";
    const type = "both";
    const evalF = "-2,2";
    const tresh = 0.6;
    const ctx = await createMockContext(
      app,
      `{"name":"${name}", "minCountReviewers": ${minCount}, "countDecisiveReviewers":${countDecRev}, "combinationOfReviewers":"${comb}", "type": "${type}", "evaluationFormula": "${evalF}", "mergeThreshold": "${tresh}"}`,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );
    await createProject(ctx);
    projects = await Project.all();
    const answer = JSON.parse(ctx.response.body as string);
    assertEquals(ctx.response.status, 201);
    assertEquals(projects.length, size + 1);

    assertEquals(answer.name, name);
    assertEquals(answer.minCountReviewers, minCount);
    assertEquals(answer.combinationOfReviewers, comb);
    assertEquals(answer.countDecisiveReviewers, countDecRev);
    assertEquals(answer.type, type);
    assertEquals(answer.evaluationFormula, evalF);
    await db.close();
    await client.end();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "createProjectNaN",
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

    /*let projects = */ await Project.all();

    // const size = projects.length
    const name = "nameee";
    const minCount = 3;
    const countDecRev = "awfeg";
    const comb = "3,8";
    const type = "type";
    const ctx = await createMockContext(
      app,
      `{"name":"${name}", "minCountReviewers": ${minCount}, "countDecisiveReviewers":${countDecRev}, "combinationOfReviewers":${comb}, "type": "${type}"}`,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );
    await createProject(ctx);
    /*projects = */ await Project.all();
    assertEquals(ctx.response.status, 422);
    await db.close();
    await client.end();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});
Deno.test({
  name: "createProjectWrongCombinationOrder",
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

    /*let projects = */ await Project.all();

    // const size = projects.length
    const name = "nameee";
    const minCount = 3;
    const countDecRev = 5;
    const comb = "8,3";
    const type = "type";
    const ctx = await createMockContext(
      app,
      `{"name":"${name}", "minCountReviewers": ${minCount}, "countDecisiveReviewers":${countDecRev}, "combinationOfReviewers":"${comb}", "type": "${type}"}`,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );
    await createProject(ctx);
    /*projects = */ await Project.all();
    assertEquals(ctx.response.status, 409);
    await db.close();
    await client.end();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "createProjectWrongCombination",
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

    /*let projects = */ await Project.all();

    // const size = projects.length
    const name = "nameee";
    const minCount = 3;
    const countDecRev = 5;
    const comb = "2";
    const type = "type";
    const ctx = await createMockContext(
      app,
      `{"name":"${name}", "minCountReviewers": ${minCount}, "countDecisiveReviewers":${countDecRev}, "combinationOfReviewers":${comb}, "type": "${type}"}`,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );
    await createProject(ctx);
    /*projects = */ await Project.all();
    assertEquals(ctx.response.status, 409);
    await db.close();
    await client.end();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "createProjectNoParams",
  async fn(): Promise<void> {
    await setup(true);
    const user = await insertUser(
      "test123@test",
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
    await createProject(ctx);
    assertEquals(ctx.response.status, 422);
    await db.close();
    await client.end();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "createProjectNoName",
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

    let projects = await Project.all();

    const size = projects.length;

    const ctx = await createMockContext(
      app,
      `{"minCountReviewers": 3, "countDecisiveReviewers":5}`,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );
    await createProject(ctx);
    projects = await Project.all();
    assertEquals(projects.length, size);
    assertEquals(ctx.response.status, 422);
    await db.close();
    await client.end();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "getProject",
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
    await Project.create({
      name: "test",
      minCountReviewers: 1,
      countDecisiveReviewers: 1,
      combinationOfReviewers: 1,
      evaluationFormula: "",
      type: "",
      mergeThreshold: 0.8,
    });
    const app = await createMockApp();
    const token = await createJWT(user);
    const ctx = await createMockContext(
      app,
      `{}`,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );
    await getProjects(ctx);

    const answer = JSON.parse(ctx.response.body as string);
    assertNotEquals(answer.projects.length, 0);

    await db.close();
    await client.end();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "getProjectUnauthorized",
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
    await Project.create({
      name: "test",
      minCountReviewers: 1,
      countDecisiveReviewers: 1,
      combinationOfReviewers: 1,
      evaluationFormula: "",
      type: "",
      mergeThreshold: 0.8,
    });
    const app = await createMockApp();
    const token = await createJWT(user);
    const ctx = await createMockContext(
      app,
      `{}`,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );
    await getProjects(ctx);

    assertEquals(ctx.response.status, 401);

    await db.close();
    await client.end();
  },

  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "getMembersAsMember",
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
    const project = await Project.create({
      name: "test",
      minCountReviewers: 1,
      countDecisiveReviewers: 1,
      combinationOfReviewers: 1,
      evaluationFormula: "",
      type: "",
      mergeThreshold: 0.8,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.crossRef,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.openCitations,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.googleScholar,
      inUse: false,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.IEEE,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.semanticScholar,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.microsoftAcademic,
      inUse: true,
    });

    await UserIsPartOfProject.create({
      projectId: Number(project.id),
      userId: Number(user.id),
      isOwner: false,
    });
    const app = await createMockApp();
    const token = await createJWT(user);
    const ctx = await createMockContext(
      app,
      `{}`,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );
    await getMembersOfProject(ctx, Number(project.id));

    const answer = JSON.parse(ctx.response.body as string);
    assertNotEquals(answer.members.length, 0);

    await db.close();
    await client.end();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "Remove Member Of Project",
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
    const project = await Project.create({
      name: "test",
      minCountReviewers: 1,
      countDecisiveReviewers: 1,
      combinationOfReviewers: 1,
      evaluationFormula: "",
      type: "",
      mergeThreshold: 0.8,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.crossRef,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.openCitations,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.googleScholar,
      inUse: false,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.IEEE,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.semanticScholar,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.microsoftAcademic,
      inUse: true,
    });

    const id = Number(
      (await UserIsPartOfProject.create({
        projectId: Number(project.id),
        userId: Number(user.id),
        isOwner: false,
      })).id,
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
    await removeMemberOfProject(ctx, Number(project.id), Number(user.id));
    assertEquals(ctx.response.status, 200);
    const upp = await UserIsPartOfProject.find(id);
    assertEquals(upp, undefined);

    await db.close();
    await client.end();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "getMembersUnauthorized",
  async fn(): Promise<void> {
    await setup(true);
    const user = await insertUser(
      "test523423424@test",
      "ash",
      false,
      "Test",
      "Tester",
      "active",
    );
    const project = await Project.create({
      name: "test",
      minCountReviewers: 1,
      countDecisiveReviewers: 1,
      combinationOfReviewers: 1,
      evaluationFormula: "",
      type: "",
      mergeThreshold: 0.8,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.crossRef,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.openCitations,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.googleScholar,
      inUse: false,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.IEEE,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.semanticScholar,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.microsoftAcademic,
      inUse: true,
    });

    const app = await createMockApp();
    const token = await createJWT(user);
    const ctx = await createMockContext(
      app,
      `{}`,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );
    await getMembersOfProject(ctx, Number(project.id));

    assertEquals(ctx.response.status, 401);

    await db.close();
    await client.end();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});
Deno.test({
  name: "addMember",
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
    const user2 = await insertUser(
      "test2@test",
      "ash2",
      false,
      "Test2",
      "Tester2",
      "active",
    );
    const project = await Project.create({
      name: "test",
      minCountReviewers: 1,
      countDecisiveReviewers: 1,
      combinationOfReviewers: 1,
      evaluationFormula: "",
      type: "",
      mergeThreshold: 0.8,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.crossRef,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.openCitations,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.googleScholar,
      inUse: false,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.IEEE,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.semanticScholar,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.microsoftAcademic,
      inUse: true,
    });

    const app = await createMockApp();
    const token = await createJWT(user);
    const ctx = await createMockContext(
      app,
      `{ "id": ${Number(user2.id)}}`,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );
    await addMemberToProject(ctx, Number(project.id));
    const worked = await checkMemberOfProject(Number(project.id), {
      id: Number(user2.id),
      isAdmin: false,
      email: "fesffer",
      firstName: "",
      lastName: "",
      status: "active",
    });
    assertEquals(worked, true);

    await db.close();
    await client.end();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "addMemberAsPO",
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
    const user2 = await insertUser(
      "test2@test",
      "ash2",
      false,
      "Test2",
      "Tester2",
      "active",
    );
    const project = await Project.create({
      name: "test",
      minCountReviewers: 1,
      countDecisiveReviewers: 1,
      combinationOfReviewers: 1,
      evaluationFormula: "",
      type: "",
      mergeThreshold: 0.8,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.crossRef,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.openCitations,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.googleScholar,
      inUse: false,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.IEEE,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.semanticScholar,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.microsoftAcademic,
      inUse: true,
    });

    /*const bla = */ await UserIsPartOfProject.create({
      projectId: Number(project.id),
      userId: Number(user.id),
      isOwner: true,
    });
    const app = await createMockApp();
    const token = await createJWT(user);
    const ctx = await createMockContext(
      app,
      `{ "id": "${Number(user2.id)}", "isOwner": true}`,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );
    await addMemberToProject(ctx, Number(project.id));
    const worked = await checkMemberOfProject(Number(project.id), {
      id: Number(user2.id),
      isAdmin: false,
      email: "fesffer",
      firstName: "",
      lastName: "",
      status: "active",
    });
    assertEquals(worked, true);

    await db.close();
    await client.end();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});
Deno.test({
  name: "addMemberWrongID",
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
    const user2 = await insertUser(
      "test2@test",
      "ash2",
      false,
      "Test2",
      "Tester2",
      "active",
    );
    const project = await Project.create({
      name: "test",
      minCountReviewers: 1,
      countDecisiveReviewers: 1,
      combinationOfReviewers: 1,
      evaluationFormula: "",
      type: "",
      mergeThreshold: 0.8,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.crossRef,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.openCitations,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.googleScholar,
      inUse: false,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.IEEE,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.semanticScholar,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.microsoftAcademic,
      inUse: true,
    });

    /*const bla = */ await UserIsPartOfProject.create({
      projectId: Number(project.id),
      userId: Number(user.id),
      isOwner: true,
    });
    const app = await createMockApp();
    const token = await createJWT(user);
    const ctx = await createMockContext(
      app,
      `{ "id": "${Number(user2.id) + 1}", "isOwner": true}`,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );
    await addMemberToProject(ctx, Number(project.id));
    assertEquals(ctx.response.status, 404);

    await db.close();
    await client.end();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "addMemberUnauthorized",
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
    /*const user2 = */ await insertUser(
      "test2@test",
      "ash2",
      false,
      "Test2",
      "Tester2",
      "active",
    );
    const project = await Project.create({
      name: "test",
      minCountReviewers: 1,
      countDecisiveReviewers: 1,
      combinationOfReviewers: 1,
      evaluationFormula: "",
      type: "",
      mergeThreshold: 0.8,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.crossRef,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.openCitations,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.googleScholar,
      inUse: false,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.IEEE,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.semanticScholar,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.microsoftAcademic,
      inUse: true,
    });

    await UserIsPartOfProject.create({
      projectId: Number(project.id),
      userId: Number(user.id),
      isOwner: false,
    });
    const app = await createMockApp();
    const token = await createJWT(user);
    const ctx = await createMockContext(
      app,
      `{ "id": {Number(user2.id)}`,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );
    await addMemberToProject(ctx, Number(project.id));

    assertEquals(ctx.response.status, 401);

    await db.close();
    await client.end();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});
Deno.test({
  name: "addStage",
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
    const project = await Project.create({
      name: "test",
      minCountReviewers: 1,
      countDecisiveReviewers: 1,
      combinationOfReviewers: 1,
      evaluationFormula: "",
      type: "",
      mergeThreshold: 0.8,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.crossRef,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.openCitations,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.googleScholar,
      inUse: false,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.IEEE,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.semanticScholar,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.microsoftAcademic,
      inUse: true,
    });

    await UserIsPartOfProject.create({
      projectId: Number(project.id),
      userId: Number(user.id),
      isOwner: false,
    });
    const app = await createMockApp();
    const token = await createJWT(user);
    const ctx = await createMockContext(
      app,
      `{ "name": "namee"}`,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );
    await addStageToProject(ctx, Number(project.id));

    assertEquals(ctx.response.status, 201);

    await db.close();
    await client.end();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "addTwoStages",
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
    const project = await Project.create({
      name: "test",
      minCountReviewers: 1,
      countDecisiveReviewers: 1,
      combinationOfReviewers: 1,
      evaluationFormula: "",
      type: "",
      mergeThreshold: 0.8,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.crossRef,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.openCitations,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.googleScholar,
      inUse: false,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.IEEE,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.semanticScholar,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.microsoftAcademic,
      inUse: true,
    });

    await UserIsPartOfProject.create({
      projectId: Number(project.id),
      userId: Number(user.id),
      isOwner: false,
    });
    const app = await createMockApp();
    const token = await createJWT(user);
    let ctx = await createMockContext(
      app,
      `{ "name": "namee"}`,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );
    await addStageToProject(ctx, Number(project.id));
    let answer = JSON.parse(ctx.response.body as string);
    assertEquals(answer.number, 0);
    ctx = await createMockContext(
      app,
      `{ "name": "namee2"}`,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );
    await addStageToProject(ctx, Number(project.id));
    answer = JSON.parse(ctx.response.body as string);
    assertEquals(answer.number, 1);
    assertEquals(ctx.response.status, 201);

    await db.close();
    await client.end();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "addStageUnauthorized",
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
    const project = await Project.create({
      name: "test",
      minCountReviewers: 1,
      countDecisiveReviewers: 1,
      combinationOfReviewers: 1,
      evaluationFormula: "",
      type: "",
      mergeThreshold: 0.8,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.crossRef,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.openCitations,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.googleScholar,
      inUse: false,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.IEEE,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.semanticScholar,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.microsoftAcademic,
      inUse: true,
    });

    await UserIsPartOfProject.create({
      projectId: Number(project.id),
      userId: Number(user.id),
      isOwner: false,
    });
    const app = await createMockApp();
    const token = await createJWT(user);
    const ctx = await createMockContext(
      app,
      `{ "name": "namee"}`,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );
    await addStageToProject(ctx, Number(project.id));

    assertEquals(ctx.response.status, 401);

    await db.close();
    await client.end();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "AddPaperToProject",
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
    const project = await Project.create({
      name: "test",
      minCountReviewers: 1,
      countDecisiveReviewers: 1,
      combinationOfReviewers: 1,
      evaluationFormula: "",
      type: "",
      mergeThreshold: 0.8,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.crossRef,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.openCitations,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.googleScholar,
      inUse: false,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.IEEE,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.semanticScholar,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.microsoftAcademic,
      inUse: true,
    });

    const stage = await Stage.create({
      name: "TestStage",
      projectId: Number(project.id),
      number: 0,
    });
    const app = await createMockApp();
    const token = await createJWT(user);
    const ctx = await createMockContext(
      app,
      `{"doi":"10.1109/SEAA.2009.60" }`,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );
    /*const bla = */ await addPaperToProjectStage(
      ctx,
      Number(project.id),
      Number(stage.id),
      true,
    );

    assertEquals(ctx.response.status, 201);

    for (let i = 1; i < 30; i++) {
      await getPaperCitations(ctx, i);
    }
    paperCache.clear();
    authorCache.clear();
    Batcher.kill();
    await db.close();
    await client.end();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "AddPaperToProjectPaperAlreadyExists",
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
    const project = await Project.create({
      name: "test",
      minCountReviewers: 1,
      countDecisiveReviewers: 1,
      combinationOfReviewers: 1,
      evaluationFormula: "",
      type: "",
      mergeThreshold: 0.8,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.crossRef,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.openCitations,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.googleScholar,
      inUse: false,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.IEEE,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.semanticScholar,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.microsoftAcademic,
      inUse: true,
    });

    const stage = await Stage.create({
      name: "TestStage",
      projectId: Number(project.id),
      number: 0,
    });
    let paper = await Paper.create({
      doi: "10.1109/seaa.2009.60",
    });
    const app = await createMockApp();
    const token = await createJWT(user);
    const ctx = await createMockContext(
      app,
      `{"doi":"10.1109/SEAA.2009.60" }`,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );
    /*const bla = */ await addPaperToProjectStage(
      ctx,
      Number(project.id),
      Number(stage.id),
      true,
    );

    assertEquals(ctx.response.status, 201);
    paper = await Paper.find(Number(paper.id));
    assertNotEquals(paper.title, null);
    paperCache.clear();
    authorCache.clear();
    Batcher.kill();
    await db.close();
    await client.end();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "AddPaperToProjectNoTitleOfDoi",
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
    const project = await Project.create({
      name: "test",
      minCountReviewers: 1,
      countDecisiveReviewers: 1,
      combinationOfReviewers: 1,
      type: "",
      evaluationFormula: "",
      mergeThreshold: 0.8,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.crossRef,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.openCitations,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.googleScholar,
      inUse: false,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.IEEE,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.semanticScholar,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.microsoftAcademic,
      inUse: true,
    });

    const stage = await Stage.create({
      name: "TestStage",
      projectId: Number(project.id),
      number: 0,
    });
    const app = await createMockApp();
    const token = await createJWT(user);
    const ctx = await createMockContext(
      app,
      `{}`,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );
    /*const bla = */ await addPaperToProjectStage(
      ctx,
      Number(project.id),
      Number(stage.id),
      true,
    );

    assertEquals(ctx.response.status, 422);

    paperCache.clear();
    authorCache.clear();
    Batcher.kill();
    await db.close();
    await client.end();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "RefetchPaperOfProject",
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
    const project = await Project.create({
      name: "test",
      minCountReviewers: 1,
      countDecisiveReviewers: 1,
      combinationOfReviewers: 1,
      evaluationFormula: "",
      type: "",
      mergeThreshold: 0.8,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.crossRef,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.openCitations,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.googleScholar,
      inUse: false,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.IEEE,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.semanticScholar,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.microsoftAcademic,
      inUse: true,
    });

    const stage = await Stage.create({
      name: "TestStage",
      projectId: Number(project.id),
      number: 0,
    });
    const app = await createMockApp();
    const token = await createJWT(user);
    const ctx = await createMockContext(
      app,
      `{"doi":"10.1109/SEAA.2009.60" }`,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );
    /*const bla = */ await addPaperToProjectStage(
      ctx,
      Number(project.id),
      Number(stage.id),
      true,
    );

    assertEquals(ctx.response.status, 201);

    for (let i = 1; i < 30; i++) {
      await getPaperCitations(ctx, i);
    }
    paperCache.clear();
    authorCache.clear();
    Batcher.kill();
    await db.close();
    await client.end();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "GetPapersOfProjectStage",
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
    const project = await Project.create({
      name: "test",
      minCountReviewers: 1,
      countDecisiveReviewers: 1,
      combinationOfReviewers: 1,
      type: "",
      evaluationFormula: "",
      mergeThreshold: 0.8,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.crossRef,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.openCitations,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.googleScholar,
      inUse: false,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.IEEE,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.semanticScholar,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.microsoftAcademic,
      inUse: true,
    });

    const stage = await Stage.create({
      name: "TestStage",
      projectId: Number(project.id),
      number: 0,
    });
    const paper = await Paper.create({
      doi: "fasfafraf",
      title: "nice title",
      abstract: "hello there",
    });
    const paper2 = await Paper.create({
      doi: "fasfd",
      title: "nicer title",
      abstract: "General Kenobi",
    });
    const author = await Author.create({
      rawString: "Awsome Name",
    });
    await PaperScopeForStage.create({
      paperId: Number(paper.id),
      stageId: Number(stage.id),
    });
    await PaperScopeForStage.create({
      paperId: Number(paper2.id),
      stageId: Number(stage.id),
    });
    /*const wrote = */ await Wrote.create({
      authorId: Number(author.id),
      paperId: Number(paper.id),
    });
    const app = await createMockApp();
    const token = await createJWT(user);
    const ctx = await createMockContext(
      app,
      `{"doi":"10.1109/SEAA.2009.60" }`,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );
    /*const bla = */ await getPapersOfProjectStage(
      ctx,
      Number(project.id),
      Number(stage.id),
    );

    const answer = JSON.parse(ctx.response.body as string);
    assertEquals(Array.isArray(answer.papers), true);
    assertEquals(answer.papers[0].title, "nice title");
    assertEquals(answer.papers[1].title, "nicer title");
    paperCache.clear();
    authorCache.clear();
    Batcher.kill();
    await db.close();
    await client.end();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "GetPaperOfProjectStage",
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
    const project = await Project.create({
      name: "test",
      minCountReviewers: 1,
      countDecisiveReviewers: 1,
      combinationOfReviewers: 1,
      evaluationFormula: "",
      type: "",
      mergeThreshold: 0.8,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.crossRef,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.openCitations,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.googleScholar,
      inUse: false,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.IEEE,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.semanticScholar,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.microsoftAcademic,
      inUse: true,
    });

    const stage = await Stage.create({
      name: "TestStage",
      projectId: Number(project.id),
      number: 0,
    });
    const paper = await Paper.create({
      doi: "fasfafraf",
      title: "nice title",
      abstract: "hellu",
    });
    const author = await Author.create({
      rawString: "Awsome Name",
    });
    const pp = await PaperScopeForStage.create({
      paperId: Number(paper.id),
      stageId: Number(stage.id),
    });
    /*const wrote = */ await Wrote.create({
      authorId: Number(author.id),
      paperId: Number(paper.id),
    });
    const app = await createMockApp();
    const token = await createJWT(user);
    const ctx = await createMockContext(
      app,
      `{"doi":"10.1109/SEAA.2009.60" }`,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );
    /*const bla = */ await getPaperOfProjectStage(
      ctx,
      Number(project.id),
      Number(stage.id),
      Number(pp.id),
    );

    const answer = JSON.parse(ctx.response.body as string);
    assertEquals(answer.title, "nice title");
    assertEquals(answer.abstract, "hellu");
    assertEquals(answer.authors[0].rawString, "Awsome Name");
    paperCache.clear();
    authorCache.clear();
    Batcher.kill();
    await db.close();
    await client.end();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "GetPaperOfProjectStageWrongId",
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
    const project = await Project.create({
      name: "test",
      minCountReviewers: 1,
      countDecisiveReviewers: 1,
      combinationOfReviewers: 1,
      evaluationFormula: "",
      type: "",
      mergeThreshold: 0.8,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.crossRef,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.openCitations,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.googleScholar,
      inUse: false,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.IEEE,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.semanticScholar,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.microsoftAcademic,
      inUse: true,
    });

    const stage = await Stage.create({
      name: "TestStage",
      projectId: Number(project.id),
      number: 0,
    });
    const paper = await Paper.create({
      doi: "fasfafraf",
      title: "nice title",
      abstract: "hellu",
    });
    const author = await Author.create({
      rawString: "Awsome Name",
    });
    const pp = await PaperScopeForStage.create({
      paperId: Number(paper.id),
      stageId: Number(stage.id),
    });
    /*const wrote = */ await Wrote.create({
      authorId: Number(author.id),
      paperId: Number(paper.id),
    });
    const app = await createMockApp();
    const token = await createJWT(user);
    const ctx = await createMockContext(
      app,
      `{"doi":"10.1109/SEAA.2009.60" }`,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );
    /*const bla = */ await getPaperOfProjectStage(
      ctx,
      Number(project.id),
      Number(stage.id),
      Number(pp.id) + 1,
    );

    assertEquals(ctx.response.status, 404);
    paperCache.clear();
    authorCache.clear();
    Batcher.kill();
    await db.close();
    await client.end();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "GetPaperOfProjectStage#unauth",
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
    const project = await Project.create({
      name: "test",
      minCountReviewers: 1,
      countDecisiveReviewers: 1,
      combinationOfReviewers: 1,
      evaluationFormula: "",
      type: "",
      mergeThreshold: 0.8,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.crossRef,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.openCitations,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.googleScholar,
      inUse: false,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.IEEE,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.semanticScholar,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.microsoftAcademic,
      inUse: true,
    });

    const stage = await Stage.create({
      name: "TestStage",
      projectId: Number(project.id),
      number: 0,
    });
    const paper = await Paper.create({
      doi: "fasfafraf",
      title: "nice title",
      abstract: "hellu",
    });
    const author = await Author.create({
      rawString: "Awsome Name",
    });
    const pp = await PaperScopeForStage.create({
      paperId: Number(paper.id),
      stageId: Number(stage.id),
    });
    /*const wrote = */ await Wrote.create({
      authorId: Number(author.id),
      paperId: Number(paper.id),
    });
    const app = await createMockApp();
    const token = await createJWT(user);
    const ctx = await createMockContext(
      app,
      `{"doi":"10.1109/SEAA.2009.60" }`,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );
    /*const bla = */ await getPaperOfProjectStage(
      ctx,
      Number(project.id),
      Number(stage.id),
      Number(pp.id),
    );

    assertEquals(ctx.response.status, 401);
    paperCache.clear();
    authorCache.clear();
    Batcher.kill();
    await db.close();
    await client.end();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "GetPapersOfProjectStage#unauth",
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
    const project = await Project.create({
      name: "test",
      minCountReviewers: 1,
      countDecisiveReviewers: 1,
      combinationOfReviewers: 1,
      evaluationFormula: "",
      type: "",
      mergeThreshold: 0.8,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.crossRef,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.openCitations,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.googleScholar,
      inUse: false,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.IEEE,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.semanticScholar,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.microsoftAcademic,
      inUse: true,
    });

    const stage = await Stage.create({
      name: "TestStage",
      projectId: Number(project.id),
      number: 0,
    });
    const paper = await Paper.create({
      doi: "fasfafraf",
      title: "nice title",
      abstract: "hellu",
    });
    const author = await Author.create({
      rawString: "Awsome Name",
    });
    /*const wrote = */ await Wrote.create({
      authorId: Number(author.id),
      paperId: Number(paper.id),
    });
    await PaperScopeForStage.create({
      paperId: Number(paper.id),
      stageId: Number(stage.id),
    });
    const app = await createMockApp();
    const token = await createJWT(user);
    const ctx = await createMockContext(
      app,
      `{"doi":"10.1109/SEAA.2009.60" }`,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );
    /*const bla = */ await getPapersOfProjectStage(
      ctx,
      Number(project.id),
      Number(stage.id),
    );

    assertEquals(ctx.response.status, 401);
    paperCache.clear();
    authorCache.clear();
    Batcher.kill();
    await db.close();
    await client.end();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "PatchPaperOfProjectStage",
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
    const project = await Project.create({
      name: "test",
      minCountReviewers: 1,
      countDecisiveReviewers: 1,
      combinationOfReviewers: 1,
      evaluationFormula: "",
      type: "",
      mergeThreshold: 0.8,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.crossRef,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.openCitations,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.googleScholar,
      inUse: false,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.IEEE,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.semanticScholar,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.microsoftAcademic,
      inUse: true,
    });

    const stage = await Stage.create({
      name: "TestStage",
      projectId: Number(project.id),
      number: 0,
    });
    const paper = await Paper.create({
      doi: "fasfafraf",
      title: "nice title",
      abstract: "hellu",
    });
    const author = await Author.create({
      rawString: "Awsome Name",
    });
    const pp = await PaperScopeForStage.create({
      paperId: Number(paper.id),
      stageId: Number(stage.id),
    });
    /*const wrote = */ await Wrote.create({
      authorId: Number(author.id),
      paperId: Number(paper.id),
    });
    const app = await createMockApp();
    const token = await createJWT(user);
    const ctx = await createMockContext(
      app,
      `{"title":"New Title" }`,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );
    /*const bla = */ await patchPaperOfProjectStage(
      ctx,
      Number(project.id),
      Number(stage.id),
      Number(pp.id),
    );

    assertEquals(ctx.response.status, 200);
    const paperFinal = await Paper.find(Number(paper.id));
    assertEquals(paperFinal.title, "New Title");
    paperCache.clear();
    authorCache.clear();
    Batcher.kill();
    await db.close();
    await client.end();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "PatchPaperOfProjectStageFinalDecicion",
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
    const project = await Project.create({
      name: "test",
      minCountReviewers: 1,
      countDecisiveReviewers: 1,
      combinationOfReviewers: 1,
      evaluationFormula: "",
      type: "",
      mergeThreshold: 0.8,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.crossRef,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.openCitations,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.googleScholar,
      inUse: false,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.IEEE,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.semanticScholar,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.microsoftAcademic,
      inUse: true,
    });

    const stage = await Stage.create({
      name: "TestStage",
      projectId: Number(project.id),
      number: 0,
    });
    const paper = await Paper.create({
      doi: "fasfafraf",
      title: "nice title",
      abstract: "hellu",
    });
    const author = await Author.create({
      rawString: "Awsome Name",
    });
    const pp = await PaperScopeForStage.create({
      paperId: Number(paper.id),
      stageId: Number(stage.id),
    });
    /*const wrote = */ await Wrote.create({
      authorId: Number(author.id),
      paperId: Number(paper.id),
    });
    const app = await createMockApp();
    const token = await createJWT(user);
    const ctx = await createMockContext(
      app,
      `{"finalDecision":"YES" }`,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );
    /*const bla = */ await patchPaperOfProjectStage(
      ctx,
      Number(project.id),
      Number(stage.id),
      Number(pp.id),
    );

    assertEquals(ctx.response.status, 200);
    const paperFinal = await PaperScopeForStage.find(Number(pp.id));
    assertEquals(paperFinal.finalDecision, "YES");
    paperCache.clear();
    authorCache.clear();
    Batcher.kill();
    await db.close();
    await client.end();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "PatchPapersOfProjectStageUnAuthorized",
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
    const project = await Project.create({
      name: "test",
      minCountReviewers: 1,
      countDecisiveReviewers: 1,
      combinationOfReviewers: 1,
      evaluationFormula: "",
      type: "",
      mergeThreshold: 0.8,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.crossRef,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.openCitations,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.googleScholar,
      inUse: false,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.IEEE,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.semanticScholar,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.microsoftAcademic,
      inUse: true,
    });

    const stage = await Stage.create({
      name: "TestStage",
      projectId: Number(project.id),
      number: 0,
    });
    const paper = await Paper.create({
      doi: "fasfafraf",
      title: "nice title",
      abstract: "hellu",
    });
    const author = await Author.create({
      rawString: "Awsome Name",
    });
    await PaperScopeForStage.create({
      paperId: Number(paper.id),
      stageId: Number(stage.id),
    });
    const wrote = await Wrote.create({
      authorId: Number(author.id),
      paperId: Number(paper.id),
    });
    const app = await createMockApp();
    const token = await createJWT(user);
    const ctx = await createMockContext(
      app,
      `{"doi":"10.1109/SEAA.2009.60" }`,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );
    /*const bla = */ await patchPaperOfProjectStage(
      ctx,
      Number(project.id),
      Number(stage.id),
      Number(wrote.id),
    );

    assertEquals(ctx.response.status, 401);
    paperCache.clear();
    authorCache.clear();
    Batcher.kill();
    await db.close();
    await client.end();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "PatchPapersOfProjectStageNoPaper",
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
    const project = await Project.create({
      name: "test",
      minCountReviewers: 1,
      countDecisiveReviewers: 1,
      combinationOfReviewers: 1,
      evaluationFormula: "",
      type: "",
      mergeThreshold: 0.8,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.crossRef,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.openCitations,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.googleScholar,
      inUse: false,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.IEEE,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.semanticScholar,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.microsoftAcademic,
      inUse: true,
    });

    const stage = await Stage.create({
      name: "TestStage",
      projectId: Number(project.id),
      number: 0,
    });
    const paper = await Paper.create({
      doi: "fasfafraf",
      title: "nice title",
      abstract: "hellu",
    });
    const author = await Author.create({
      rawString: "Awsome Name",
    });
    const pp = await PaperScopeForStage.create({
      paperId: Number(paper.id),
      stageId: Number(stage.id),
    });
    /*const wrote = */ await Wrote.create({
      authorId: Number(author.id),
      paperId: Number(paper.id),
    });
    const app = await createMockApp();
    const token = await createJWT(user);
    const ctx = await createMockContext(
      app,
      `{"doi":"10.1109/SEAA.2009.60" }`,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );
    /*const bla = */ await patchPaperOfProjectStage(
      ctx,
      Number(project.id),
      Number(stage.id),
      Number(pp.id) + 1,
    );

    assertEquals(ctx.response.status, 404);
    paperCache.clear();
    authorCache.clear();
    Batcher.kill();
    await db.close();
    await client.end();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "deletePaperOfProjectStage",
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
    const project = await Project.create({
      name: "test",
      minCountReviewers: 1,
      countDecisiveReviewers: 1,
      combinationOfReviewers: 1,
      evaluationFormula: "",
      type: "",
      mergeThreshold: 0.8,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.crossRef,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.openCitations,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.googleScholar,
      inUse: false,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.IEEE,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.semanticScholar,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.microsoftAcademic,
      inUse: true,
    });

    const stage = await Stage.create({
      name: "TestStage",
      projectId: Number(project.id),
      number: 0,
    });
    const paper = await Paper.create({
      doi: "fasfafraf",
      title: "nice title",
      abstract: "hellu",
    });

    const pp = await PaperScopeForStage.create({
      paperId: Number(paper.id),
      stageId: Number(stage.id),
    });
    const ppid = Number(pp.id);
    const app = await createMockApp();
    const token = await createJWT(user);
    const ctx = await createMockContext(
      app,
      `{"doi":"10.1109/SEAA.2009.60" }`,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );
    await deletePaperOfProjectStage(
      ctx,
      Number(project.id),
      Number(stage.id),
      Number(pp.id),
    );

    assertEquals(ctx.response.status, 200);
    assertEquals(await PaperScopeForStage.find(ppid), undefined);
    paperCache.clear();
    authorCache.clear();
    Batcher.kill();
    await db.close();
    await client.end();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});
Deno.test({
  name: "getRefs",
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
      ``,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );
    const paper = await Paper.create({
      title: "Hello there",
      abstract: "General Kenobi",
    });
    const paper1 = await Paper.create({
      title: "Hi",
      abstract: "this abstract",
    });
    const paper2 = await Paper.create({ title: "mlem", abstract: "mlem mlem" });
    const project = await Project.create({
      name: "test",
      minCountReviewers: 1,
      countDecisiveReviewers: 1,
      combinationOfReviewers: "3,8",
      evaluationFormula: "",
      type: "",
      mergeThreshold: 0.8,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.crossRef,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.openCitations,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.googleScholar,
      inUse: false,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.IEEE,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.semanticScholar,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.microsoftAcademic,
      inUse: true,
    });

    const stage = await Stage.create({
      name: "TestStage",
      projectId: Number(project.id),
      number: 0,
    });
    const stage2 = await Stage.create({
      name: "TestStage",
      projectId: Number(project.id),
      number: 1,
    });
    const pp = await PaperScopeForStage.create({
      paperId: Number(paper.id),
      stageId: Number(stage.id),
    });
    /*const pp1 = */ await PaperScopeForStage.create({
      paperId: Number(paper1.id),
      stageId: Number(stage2.id),
    });
    await saveChildren(
      "referencedby",
      "paperreferencedid",
      "paperreferencingid",
      Number(paper.id),
      Number(paper1.id),
    );
    await saveChildren(
      "referencedby",
      "paperreferencedid",
      "paperreferencingid",
      Number(paper.id),
      Number(paper2.id),
    );

    await getReferencesOfProjectPaper(
      ctx,
      Number(project.id),
      Number(stage.id),
      Number(pp.id),
    );
    assertEquals(ctx.response.status, 200);
    const answer = JSON.parse(ctx.response.body as string);
    assertEquals(answer.papers.length, 1);
    assertEquals(answer.papers[0].title, "Hi");
    assertEquals(answer.papers[0].abstract, "this abstract");
    assertEquals(answer.papers[0].id, Number(paper1.id));
    await db.close();
    await client.end();
    Batcher.kill();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "getCites",
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
      ``,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );
    const paper = await Paper.create({
      title: "Hello there",
      abstract: "General Kenobi",
    });
    const paper1 = await Paper.create({
      title: "Hi",
      abstract: "this abstract",
    });
    const paper2 = await Paper.create({ title: "mlem", abstract: "mlem mlem" });
    const project = await Project.create({
      name: "test",
      minCountReviewers: 1,
      countDecisiveReviewers: 1,
      combinationOfReviewers: "3,8",
      evaluationFormula: "",
      type: "",
      mergeThreshold: 0.8,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.crossRef,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.openCitations,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.googleScholar,
      inUse: false,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.IEEE,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.semanticScholar,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.microsoftAcademic,
      inUse: true,
    });

    const stage = await Stage.create({
      name: "TestStage",
      projectId: Number(project.id),
      number: 0,
    });
    const stage2 = await Stage.create({
      name: "TestStage",
      projectId: Number(project.id),
      number: 1,
    });
    const pp = await PaperScopeForStage.create({
      paperId: Number(paper.id),
      stageId: Number(stage.id),
    });
    /*const pp1 = */ await PaperScopeForStage.create({
      paperId: Number(paper1.id),
      stageId: Number(stage2.id),
    });
    await saveChildren(
      "citedby",
      "papercitingid",
      "papercitedid",
      Number(paper.id),
      Number(paper1.id),
    );
    await saveChildren(
      "citedby",
      "papercitingid",
      "papercitedid",
      Number(paper.id),
      Number(paper2.id),
    );

    await getCitationsOfProjectPaper(
      ctx,
      Number(project.id),
      Number(stage.id),
      Number(pp.id),
    );
    const answer = JSON.parse(ctx.response.body as string);
    assertEquals(ctx.response.status, 200);
    assertEquals(answer.papers.length, 1);
    assertEquals(answer.papers[0].title, "Hi");
    assertEquals(answer.papers[0].abstract, "this abstract");
    assertEquals(answer.papers[0].id, Number(paper1.id));
    await db.close();
    await client.end();
    Batcher.kill();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "makeRefCiteCsv",
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
      ``,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );
    const paper = await Paper.create({
      title: "Hello there",
      abstract: "General Kenobi",
    });
    const paper1 = await Paper.create({
      title: "Hi",
      abstract: "this abstract",
    });
    const paper2 = await Paper.create({ title: "mlem", abstract: "mlem mlem" });
    const paper3 = await Paper.create({
      title: "Hey",
      abstract: "other abstract",
    });
    const paper4 = await Paper.create({ title: "miau", abstract: "mlem" });
    const project = await Project.create({
      name: "test",
      minCountReviewers: 1,
      countDecisiveReviewers: 1,
      combinationOfReviewers: "3,8",
      evaluationFormula: "",
      type: "",
      mergeThreshold: 0.8,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.crossRef,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.openCitations,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.googleScholar,
      inUse: false,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.IEEE,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.semanticScholar,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.microsoftAcademic,
      inUse: true,
    });

    const stage = await Stage.create({
      name: "TestStage",
      projectId: Number(project.id),
      number: 0,
    });
    const stage2 = await Stage.create({
      name: "TestStage",
      projectId: Number(project.id),
      number: 1,
    });
    const pp = await PaperScopeForStage.create({
      paperId: Number(paper.id),
      stageId: Number(stage.id),
    });
    /*const pp1 = */ await PaperScopeForStage.create({
      paperId: Number(paper1.id),
      stageId: Number(stage2.id),
    });

    /*const pp2 = */ await PaperScopeForStage.create({
      paperId: Number(paper2.id),
      stageId: Number(stage2.id),
    });
    /*const pp3 = */ await PaperScopeForStage.create({
      paperId: Number(paper3.id),
      stageId: Number(stage2.id),
    });

    /*const pp4 = */ await PaperScopeForStage.create({
      paperId: Number(paper4.id),
      stageId: Number(stage2.id),
    });
    await saveChildren(
      "citedby",
      "papercitingid",
      "papercitedid",
      Number(paper.id),
      Number(paper1.id),
    );
    await saveChildren(
      "citedby",
      "papercitingid",
      "papercitedid",
      Number(paper.id),
      Number(paper2.id),
    );

    await saveChildren(
      "referencedby",
      "paperreferencedid",
      "paperreferencingid",
      Number(paper.id),
      Number(paper3.id),
    );
    await saveChildren(
      "referencedby",
      "paperreferencedid",
      "paperreferencingid",
      Number(paper.id),
      Number(paper4.id),
    );

    await makeRefCiteCsv(
      ctx,
      Number(project.id),
      Number(stage.id),
      Number(pp.id),
    );
    assertEquals(ctx.response.status, 200);
    const answer = ctx.response.body as string;
    assertEquals(answer.includes("References"), true);
    assertEquals(answer.includes("Citations"), true);
    assertEquals(answer.includes("mlem"), true);
    const lengthTest = answer.split(/\r?\n/).length >= 10;
    assertEquals(lengthTest, true);
    await db.close();
    await client.end();
    Batcher.kill();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "makeStageCsv",
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
      ``,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );
    const paper = await Paper.create({
      title: "Hello there",
      abstract: "General Kenobi",
    });
    const paper1 = await Paper.create({
      title: "Hi",
      abstract: "this abstract",
    });
    /*const paper2 = */ await Paper.create({
      title: "mlem",
      abstract: "mlem mlem",
    });
    const paper3 = await Paper.create({
      title: "Hey",
      abstract: "other abstract",
    });
    const paper4 = await Paper.create({ title: "miau", abstract: "mlem" });
    const project = await Project.create({
      name: "test",
      minCountReviewers: 1,
      countDecisiveReviewers: 1,
      combinationOfReviewers: "3,8",
      evaluationFormula: "",
      type: "",
      mergeThreshold: 0.8,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.crossRef,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.openCitations,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.googleScholar,
      inUse: false,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.IEEE,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.semanticScholar,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.microsoftAcademic,
      inUse: true,
    });

    const stage = await Stage.create({
      name: "TestStage",
      projectId: Number(project.id),
      number: 0,
    });

    /*const pp = */ await PaperScopeForStage.create({
      paperId: Number(paper.id),
      stageId: Number(stage.id),
    });
    /*const pp1 = */ await PaperScopeForStage.create({
      paperId: Number(paper1.id),
      stageId: Number(stage.id),
    });

    /*const pp3 = */ await PaperScopeForStage.create({
      paperId: Number(paper3.id),
      stageId: Number(stage.id),
    });
    /*const pp4 = */ await PaperScopeForStage.create({
      paperId: Number(paper4.id),
      stageId: Number(stage.id),
    });

    await getStageCsv(ctx, Number(project.id), Number(stage.id));
    assertEquals(ctx.response.status, 200);
    const answer = ctx.response.body as string;
    assertEquals(answer.includes("miau"), true);
    const lengthTest = answer.split(/\r?\n/).length >= 5;
    assertEquals(lengthTest, true);
    await db.close();
    await client.end();
    Batcher.kill();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "postPaperProjectReferences",
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
    let ctx = await createMockContext(
      app,
      ``,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );
    const paper = await Paper.create({
      title: "Hello there",
      abstract: "General Kenobi",
    });
    const paper1 = await Paper.create({
      title: "Hi",
      abstract: "this abstract",
    });
    /*const paper2 = */ await Paper.create({
      title: "mlem",
      abstract: "mlem mlem",
    });
    const project = await Project.create({
      name: "test",
      minCountReviewers: 1,
      countDecisiveReviewers: 1,
      combinationOfReviewers: "3,8",
      evaluationFormula: "",
      type: "",
      mergeThreshold: 0.8,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.crossRef,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.openCitations,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.googleScholar,
      inUse: false,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.IEEE,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.semanticScholar,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.microsoftAcademic,
      inUse: true,
    });

    const stage = await Stage.create({
      name: "TestStage",
      projectId: Number(project.id),
      number: 0,
    });
    const pp = await PaperScopeForStage.create({
      paperId: Number(paper.id),
      stageId: Number(stage.id),
    });
    await getReferencesOfProjectPaper(
      ctx,
      Number(project.id),
      Number(stage.id),
      Number(pp.id),
    );
    let answer = JSON.parse(ctx.response.body as string);
    const length = answer.papers.length;
    ctx = await createMockContext(
      app,
      `{"id": ${paper1.id}}`,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );
    await postRefProject(
      ctx,
      Number(project.id),
      Number(stage.id),
      Number(pp.id),
    );

    await getReferencesOfProjectPaper(
      ctx,
      Number(project.id),
      Number(stage.id),
      Number(pp.id),
    );
    answer = JSON.parse(ctx.response.body as string);
    assertEquals(ctx.response.status, 200);
    assertEquals(answer.papers.length, length + 1);
    assertEquals(answer.papers[0].title, "Hi");
    assertEquals(answer.papers[0].abstract, "this abstract");
    assertEquals(answer.papers[0].id, Number(paper1.id));
    await db.close();
    await client.end();
    Batcher.kill();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "postPaperProjectCitations",
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
    let ctx = await createMockContext(
      app,
      ``,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );
    const paper = await Paper.create({
      title: "Hello there",
      abstract: "General Kenobi",
    });
    const paper1 = await Paper.create({
      title: "Hi",
      abstract: "this abstract",
    });
    /*const paper2 = */ await Paper.create({
      title: "mlem",
      abstract: "mlem mlem",
    });
    const project = await Project.create({
      name: "test",
      minCountReviewers: 1,
      countDecisiveReviewers: 1,
      combinationOfReviewers: "3,8",
      evaluationFormula: "",
      type: "",
      mergeThreshold: 0.8,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.crossRef,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.openCitations,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.googleScholar,
      inUse: false,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.IEEE,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.semanticScholar,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.microsoftAcademic,
      inUse: true,
    });

    const stage = await Stage.create({
      name: "TestStage",
      projectId: Number(project.id),
      number: 0,
    });
    const pp = await PaperScopeForStage.create({
      paperId: Number(paper.id),
      stageId: Number(stage.id),
    });
    await getCitationsOfProjectPaper(
      ctx,
      Number(project.id),
      Number(stage.id),
      Number(pp.id),
    );
    let answer = JSON.parse(ctx.response.body as string);
    const length = answer.papers.length;
    ctx = await createMockContext(
      app,
      `{"id": ${paper1.id}}`,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );
    await postCiteProject(
      ctx,
      Number(project.id),
      Number(stage.id),
      Number(pp.id),
    );

    await getCitationsOfProjectPaper(
      ctx,
      Number(project.id),
      Number(stage.id),
      Number(pp.id),
    );
    answer = JSON.parse(ctx.response.body as string);
    assertEquals(ctx.response.status, 200);
    assertEquals(answer.papers.length, length + 1);
    assertEquals(answer.papers[0].title, "Hi");
    assertEquals(answer.papers[0].abstract, "this abstract");
    assertEquals(answer.papers[0].id, Number(paper1.id));
    await db.close();
    await client.end();
    Batcher.kill();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "GetCriteriasOfProject",
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
      ``,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );

    const project = await Project.create({
      name: "test",
      minCountReviewers: 1,
      countDecisiveReviewers: 1,
      combinationOfReviewers: 1,
      evaluationFormula: "",
      type: "",
      mergeThreshold: 0.8,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.crossRef,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.openCitations,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.googleScholar,
      inUse: false,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.IEEE,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.semanticScholar,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.microsoftAcademic,
      inUse: true,
    });

    const criteria = await Criteria.create({
      projectId: Number(project.id),
      short: "FG",
      abbreviation: "An abbrevation",
      inclusionExclusion: "inclusion",
      weight: 3,
      description: "a description",
    });

    const criteria1 = await Criteria.create({
      projectId: Number(project.id),
      short: "AD",
      abbreviation: "Another abbrevation",
      inclusionExclusion: "exclusion",
      weight: 8,
      description: "another description",
    });
    const criteria2 = await Criteria.create({
      projectId: Number(project.id),
      short: "AG",
      abbreviation: "abbrevation",
      inclusionExclusion: "hard exclusion",
      weight: 4,
      description: "third description",
    });
    await getCriteriasOfProject(ctx, Number(project.id));
    const answer = JSON.parse(ctx.response.body as string);
    assertEquals(ctx.response.status, 200);
    assertEquals(answer.criterias.length, 3);
    assertEquals(answer.criterias[0].short, String(criteria.short));
    assertEquals(answer.criterias[0].description, String(criteria.description));
    assertEquals(
      answer.criterias[0].abbreviation,
      String(criteria.abbreviation),
    );
    assertEquals(
      answer.criterias[0].inclusionExclusion,
      String(criteria.inclusionExclusion),
    );
    assertEquals(answer.criterias[0].weight, Number(criteria.weight));
    assertEquals(answer.criterias[1].short, String(criteria1.short));
    assertEquals(
      answer.criterias[1].description,
      String(criteria1.description),
    );
    assertEquals(
      answer.criterias[1].abbreviation,
      String(criteria1.abbreviation),
    );
    assertEquals(
      answer.criterias[1].inclusionExclusion,
      String(criteria1.inclusionExclusion),
    );
    assertEquals(answer.criterias[1].weight, Number(criteria1.weight));
    assertEquals(answer.criterias[2].short, String(criteria2.short));
    assertEquals(
      answer.criterias[2].description,
      String(criteria2.description),
    );
    assertEquals(
      answer.criterias[2].abbreviation,
      String(criteria2.abbreviation),
    );
    assertEquals(
      answer.criterias[2].inclusionExclusion,
      String(criteria2.inclusionExclusion),
    );
    assertEquals(answer.criterias[2].weight, Number(criteria2.weight));
    await db.close();
    await client.end();
    Batcher.kill();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "addCriteriaToProject",
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
      `{  "short": "AD",
                "description": "A good criteria",
                "abbreviation": "a good abbrevation",
                "inclusionExclusion": "inclusion",
                "weight": 3
            }`,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );

    const project = await Project.create({
      name: "test",
      minCountReviewers: 1,
      countDecisiveReviewers: 1,
      combinationOfReviewers: 1,
      evaluationFormula: "",
      type: "",
      mergeThreshold: 0.8,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.crossRef,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.openCitations,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.googleScholar,
      inUse: false,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.IEEE,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.semanticScholar,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.microsoftAcademic,
      inUse: true,
    });

    await addCriteriaToProject(ctx, Number(project.id));
    const answer = JSON.parse(ctx.response.body as string);
    assertEquals(ctx.response.status, 201);
    assertNotEquals(answer.id, undefined);
    const criteria = await Criteria.find(answer.id);
    assertEquals(String(criteria.short), "AD");
    assertEquals(String(criteria.description), "A good criteria");
    assertEquals(String(criteria.abbreviation), "a good abbrevation");
    assertEquals(String(criteria.inclusionExclusion), "inclusion");
    assertEquals(Number(criteria.weight), 3);
    await db.close();
    await client.end();
    Batcher.kill();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "addCriteriaToProjectWeigthNotANumber",
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
      `{  "short": "AD",
                "description": "A good criteria",
                "abbreviation": "a good abbrevation",
                "inclusionExclusion": "inclusion",
                "weight": abv
            }`,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );

    const project = await Project.create({
      name: "test",
      minCountReviewers: 1,
      countDecisiveReviewers: 1,
      combinationOfReviewers: 1,
      evaluationFormula: "",
      type: "",
      mergeThreshold: 0.8,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.crossRef,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.openCitations,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.googleScholar,
      inUse: false,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.IEEE,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.semanticScholar,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.microsoftAcademic,
      inUse: true,
    });

    await addCriteriaToProject(ctx, Number(project.id));
    const answer = JSON.parse(ctx.response.body as string);

    assertEquals(ctx.response.status, 422);
    assertEquals(answer.id, undefined);

    await db.close();
    await client.end();
    Batcher.kill();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "addCriteriaToProjectInclusionWrong",
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
      `{  "short": "AD",
                "description": "A good criteria",
                "abbreviation": "a good abbrevation",
                "inclusionExclusion": "inclustion",
                "weight": abv
            }`,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );

    const project = await Project.create({
      name: "test",
      minCountReviewers: 1,
      countDecisiveReviewers: 1,
      combinationOfReviewers: 1,
      evaluationFormula: "",
      type: "",
      mergeThreshold: 0.8,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.crossRef,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.openCitations,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.googleScholar,
      inUse: false,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.IEEE,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.semanticScholar,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.microsoftAcademic,
      inUse: true,
    });

    await addCriteriaToProject(ctx, Number(project.id));

    const answer = JSON.parse(ctx.response.body as string);
    assertEquals(ctx.response.status, 422);
    assertEquals(answer.id, undefined);

    await db.close();
    await client.end();
    Batcher.kill();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});
Deno.test({
  name: "addCriteriaToProjectWrongID",
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
      `{  "short": "AD",
                "description": "A good criteria",
                "abbreviation": "a good abbrevation",
                "inclusionExclusion": "inclusion",
                "weight": 3
            }`,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );

    const project = await Project.create({
      name: "test",
      minCountReviewers: 1,
      countDecisiveReviewers: 1,
      combinationOfReviewers: 1,
      evaluationFormula: "",
      type: "",
      mergeThreshold: 0.8,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.crossRef,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.openCitations,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.googleScholar,
      inUse: false,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.IEEE,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.semanticScholar,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.microsoftAcademic,
      inUse: true,
    });

    await addCriteriaToProject(ctx, Number(project.id) + 1);
    const answer = JSON.parse(ctx.response.body as string);
    assertEquals(ctx.response.status, 404);
    assertEquals(answer.id, undefined);

    await db.close();
    await client.end();
    Batcher.kill();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "deleteCriteriaOfProject",
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
      ``,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );

    const project = await Project.create({
      name: "test",
      minCountReviewers: 1,
      countDecisiveReviewers: 1,
      combinationOfReviewers: 1,
      evaluationFormula: "",
      type: "",
      mergeThreshold: 0.8,
    });

    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.crossRef,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.openCitations,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.googleScholar,
      inUse: false,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.IEEE,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.semanticScholar,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.microsoftAcademic,
      inUse: true,
    });

    const criteriaID = Number(
      (await Criteria.create({
        projectId: Number(project.id),
        short: "AD",
        abbreviation: "An abbrevation",
        inclusionExclusion: "inclusion",
        weight: 3,
        description: "description",
      })).id,
    );

    await deleteCriteriaOfProject(ctx, Number(project.id), criteriaID);
    assertEquals(ctx.response.status, 200);
    assertEquals(await Criteria.find(criteriaID), undefined);
    await db.close();
    await client.end();
    Batcher.kill();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "deleteCriteriaOfProjectForbidden",
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
      ``,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );

    const project = await Project.create({
      name: "test",
      minCountReviewers: 1,
      countDecisiveReviewers: 1,
      combinationOfReviewers: 1,
      evaluationFormula: "",
      type: "",
      mergeThreshold: 0.8,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.crossRef,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.openCitations,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.googleScholar,
      inUse: false,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.IEEE,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.semanticScholar,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.microsoftAcademic,
      inUse: true,
    });

    const criteriaID = Number(
      (await Criteria.create({
        projectId: Number(project.id),
        short: "AD",
        abbreviation: "An abbrevation",
        inclusionExclusion: "inclusion",
        weight: 3,
        description: "desc",
      })).id,
    );
    const stage = await Stage.create({
      name: "TestStage",
      projectId: Number(project.id),
      number: 0,
    });
    const paper = await Paper.create({});
    const review = await Review.create({
      userId: Number(user.id),
      stageId: Number(stage.id),
      paperscopeforstageId: Number(paper.id),
    });
    await CriteriaEvaluation.create({
      criteriaId: criteriaID,
      reviewId: Number(review.id),
      value: "yes",
    });
    await deleteCriteriaOfProject(ctx, Number(project.id), criteriaID);
    assertEquals(ctx.response.status, 200);
    assertEquals(await Criteria.find(criteriaID), undefined);
    await db.close();
    await client.end();
    Batcher.kill();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "patchCriteriaOfProject",
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
      `{
            "short": "AD2",
            "description": "another criteria",
            "abbreviation": "another abbrevation",
            "inclusionExclusion": "exclusion",
            "weight": 4
        }`,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );

    const project = await Project.create({
      name: "test",
      minCountReviewers: 1,
      countDecisiveReviewers: 1,
      combinationOfReviewers: 1,
      evaluationFormula: "",
      type: "",
      mergeThreshold: 0.8,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.crossRef,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.openCitations,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.googleScholar,
      inUse: false,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.IEEE,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.semanticScholar,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.microsoftAcademic,
      inUse: true,
    });

    const criteriaID = Number(
      (await Criteria.create({
        projectId: Number(project.id),
        short: "AD",
        abbreviation: "An abbrevation",
        inclusionExclusion: "inclusion",
        weight: 3,
        description: "des",
      })).id,
    );

    await patchCriteriaOfProject(ctx, Number(project.id), criteriaID);
    assertEquals(ctx.response.status, 200);
    const criteria = await Criteria.find(criteriaID);
    assertEquals(String(criteria.short), "AD2");
    assertEquals(String(criteria.description), "another criteria");
    assertEquals(String(criteria.abbreviation), "another abbrevation");
    assertEquals(String(criteria.inclusionExclusion), "exclusion");
    assertEquals(Number(criteria.weight), 4);
    await db.close();
    await client.end();
    Batcher.kill();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "patchCriteriaOfProjectWrongID",
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
      `{
            "short": "AD2",
            "description": "another criteria",
            "abbreviation": "another abbrevation",
            "inclusionExclusion": "exclusion",
            "weight": 4
        }`,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );

    const project = await Project.create({
      name: "test",
      minCountReviewers: 1,
      countDecisiveReviewers: 1,
      combinationOfReviewers: 1,
      evaluationFormula: "",
      type: "",
      mergeThreshold: 0.8,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.crossRef,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.openCitations,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.googleScholar,
      inUse: false,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.IEEE,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.semanticScholar,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.microsoftAcademic,
      inUse: true,
    });

    const criteriaID = Number(
      (await Criteria.create({
        projectId: Number(project.id),
        short: "AD",
        abbreviation: "An abbrevation",
        inclusionExclusion: "inclusion",
        weight: 3,
        description: "des",
      })).id,
    );

    await patchCriteriaOfProject(ctx, Number(project.id), criteriaID + 1);
    assertEquals(ctx.response.status, 404);

    await db.close();
    await client.end();
    Batcher.kill();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "patchCriteriaOfProjectWeightNotNumber",
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
      `{
            "short": "AD2",
            "description": "another criteria",
            "abbreviation": "another abbrevation",
            "inclusionExclusion": "exclusion",
            "weight": "afg"
        }`,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );

    const project = await Project.create({
      name: "test",
      minCountReviewers: 1,
      countDecisiveReviewers: 1,
      combinationOfReviewers: 1,
      evaluationFormula: "",
      type: "",
      mergeThreshold: 0.8,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.crossRef,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.openCitations,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.googleScholar,
      inUse: false,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.IEEE,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.semanticScholar,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.microsoftAcademic,
      inUse: true,
    });

    const criteriaID = Number(
      (await Criteria.create({
        projectId: Number(project.id),
        short: "AD",
        abbreviation: "An abbrevation",
        inclusionExclusion: "inclusion",
        weight: 3,
        description: "des",
      })).id,
    );

    await patchCriteriaOfProject(ctx, Number(project.id), criteriaID);
    assertEquals(ctx.response.status, 422);

    await db.close();
    await client.end();
    Batcher.kill();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "GetCriteriaOfProject",
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
      ``,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );

    const project = await Project.create({
      name: "test",
      minCountReviewers: 1,
      countDecisiveReviewers: 1,
      combinationOfReviewers: 1,
      evaluationFormula: "",
      type: "",
      mergeThreshold: 0.8,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.crossRef,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.openCitations,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.googleScholar,
      inUse: false,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.IEEE,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.semanticScholar,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.microsoftAcademic,
      inUse: true,
    });

    const criteria = await Criteria.create({
      projectId: Number(project.id),
      short: "FG",
      abbreviation: "An abbrevation",
      inclusionExclusion: "inclusion",
      weight: 3,
      description: "a description",
    });
    await getCriteriaOfProject(ctx, Number(project.id), Number(criteria.id));
    const answer = JSON.parse(ctx.response.body as string);
    assertEquals(ctx.response.status, 200);

    assertEquals(answer.short, String(criteria.short));
    assertEquals(answer.description, String(criteria.description));
    assertEquals(answer.abbreviation, String(criteria.abbreviation));
    assertEquals(
      answer.inclusionExclusion,
      String(criteria.inclusionExclusion),
    );
    assertEquals(answer.weight, Number(criteria.weight));

    await db.close();
    await client.end();
    Batcher.kill();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "getReviews",
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
      `{}`,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );

    const project = await Project.create({
      name: "test",
      minCountReviewers: 1,
      countDecisiveReviewers: 1,
      combinationOfReviewers: 1,
      evaluationFormula: "",
      type: "",
      mergeThreshold: 0.8,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.crossRef,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.openCitations,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.googleScholar,
      inUse: false,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.IEEE,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.semanticScholar,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.microsoftAcademic,
      inUse: true,
    });

    const stage = await Stage.create({
      name: "TestStage",
      projectId: Number(project.id),
      number: 0,
    });

    const paper = await Paper.create({
      doi: "fasfafraf",
      title: "nice title",
      abstract: "hello there",
    });

    const pp = await PaperScopeForStage.create({
      paperId: Number(paper.id),
      stageId: Number(stage.id),
    });

    const review = await Review.create({
      finished: true,
      paperscopeforstageId: Number(pp.id),
      userId: Number(user.id),
      stageId: Number(stage.id),
    });

    /*const review2 = */ await Review.create({
      finished: false,
      paperscopeforstageId: Number(pp.id),
      userId: Number(user.id),
      stageId: Number(stage.id),
    });

    const criteria = await Criteria.create({
      projectId: Number(project.id),
      short: "FG",
      abbreviation: "An abbrevation",
      inclusionExclusion: "inclusion",
      weight: 3,
      description: "a description",
    });

    /*const criteriaEval = */ await CriteriaEvaluation.create({
      value: "in",
      reviewId: Number(review.id),
      criteriaId: Number(criteria.id),
    });
    /*const criteriaEval2 = */ await CriteriaEvaluation.create({
      value: "in",
      reviewId: Number(review.id),
      criteriaId: Number(criteria.id),
    });
    await getReviewsOfPaper(
      ctx,
      Number(project.id),
      Number(stage.id),
      Number(pp.id),
    );
    assertEquals(ctx.response.status, 200);
    const answer = JSON.parse(ctx.response.body as string);
    assertEquals(answer.reviews.length, 2);
    assertEquals(answer.reviews[0].finished, true);
    assertEquals(answer.reviews[0].criteriaEvaluations.length, 2);
    assertEquals(answer.reviews[1].criteriaEvaluations.length, 0);

    await db.close();
    await client.end();
    Batcher.kill();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "getReview",
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
      `{}`,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );

    const project = await Project.create({
      name: "test",
      minCountReviewers: 1,
      countDecisiveReviewers: 1,
      combinationOfReviewers: 1,
      evaluationFormula: "",
      type: "",
      mergeThreshold: 0.8,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.crossRef,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.openCitations,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.googleScholar,
      inUse: false,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.IEEE,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.semanticScholar,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.microsoftAcademic,
      inUse: true,
    });

    const stage = await Stage.create({
      name: "TestStage",
      projectId: Number(project.id),
      number: 0,
    });

    const paper = await Paper.create({
      doi: "fasfafraf",
      title: "nice title",
      abstract: "hello there",
    });

    const pp = await PaperScopeForStage.create({
      paperId: Number(paper.id),
      stageId: Number(stage.id),
    });

    const review = await Review.create({
      finished: true,
      paperscopeforstageId: Number(pp.id),
      userId: Number(user.id),
      stageId: Number(stage.id),
    });

    const criteria = await Criteria.create({
      projectId: Number(project.id),
      short: "FG",
      abbreviation: "An abbrevation",
      inclusionExclusion: "inclusion",
      weight: 3,
      description: "a description",
    });

    /*const criteriaEval = */ await CriteriaEvaluation.create({
      value: "in",
      reviewId: Number(review.id),
      criteriaId: Number(criteria.id),
    });
    /*const criteriaEval2 = */ await CriteriaEvaluation.create({
      value: "in",
      reviewId: Number(review.id),
      criteriaId: Number(criteria.id),
    });

    await getReviewOfPaper(
      ctx,
      Number(project.id),
      Number(stage.id),
      Number(pp.id),
      Number(review.id),
    );
    assertEquals(ctx.response.status, 200);
    const answer = JSON.parse(ctx.response.body as string);
    assertEquals(answer.finished, true);
    assertEquals(answer.criteriaEvaluations.length, 2);
    assertEquals(answer.criteriaEvaluations[0].value, "in");
    assertEquals(answer.criteriaEvaluations[1].reviewId, Number(review.id));
    assertEquals(answer.criteriaEvaluations[1].criteriaId, Number(criteria.id));

    await db.close();
    await client.end();
    Batcher.kill();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "postReview",
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

    const project = await Project.create({
      name: "test",
      minCountReviewers: 1,
      countDecisiveReviewers: 1,
      combinationOfReviewers: 3,
      evaluationFormula: "",
      type: "",
      mergeThreshold: 0.8,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.crossRef,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.openCitations,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.googleScholar,
      inUse: false,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.IEEE,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.semanticScholar,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.microsoftAcademic,
      inUse: true,
    });

    const stage = await Stage.create({
      name: "TestStage",
      projectId: Number(project.id),
      number: 0,
    });

    const paper = await Paper.create({
      doi: "fasfafraf",
      title: "nice title",
      abstract: "hello there",
    });

    const pp = await PaperScopeForStage.create({
      paperId: Number(paper.id),
      stageId: Number(stage.id),
    });

    const ctx = await createMockContext(
      app,
      `{
            "paperId": ${Number(pp.id)},
            "stageId": ${Number(stage.id)}
        }`,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );

    await addReviewToPaper(
      ctx,
      Number(project.id),
      Number(stage.id),
      Number(pp.id),
    );
    assertEquals(ctx.response.status, 201);
    const answer = JSON.parse(ctx.response.body as string);
    assertNotEquals(answer.id, undefined);
    const review = await Review.find(answer.id);
    assertEquals(review.userId, user.id);
    assertEquals(review.paperscopeforstage, pp.id);
    assertEquals(review.stageId, stage.id);
    await db.close();
    await client.end();
    Batcher.kill();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "patchReview",
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

    const project = await Project.create({
      name: "test",
      minCountReviewers: 1,
      countDecisiveReviewers: 1,
      combinationOfReviewers: 1,
      evaluationFormula: "",
      type: "",
      mergeThreshold: 0.8,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.crossRef,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.openCitations,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.googleScholar,
      inUse: false,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.IEEE,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.semanticScholar,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.microsoftAcademic,
      inUse: true,
    });

    const stage = await Stage.create({
      name: "TestStage",
      projectId: Number(project.id),
      number: 0,
    });

    const paper = await Paper.create({
      doi: "fasfafraf",
      title: "nice title",
      abstract: "hello there",
    });

    const paper2 = await Paper.create({
      doi: "fasfafraf",
      title: "nice title",
      abstract: "hello there",
    });

    const pp = await PaperScopeForStage.create({
      paperId: Number(paper.id),
      stageId: Number(stage.id),
    });
    const pp2 = await PaperScopeForStage.create({
      paperId: Number(paper2.id),
      stageId: Number(stage.id),
    });

    let review = await Review.create({
      paperscopeforstageId: Number(pp.id),
      userId: Number(user.id),
      stageId: Number(stage.id),
    });
    const ctx = await createMockContext(
      app,
      `{
            "paperId": ${Number(pp2.id)},
            "finished": true
        }`,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );

    await patchReviewOfPaper(
      ctx,
      Number(project.id),
      Number(stage.id),
      Number(pp.id),
      Number(review.id),
    );
    assertEquals(ctx.response.status, 200);
    const answer = JSON.parse(ctx.response.body as string);
    assertNotEquals(answer.id, undefined);
    review = await Review.find(answer.id);
    assertEquals(review.finished, true);
    assertEquals(review.userId, user.id);
    assertEquals(review.paperscopeforstage, pp2.id);
    assertEquals(review.stageId, stage.id);
    await db.close();
    await client.end();
    Batcher.kill();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "deleteReview",
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

    const project = await Project.create({
      name: "test",
      minCountReviewers: 1,
      countDecisiveReviewers: 1,
      combinationOfReviewers: 1,
      evaluationFormula: "",
      type: "",
      mergeThreshold: 0.8,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.crossRef,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.openCitations,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.googleScholar,
      inUse: false,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.IEEE,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.semanticScholar,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.microsoftAcademic,
      inUse: true,
    });

    const stage = await Stage.create({
      name: "TestStage",
      projectId: Number(project.id),
      number: 0,
    });

    const paper = await Paper.create({
      doi: "fasfafraf",
      title: "nice title",
      abstract: "hello there",
    });

    const pp = await PaperScopeForStage.create({
      paperId: Number(paper.id),
      stageId: Number(stage.id),
    });

    let review = await Review.create({
      paperscopeforstageId: Number(pp.id),
      userId: Number(user.id),
      stageId: Number(stage.id),
    });
    const ctx = await createMockContext(
      app,
      `{}`,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );

    await deleteReviewOfPaper(
      ctx,
      Number(project.id),
      Number(stage.id),
      Number(pp.id),
      Number(review.id),
    );
    assertEquals(ctx.response.status, 200);

    review = await Review.find(Number(review.id));
    assertEquals(review, undefined);

    await db.close();
    await client.end();
    Batcher.kill();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "GetCriteriaEvaluationsBasedOnCriteriaOfProject",
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
      ``,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );

    const project = await Project.create({
      name: "test",
      minCountReviewers: 1,
      countDecisiveReviewers: 1,
      combinationOfReviewers: "3,8",
      evaluationFormula: "",
      type: "",
      mergeThreshold: 0.8,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.crossRef,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.openCitations,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.googleScholar,
      inUse: false,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.IEEE,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.semanticScholar,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.microsoftAcademic,
      inUse: true,
    });

    const paper = await Paper.create({
      doi: "fasfafraf",
      title: "nice title",
      abstract: "hello there",
    });
    const stage = await Stage.create({
      name: "TestStage",
      projectId: Number(project.id),
      number: 0,
    });

    const pp = await PaperScopeForStage.create({
      paperId: Number(paper.id),
      stageId: Number(stage.id),
    });

    const review = await Review.create({
      finished: true,
      paperscopeforstageId: Number(pp.id),
      userId: Number(user.id),
      stageId: Number(stage.id),
    });
    const criteria = await Criteria.create({
      projectId: Number(project.id),
      short: "FG",
      abbreviation: "An abbrevation",
      inclusionExclusion: "inclusion",
      weight: 3,
      description: "a description",
    });

    /*const criteriaEval = */ await CriteriaEvaluation.create({
      value: "MAYBE",
      reviewId: Number(review.id),
      criteriaId: Number(criteria.id),
    });
    /*const criteriaEval2 = */ await CriteriaEvaluation.create({
      value: "in",
      reviewId: Number(review.id),
      criteriaId: Number(criteria.id),
    });

    await getCriteriaEvalsOfCriteria(
      ctx,
      Number(project.id),
      Number(criteria.id),
    );
    const answer = JSON.parse(ctx.response.body as string);
    assertEquals(ctx.response.status, 200);

    assertEquals(answer.criteriaevaluations.length, 2);
    assertEquals(answer.criteriaevaluations[0].value, "MAYBE");
    assertEquals(answer.criteriaevaluations[1].reviewId, Number(review.id));
    assertEquals(answer.criteriaevaluations[0].criteriaId, Number(criteria.id));
    await db.close();
    await client.end();
    Batcher.kill();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "GetCriteriaEvaluations",
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
      ``,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );

    const project = await Project.create({
      name: "test",
      minCountReviewers: 1,
      countDecisiveReviewers: 1,
      combinationOfReviewers: "3,8",
      evaluationFormula: "",
      type: "",
      mergeThreshold: 0.8,
    });
    const paper = await Paper.create({
      doi: "fasfafraf",
      title: "nice title",
      abstract: "hello there",
    });
    const stage = await Stage.create({
      name: "TestStage",
      projectId: Number(project.id),
      number: 0,
    });

    const pp = await PaperScopeForStage.create({
      paperId: Number(paper.id),
      stageId: Number(stage.id),
    });

    const review = await Review.create({
      finished: true,
      paperscopeforstageId: Number(pp.id),
      userId: Number(user.id),
      stageId: Number(stage.id),
    });
    const criteria = await Criteria.create({
      projectId: Number(project.id),
      short: "FG",
      abbreviation: "An abbrevation",
      inclusionExclusion: "inclusion",
      weight: 3,
      description: "a description",
    });

    /*const criteriaEval = */ await CriteriaEvaluation.create({
      value: "MAYBE",
      reviewId: Number(review.id),
      criteriaId: Number(criteria.id),
    });
    /*const criteriaEval2 = */ await CriteriaEvaluation.create({
      value: "in",
      reviewId: Number(review.id),
      criteriaId: Number(criteria.id),
    });

    await getCrtieriaEvalsOfReview(
      ctx,
      Number(project.id),
      Number(stage.id),
      Number(pp.id),
      Number(review.id),
    );
    const answer = JSON.parse(ctx.response.body as string);
    assertEquals(ctx.response.status, 200);

    assertEquals(answer.criteriaevaluations.length, 2);
    assertEquals(answer.criteriaevaluations[0].value, "MAYBE");
    assertEquals(answer.criteriaevaluations[1].reviewId, Number(review.id));
    assertEquals(answer.criteriaevaluations[0].criteriaId, Number(criteria.id));
    await db.close();
    await client.end();
    Batcher.kill();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "AddCriteriaEvaluation",
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

    const project = await Project.create({
      name: "test",
      minCountReviewers: 1,
      countDecisiveReviewers: 1,
      combinationOfReviewers: "3,8",
      evaluationFormula: "",
      type: "",
      mergeThreshold: 0.8,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.crossRef,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.openCitations,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.googleScholar,
      inUse: false,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.IEEE,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.semanticScholar,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.microsoftAcademic,
      inUse: true,
    });

    const paper = await Paper.create({
      doi: "fasfafraf",
      title: "nice title",
      abstract: "hello there",
    });
    const stage = await Stage.create({
      name: "TestStage",
      projectId: Number(project.id),
      number: 0,
    });

    const pp = await PaperScopeForStage.create({
      paperId: Number(paper.id),
      stageId: Number(stage.id),
    });

    const review = await Review.create({
      finished: true,
      paperscopeforstageId: Number(pp.id),
      userId: Number(user.id),
      stageId: Number(stage.id),
    });
    const criteria = await Criteria.create({
      projectId: Number(project.id),
      short: "FG",
      abbreviation: "An abbrevation",
      inclusionExclusion: "inclusion",
      weight: 3,
      description: "a description",
    });

    const ctx = await createMockContext(
      app,
      `{
                        "value": "MAYBE",
            "criteriaId": ${Number(criteria.id)}
        }`,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );

    await addCrtieriaEvalToReview(
      ctx,
      Number(project.id),
      Number(stage.id),
      Number(pp.id),
      Number(review.id),
    );
    assertEquals(ctx.response.status, 201);
    const answer = JSON.parse(ctx.response.body as string);
    assertNotEquals(answer.id, undefined);
    const ce = await CriteriaEvaluation.find(answer.id);
    assertEquals(ce.value, "MAYBE");
    assertEquals(Number(ce.criteriaId), Number(criteria.id));
    assertEquals(Number(ce.reviewId), Number(review.id));

    await db.close();
    await client.end();
    Batcher.kill();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});
Deno.test({
  name: "GetCriteriaEvaluation",
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
      ``,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );

    const project = await Project.create({
      name: "test",
      minCountReviewers: 1,
      countDecisiveReviewers: 1,
      combinationOfReviewers: "3,8",
      evaluationFormula: "",
      type: "",
      mergeThreshold: 0.8,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.crossRef,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.openCitations,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.googleScholar,
      inUse: false,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.IEEE,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.semanticScholar,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.microsoftAcademic,
      inUse: true,
    });

    const paper = await Paper.create({
      doi: "fasfafraf",
      title: "nice title",
      abstract: "hello there",
    });
    const stage = await Stage.create({
      name: "TestStage",
      projectId: Number(project.id),
      number: 0,
    });

    const pp = await PaperScopeForStage.create({
      paperId: Number(paper.id),
      stageId: Number(stage.id),
    });

    const review = await Review.create({
      finished: true,
      paperscopeforstageId: Number(pp.id),
      userId: Number(user.id),
      stageId: Number(stage.id),
    });
    const criteria = await Criteria.create({
      projectId: Number(project.id),
      short: "FG",
      abbreviation: "An abbrevation",
      inclusionExclusion: "inclusion",
      weight: 3,
      description: "a description",
    });

    const criteriaEval = await CriteriaEvaluation.create({
      value: "MAYBE",
      reviewId: Number(review.id),
      criteriaId: Number(criteria.id),
    });

    await getCritieriaEvalOfReview(
      ctx,
      Number(project.id),
      Number(stage.id),
      Number(pp.id),
      Number(review.id),
      Number(criteriaEval.id),
    );
    const answer = JSON.parse(ctx.response.body as string);
    assertEquals(ctx.response.status, 200);

    assertEquals(answer.value, "MAYBE");
    assertEquals(answer.reviewId, Number(review.id));
    assertEquals(answer.criteriaId, Number(criteria.id));
    await db.close();
    await client.end();
    Batcher.kill();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "PatchCriteriaEvaluation",
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

    const project = await Project.create({
      name: "test",
      minCountReviewers: 1,
      countDecisiveReviewers: 1,
      combinationOfReviewers: "3,8",
      evaluationFormula: "",
      type: "",
      mergeThreshold: 0.8,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.crossRef,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.openCitations,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.googleScholar,
      inUse: false,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.IEEE,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.semanticScholar,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.microsoftAcademic,
      inUse: true,
    });

    const paper = await Paper.create({
      doi: "fasfafraf",
      title: "nice title",
      abstract: "hello there",
    });
    const stage = await Stage.create({
      name: "TestStage",
      projectId: Number(project.id),
      number: 0,
    });

    const pp = await PaperScopeForStage.create({
      paperId: Number(paper.id),
      stageId: Number(stage.id),
    });

    let review = await Review.create({
      finished: true,
      paperscopeforstageId: Number(pp.id),
      userId: Number(user.id),
      stageId: Number(stage.id),
    });
    const criteria = await Criteria.create({
      projectId: Number(project.id),
      short: "FG",
      abbreviation: "An abbrevation",
      inclusionExclusion: "inclusion",
      weight: 3,
      description: "a description",
    });
    const criteriaEval = await CriteriaEvaluation.create({
      value: "MAYBE",
      reviewId: Number(review.id),
      criteriaId: Number(criteria.id),
    });
    const ctx = await createMockContext(
      app,
      `{
                        "value": "YES"
        }`,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );

    await patchCritieriaEvalOfReview(
      ctx,
      Number(project.id),
      Number(stage.id),
      Number(pp.id),
      Number(review.id),
      Number(criteriaEval.id),
    );
    assertEquals(ctx.response.status, 200);

    review = await CriteriaEvaluation.find(Number(criteriaEval.id));
    assertEquals(review.value, "YES");

    await db.close();
    await client.end();
    Batcher.kill();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "deleteCriteriaEvaluation",
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

    const project = await Project.create({
      name: "test",
      minCountReviewers: 1,
      countDecisiveReviewers: 1,
      combinationOfReviewers: "3,8",
      evaluationFormula: "",
      type: "",
      mergeThreshold: 0.8,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.crossRef,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.openCitations,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.googleScholar,
      inUse: false,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.IEEE,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.semanticScholar,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(project.id),
      searchapiId: IDOfApi.microsoftAcademic,
      inUse: true,
    });

    const paper = await Paper.create({
      doi: "fasfafraf",
      title: "nice title",
      abstract: "hello there",
    });
    const stage = await Stage.create({
      name: "TestStage",
      projectId: Number(project.id),
      number: 0,
    });

    const pp = await PaperScopeForStage.create({
      paperId: Number(paper.id),
      stageId: Number(stage.id),
    });

    let ce = await Review.create({
      finished: true,
      paperscopeforstageId: Number(pp.id),
      userId: Number(user.id),
      stageId: Number(stage.id),
    });
    const criteria = await Criteria.create({
      projectId: Number(project.id),
      short: "FG",
      abbreviation: "An abbrevation",
      inclusionExclusion: "inclusion",
      weight: 3,
      description: "a description",
    });
    const criteriaEvalId = Number(
      (await CriteriaEvaluation.create({
        value: "MAYBE",
        reviewId: Number(ce.id),
        criteriaId: Number(criteria.id),
      })).id,
    );
    const ctx = await createMockContext(
      app,
      `{}`,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );

    await deleteCritieriaEvalOfReview(
      ctx,
      Number(project.id),
      Number(stage.id),
      Number(pp.id),
      Number(ce.id),
      criteriaEvalId,
    );
    assertEquals(ctx.response.status, 200);

    ce = await CriteriaEvaluation.find(criteriaEvalId);
    assertEquals(ce, undefined);

    await db.close();
    await client.end();
    Batcher.kill();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});
