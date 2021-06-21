import {Response} from 'https://deno.land/x/oak/mod.ts';

export function createMockResponse(headersInit: string[][]): Response {
    let response = {
        headers: new Headers(headersInit),
        status: 999,
        body: undefined,
        redirect(url: string | URL) {
            this.headers.append("Location", encodeURI(String(url)));
        },
    };
    return response as any;
}