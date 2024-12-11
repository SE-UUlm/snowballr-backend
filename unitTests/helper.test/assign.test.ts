import { assertEquals } from "https://deno.land/std@0.150.0/testing/asserts.ts";
import { assign, isEqual } from "../../src/helper/assign.ts";
import { setup } from "../../src/helper/setup.ts";

Deno.test({
  name: "assign",
  async fn(): Promise<void> {
    await setup(true);
    const obj1: any = {};
    const obj2 = {
      first: "hello",
      second: 2,
      third: [2, 3, 4],
      fourth: [{ test: "test" }, { test: "test" }],
      fifth: { hello: { there: { general: "kenobi" } } },
    };
    assign(obj1, obj2);

    assertEquals(obj1.first, "hello");
    assertEquals(obj1.second, 2);
    assertEquals(obj1.third, [2, 3, 4]);
    assertEquals(obj1.fourth, [{ test: "test" }, { test: "test" }]);
    assertEquals(obj1.fifth, { hello: { there: { general: "kenobi" } } });
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "isEqual",
  async fn(): Promise<void> {
    await setup(true);
    const obj1: any = {
      first: "hello",
      second: 2,
      third: [2, 3, 4],
      fourth: [{ test: "test" }, { test: "test" }],
      fifth: { hello: { there: { general: "kenobi" } } },
    };
    const obj2 = {
      first: "hello",
      second: 2,
      third: [2, 3, 4],
      fourth: [{ test: "test" }, { test: "test" }],
      fifth: { hello: { there: { general: "kenobi" } } },
    };

    assertEquals(isEqual(obj1, obj2), true);
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "isUnEqualParameter",
  async fn(): Promise<void> {
    await setup(true);
    const obj1: any = {
      first: "hello",
      second: 2,
      third: [2, 3, 4],
      fourth: [{ test: "test" }, { test: "test" }],
      fifth: { hello: { there: { general: "kenobi" } } },
    };
    const obj2 = {
      first: "hello",
      second: 2,
      third: [2, 3, 4],
      fourth: [{ test: "test" }, { test: "test" }],
      fifth: { hello: { there: "general" } },
    };

    assertEquals(isEqual(obj1, obj2), false);
  },
  sanitizeResources: false,
  sanitizeOps: false,
});

Deno.test({
  name: "isUnEqualDifferentParameter",
  async fn(): Promise<void> {
    await setup(true);
    const obj1: any = {
      first: "hello",
      second: 2,
      fourth: [{ test: "test" }, { test: "test" }],
      fifth: { hello: { there: { general: "kenobi" } } },
    };
    const obj2 = {
      first: "hello",
      second: 2,
      third: [2, 3, 4],
      fourth: [{ test: "test" }, { test: "test" }],
      fifth: { hello: { there: "general" } },
    };

    assertEquals(isEqual(obj1, obj2), false);
  },
  sanitizeResources: false,
  sanitizeOps: false,
});
