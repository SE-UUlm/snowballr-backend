import {Response} from 'https://deno.land/x/oak/mod.ts';

export function createMockResponse(): Response {
    const headers = new Headers();
    return {
        headers: headers,
        status: 200,
        body: undefined,
        redirect(url: string | URL) {
            headers.set("Location", encodeURI(String(url)));
        },
    } as any;
}