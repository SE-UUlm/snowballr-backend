import {Application} from 'https://deno.land/x/oak/mod.ts';

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