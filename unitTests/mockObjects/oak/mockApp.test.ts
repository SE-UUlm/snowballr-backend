import { Application } from "https://deno.land/x/oak@v11.1.0/mod.ts";

/**
 * This class is used to mock the basic functionality of an app, to use it for a test
 * @param state
 */
export function createMockApp<
  S extends Record<string | number | symbol, any> = Record<string, any>,
>(
  state = {} as S,
): Application<S> {
  const app = {
    state,
    use() {
      return app;
    },
  };
  return app as any;
}
