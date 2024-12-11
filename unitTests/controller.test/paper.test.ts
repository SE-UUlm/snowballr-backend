import {
  assertEquals,
  assertNotEquals,
} from "https://deno.land/std@0.150.0/testing/asserts.ts";
import { IApiPaper } from "../../src/api/iApiPaper.ts";
import { idType } from "../../src/api/iApiUniqueId.ts";
import {
  client,
  db,
  saveChildren,
} from "../../src/controller/database.controller.ts";
import { insertUser } from "../../src/controller/databaseFetcher/user.ts";
import { Batcher } from "../../src/controller/fetch.controller.ts";
import {
  addAuthorToPaper,
  deleteAuthorOfPaper,
  getAuthors,
  getPaper,
  getPaperCitations,
  getPaperReferences,
  getPapers,
  getSourcePaper,
  patchPaper,
  postPaper,
  postPaperCitation,
  postPaperReference,
} from "../../src/controller/paper.controller.ts";
import {
  paperCache,
  savePaper,
} from "../../src/controller/project.controller.ts";
import { createJWT } from "../../src/controller/validation.controller.ts";
import { IDOfApi, setup } from "../../src/helper/setup.ts";
import { Author } from "../../src/model/db/author.ts";
import { Criteria } from "../../src/model/db/criteria.ts";
import { CriteriaEvaluation } from "../../src/model/db/criteriaEval.ts";
import { Paper } from "../../src/model/db/paper.ts";
import { PaperScopeForStage } from "../../src/model/db/paperScopeForStage.ts";
import { Project } from "../../src/model/db/project.ts";
import { ProjectUsesApi } from "../../src/model/db/projectUsesApi.ts";
import { Review } from "../../src/model/db/review.ts";
import { Stage } from "../../src/model/db/stage.ts";
import { Wrote } from "../../src/model/db/wrote.ts";
import { createMockApp } from "../mockObjects/oak/mockApp.test.ts";
import { createMockContext } from "../mockObjects/oak/mockContext.test.ts";

Deno.test({
  name: "getExistingReviewInProject",
  async fn(): Promise<void> {
    await setup(true);

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
    const nextStage = await Stage.create({
      name: "NextStage",
      projectId: Number(project.id),
      number: 1,
    });
    const source: IApiPaper = {
      title: ["Hello there"],
      abstract: ["General Kenobi"],
      uniqueId: [{ type: idType.DOI, value: "123" }],
    } as IApiPaper;
    const paper = await Paper.create({
      title: "Hello there",
      abstract: "General Kenobi",
      doi: "123",
    });

    const review = await Review.create({
      userId: 1,
      stageId: Number(stage.id),
    });
    await PaperScopeForStage.create({
      stageId: Number(stage.id),
      paperId: Number(paper.id),
      reviewId: Number(review.id),
    });
    await CriteriaEvaluation.create({
      criteriaId: criteriaID,
      reviewId: Number(review.id),
      value: "yes",
    });

    await savePaper(source, nextStage, Number(project.mergeThreshold));

    const reviewCount = await Review.count();
    const projectCount = await Project.count();
    const paperCount = await Paper.count();
    const paperScopeForStageCount = await PaperScopeForStage.count();
    const firstPsfsc = await PaperScopeForStage.find(1);
    const secondPsfsc = await PaperScopeForStage.find(2);

    console.log(await Review.all());
    console.log(await PaperScopeForStage.all());
    console.log("-----------------------------" + Number(review.id));
    console.log(await Review.where("id", Number(review.id)).paper());

    assertEquals(reviewCount, 1);
    assertEquals(projectCount, 1);
    assertEquals(paperCount, 1);
    assertEquals(paperScopeForStageCount, 2);
    assertEquals(firstPsfsc.reviewId, 1);
    assertEquals(secondPsfsc.reviewId, 1);
    assertEquals(firstPsfsc.stageId, 1);
    assertEquals(secondPsfsc.stageId, 2);
    assertEquals(firstPsfsc.paperId, 1);
    assertEquals(secondPsfsc.paperId, 1);

    await client.end();
    Batcher.kill();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "dontCopyReviewInProject",
  async fn(): Promise<void> {
    await setup(true);

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
    const nextStage = await Stage.create({
      name: "NextStage",
      projectId: Number(project.id),
      number: 1,
    });
    const source: IApiPaper = {
      title: ["Another Paper"],
      abstract: ["General Griveous"],
      uniqueId: [{ type: idType.DOI, value: "456" }],
    } as IApiPaper;
    const paper = await Paper.create({
      title: "Hello there",
      abstract: "General Kenobi",
      doi: "123",
    });

    const review = await Review.create({
      userId: 1,
      stageId: Number(stage.id),
    });
    /*const paperScopeForStage = */ await PaperScopeForStage.create({
      stageId: Number(stage.id),
      paperId: Number(paper.id),
      reviewId: Number(review.id),
    });
    await CriteriaEvaluation.create({
      criteriaId: criteriaID,
      reviewId: Number(review.id),
      value: "yes",
    });

    await savePaper(source, nextStage, Number(project.mergeThreshold));

    const reviewCount = await Review.count();
    const projectCount = await Project.count();
    const paperCount = await Paper.count();
    const paperScopeForStageCount = await PaperScopeForStage.count();
    const firstPsfsc = await PaperScopeForStage.find(1);
    //const secondPsfsc = await PaperScopeForStage.find(2);

    console.log(await Review.all());
    console.log(await PaperScopeForStage.all());
    console.log("-----------------------------" + Number(review.id));
    console.log(await Review.where("id", Number(review.id)).paper());

    assertEquals(reviewCount, 1);
    assertEquals(projectCount, 1);
    assertEquals(paperCount, 2);
    assertEquals(paperScopeForStageCount, 1);
    assertEquals(firstPsfsc.reviewId, 1);
    assertEquals(firstPsfsc.stageId, 1);
    assertEquals(firstPsfsc.paperId, 1);

    await client.end();
    Batcher.kill();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "dontCopyExistingReviewFromAnotherProject",
  async fn(): Promise<void> {
    await setup(true);

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

    const anotherProject = await Project.create({
      name: "another",
      minCountReviewers: 1,
      countDecisiveReviewers: 1,
      combinationOfReviewers: 1,
      evaluationFormula: "",
      type: "",
      mergeThreshold: 0.8,
    });
    await ProjectUsesApi.create({
      projectId: Number(anotherProject.id),
      searchapiId: IDOfApi.crossRef,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(anotherProject.id),
      searchapiId: IDOfApi.openCitations,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(anotherProject.id),
      searchapiId: IDOfApi.googleScholar,
      inUse: false,
    });
    await ProjectUsesApi.create({
      projectId: Number(anotherProject.id),
      searchapiId: IDOfApi.IEEE,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(anotherProject.id),
      searchapiId: IDOfApi.semanticScholar,
      inUse: true,
    });
    await ProjectUsesApi.create({
      projectId: Number(anotherProject.id),
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
    /*const anotherCriteriaID = */ Number(
      (await Criteria.create({
        projectId: Number(project.id),
        short: "AD",
        abbreviation: "Another abbrevation",
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
    const nextStage = await Stage.create({
      name: "NextStage",
      projectId: Number(anotherProject.id),
      number: 1,
    });
    const source: IApiPaper = {
      title: ["Hello there"],
      abstract: ["General Kenobi"],
      uniqueId: [{ type: idType.DOI, value: "123" }],
    } as IApiPaper;
    const paper = await Paper.create({
      title: "Hello there",
      abstract: "General Kenobi",
      doi: "123",
    });

    const review = await Review.create({
      userId: 1,
      stageId: Number(stage.id),
    });
    /*const paperScopeForStage = */ await PaperScopeForStage.create({
      stageId: Number(stage.id),
      paperId: Number(paper.id),
      reviewId: Number(review.id),
    });
    await CriteriaEvaluation.create({
      criteriaId: criteriaID,
      reviewId: Number(review.id),
      value: "yes",
    });

    await savePaper(source, nextStage, Number(anotherProject.mergeThreshold));

    const reviewCount = await Review.count();
    const paperScopeForStageCount = await PaperScopeForStage.count();
    const projectCount = await Project.count();
    const paperCount = await Paper.count();
    const firstPsfsc = await PaperScopeForStage.find(1);
    //const secondPsfsc = await PaperScopeForStage.find(2);

    console.log(await Review.all());
    console.log(await PaperScopeForStage.all());
    console.log("-----------------------------" + Number(review.id));
    console.log(await Review.where("id", Number(review.id)).paper());

    assertEquals(reviewCount, 1);
    assertEquals(projectCount, 2);
    assertEquals(paperCount, 1);
    assertEquals(paperScopeForStageCount, 1);
    assertEquals(firstPsfsc.reviewId, 1);
    assertEquals(firstPsfsc.stageId, 1);
    assertEquals(firstPsfsc.paperId, 1);

    await client.end();
    Batcher.kill();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "getAllPapers",
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

    const paperNumber = (await Paper.all()).length;
    const app = await createMockApp();
    await Paper.create({});
    await Paper.create({});
    await Paper.create({});
    await Paper.create({});

    const token = await createJWT(user);
    const ctx = await createMockContext(
      app,
      ``,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );
    await getPapers(ctx);
    const answer = JSON.parse(ctx.response.body as string);
    assertEquals(ctx.response.status, 200);
    assertEquals(answer.papers.length, paperNumber + 4);
    await db.close();
    await client.end();
    Batcher.kill();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "postPaper",
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
            "doi": "1234",
            "title": "a title",
            "abstract": "an abstract",
            "year": 2009,
            "publisher": "uni ulm",
            "type": "proceeding",
            "scope": "a scope",
            "scopeName": "a scopeName"
        }`,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );
    await postPaper(ctx);
    const answer = JSON.parse(ctx.response.body as string);
    assertEquals(ctx.response.status, 200);
    const paper = await Paper.find(answer.id);
    assertEquals(String(paper.doi), "1234");
    assertEquals(String(paper.title), "a title");
    assertEquals(String(paper.abstract), "an abstract");
    assertEquals(Number(paper.year), 2009);
    assertEquals(String(paper.publisher), "uni ulm");
    assertEquals(String(paper.type), "proceeding");
    assertEquals(String(paper.scope), "a scope");
    assertEquals(String(paper.scopeName), "a scopeName");
    await db.close();
    await client.end();
    Batcher.kill();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "getPaper",
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

    // const paperNumber = (await Paper.all()).length
    const app = await createMockApp();
    const paper = await Paper.create({
      title: "Hello there",
      abstract: "General Kenobi",
    });

    const token = await createJWT(user);
    const ctx = await createMockContext(
      app,
      `{}`,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );
    await getPaper(ctx, Number(paper.id));
    const answer = JSON.parse(ctx.response.body as string);
    assertEquals(ctx.response.status, 200);
    assertEquals(answer.title, "Hello there");
    assertEquals(answer.abstract, "General Kenobi");
    assertEquals(answer.ppid, undefined);
    assertEquals(answer.id, Number(paper.id));
    await db.close();
    await client.end();
    Batcher.kill();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "getPaperWrongId",
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
    const paper = await Paper.create({
      title: "Hello there",
      abstract: "General Kenobi",
    });

    const token = await createJWT(user);
    const ctx = await createMockContext(
      app,
      ``,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );
    await getPaper(ctx, Number(paper.id) + 1);
    assertEquals(ctx.response.status, 404);
    Batcher.kill();
    await db.close();
    await client.end();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "getPaperReferences",
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
    const paper = await Paper.create({
      title: "Hello there",
      abstract: "General Kenobi",
    });
    const paper1 = await Paper.create({
      title: "Hi",
      abstract: "this abstract",
    });
    const paper2 = await Paper.create({ title: "mlem", abstract: "mlem mlem" });
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
    const token = await createJWT(user);
    const ctx = await createMockContext(
      app,
      ``,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );
    await getPaperReferences(ctx, Number(paper.id));
    const answer = JSON.parse(ctx.response.body as string);
    assertEquals(ctx.response.status, 200);
    assertEquals(answer.papers.length, 2);
    assertEquals(answer.papers[0].title, "Hi");
    assertEquals(answer.papers[0].id, Number(paper1.id));
    assertEquals(answer.papers[1].title, "mlem");
    assertEquals(answer.papers[1].abstract, "mlem mlem");
    await db.close();
    await client.end();
    Batcher.kill();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "getPaperCitations",
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
    const paper = await Paper.create({
      title: "Hello there",
      abstract: "General Kenobi",
    });
    const paper1 = await Paper.create({
      title: "Hi",
      abstract: "this abstract",
    });
    const paper2 = await Paper.create({ title: "mlem", abstract: "mlem mlem" });
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
    const token = await createJWT(user);
    const ctx = await createMockContext(
      app,
      ``,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );
    await getPaperCitations(ctx, Number(paper.id));
    const answer = JSON.parse(ctx.response.body as string);
    assertEquals(ctx.response.status, 200);
    assertEquals(answer.papers.length, 2);
    assertEquals(answer.papers[0].title, "Hi");
    assertEquals(answer.papers[0].id, Number(paper1.id));
    assertEquals(answer.papers[1].title, "mlem");
    assertEquals(answer.papers[1].abstract, "mlem mlem");
    await db.close();
    await client.end();
    Batcher.kill();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});
Deno.test({
  name: "postPaperCitations",
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
    await getPaperCitations(ctx, Number(paper.id));
    let answer = JSON.parse(ctx.response.body as string);
    const length = answer.papers.length;
    ctx = await createMockContext(
      app,
      `{"id": ${paper1.id}}`,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );
    await postPaperCitation(ctx, Number(paper.id));

    await getPaperCitations(ctx, Number(paper.id));
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
  name: "postPaperReferences",
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
    await getPaperReferences(ctx, Number(paper.id));
    let answer = JSON.parse(ctx.response.body as string);
    const length = answer.papers.length;
    ctx = await createMockContext(
      app,
      `{"id": ${paper1.id}}`,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );
    await postPaperReference(ctx, Number(paper.id));

    await getPaperReferences(ctx, Number(paper.id));
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
  name: "getPaperSource",
  async fn(): Promise<void> {
    await setup(true);
    await paperCache.fileCache!.purge();
    const user = await insertUser(
      "test@test",
      "ash",
      true,
      "Test",
      "Tester",
      "active",
    );

    const app = await createMockApp();
    const paper = await Paper.create({
      title: "Hello there",
      abstract: "General Kenobi",
    });
    const source: IApiPaper = {
      title: ["Hello there", "Hi There"],
    } as IApiPaper;
    await paperCache.add(String(paper.id), source);

    const token = await createJWT(user);
    const ctx = await createMockContext(
      app,
      ``,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );
    await getSourcePaper(ctx, Number(paper.id));
    const answer = JSON.parse(ctx.response.body as string);
    assertEquals(ctx.response.status, 200);
    assertEquals(answer.title, ["Hello there", "Hi There"]);

    await db.close();
    await client.end();
    Batcher.kill();
    paperCache.clear();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "getPaperSourceWrongId",
  async fn(): Promise<void> {
    await setup(true);
    await paperCache.fileCache!.purge();
    const user = await insertUser(
      "test@test",
      "ash",
      true,
      "Test",
      "Tester",
      "active",
    );

    const app = await createMockApp();
    const paper = await Paper.create({
      title: "Hello there",
      abstract: "General Kenobi",
    });
    const source: IApiPaper = { title: ["Hello there, Hi There"] } as IApiPaper;
    paperCache.add(String(paper.id), source);

    const token = await createJWT(user);
    const ctx = await createMockContext(
      app,
      ``,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );
    await getSourcePaper(ctx, Number(paper.id) + 1);
    assertEquals(ctx.response.status, 404);

    await db.close();
    await client.end();
    Batcher.kill();
    paperCache.clear();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "PatchPaper",
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
    let paper = await Paper.create({
      title: "Hello there",
      abstract: "General Kenobi",
    });

    const token = await createJWT(user);
    const ctx = await createMockContext(
      app,
      `{"title":"Hello There"}`,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );
    await patchPaper(ctx, Number(paper.id));
    assertEquals(ctx.response.status, 200);
    paper = await Paper.find(Number(paper.id));
    assertEquals(String(paper.title), "Hello There");

    await db.close();
    await client.end();
    Batcher.kill();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "PatchPaperWrongId",
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
    let paper = await Paper.create({
      title: "Hello there",
      abstract: "General Kenobi",
    });

    const token = await createJWT(user);
    const ctx = await createMockContext(
      app,
      `{"title":"Hello There}`,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );
    await patchPaper(ctx, Number(paper.id) + 1);
    assertEquals(ctx.response.status, 404);
    paper = await Paper.find(Number(paper.id));
    assertEquals(String(paper.title), "Hello there");

    await db.close();
    await client.end();
    Batcher.kill();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "PatchPaperPaperCache",
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
    let paper = await Paper.create({
      title: "Hello there",
      abstract: "General Kenobi",
    });
    await paperCache.add(
      String(paper.id),
      {
        title: ["Hello there", "Hi There"],
        titleSource: ["BA", "MA"],
      } as IApiPaper,
    );
    const token = await createJWT(user);
    const ctx = await createMockContext(
      app,
      `{"title":"Hello There"}`,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );
    await getPaper(ctx, Number(paper.id));
    let answer = JSON.parse(ctx.response.body as string);
    assertEquals(answer.status, "unfinished");
    await patchPaper(ctx, Number(paper.id));
    assertEquals(ctx.response.status, 200);
    paper = await Paper.find(Number(paper.id));
    assertEquals(String(paper.title), "Hello There");
    const source = paperCache.get(String(paper.id));
    assertEquals(source, undefined);
    await getPaper(ctx, Number(paper.id));
    answer = JSON.parse(ctx.response.body as string);
    assertEquals(answer.status, "ready");

    await db.close();
    await client.end();
    Batcher.kill();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "getAuthorsOfPaper",
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
    const paper = await Paper.create({
      title: "Hello there",
      abstract: "General Kenobi",
    });
    const author1 = await Author.create({ firstName: "Hi" });
    const author2 = await Author.create({ lastName: "Hellu" });
    await Wrote.create({
      authorId: Number(author1.id),
      paperId: Number(paper.id),
    });
    await Wrote.create({
      authorId: Number(author2.id),
      paperId: Number(paper.id),
    });
    const token = await createJWT(user);
    const ctx = await createMockContext(
      app,
      ``,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );
    await getAuthors(ctx, Number(paper.id));
    const answer = JSON.parse(ctx.response.body as string);
    assertEquals(ctx.response.status, 200);
    assertEquals(answer.authors.length, 2);
    assertEquals(answer.authors[0].firstName, "Hi");
    assertEquals(answer.authors[1].lastName, "Hellu");
    await db.close();
    await client.end();
    Batcher.kill();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "addAuthorToPaper",
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
    const paper = await Paper.create({
      title: "Hello there",
      abstract: "General Kenobi",
    });
    const author1 = await Author.create({ firstName: "Hi" });
    const token = await createJWT(user);
    const ctx = await createMockContext(
      app,
      `{"id": ${Number(author1.id)}}`,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );
    await addAuthorToPaper(ctx, Number(paper.id));
    assertEquals(ctx.response.status, 200);
    const wrote = await Wrote.where({
      paperId: Number(paper.id),
      authorId: Number(author1.id),
    }).get();
    assertNotEquals(wrote, undefined);
    await db.close();
    await client.end();
    Batcher.kill();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "addAuthorToPaperWrongId",
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
    const paper = await Paper.create({
      title: "Hello there",
      abstract: "General Kenobi",
    });
    const author1 = await Author.create({ firstName: "Hi" });
    const token = await createJWT(user);
    const ctx = await createMockContext(
      app,
      `{"id": ${Number(author1.id) + 1}}`,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );
    await addAuthorToPaper(ctx, Number(paper.id));
    assertEquals(ctx.response.status, 404);
    const wrote = await Wrote.where({
      paperId: Number(paper.id),
      authorId: Number(author1.id),
    }).get();
    if (Array.isArray(wrote)) {
      assertEquals(wrote[0], undefined);
    } else {
      assertEquals(wrote, "database now doesn't always return array");
    }

    await db.close();
    await client.end();
    Batcher.kill();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "deleteAuthorOfPaper",
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
    const paper = await Paper.create({
      title: "Hello there",
      abstract: "General Kenobi",
    });
    const author1 = await Author.create({ firstName: "Hi" });
    let wrote = await Wrote.create({
      authorId: Number(author1.id),
      paperId: Number(paper.id),
    });
    const token = await createJWT(user);
    const ctx = await createMockContext(
      app,
      ``,
      [["Content-Type", "application/json"]],
      "/",
      token,
    );
    await deleteAuthorOfPaper(ctx, Number(paper.id), Number(author1.id));
    assertEquals(ctx.response.status, 200);
    wrote = await Wrote.find(Number(wrote.id));
    assertEquals(wrote, undefined);
    await db.close();
    await client.end();
    Batcher.kill();
  },
  sanitizeResources: false,
  sanitizeOps: false,
});
