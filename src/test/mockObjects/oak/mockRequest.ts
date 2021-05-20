import {Request, ServerResponse, BodyOptions, Body, BodyReader, BodyStream} from 'https://deno.land/x/oak/mod.ts';
import {RequestBodyMock1} from './mockBody.ts'

let requestResponseStack: ServerResponse[] = [];

export function createMockRequest(
    requestBody: any,
    url = "/index.html",
    proto = "HTTP/1.1",
    headersInit: string[][] = [["host", "example.com"]],
    host = "localhost",
): Promise<Request> {
    let request = {
        url,
        headers: new Headers(headersInit),
        respond(response: ServerResponse) {
            requestResponseStack.push(response);
            return Promise.resolve();
        },
        proto,
        body: requestBody.get,
        method: "GET",
        accepts: (_contentType: string) => {
            return true;
        },

    }
    return request as any;
}
