import {Request, ServerResponse, BodyOptions, Body, BodyReader, BodyStream, ServerRequest} from 'https://deno.land/x/oak/mod.ts';
import {RequestBodyMock} from './mockBody.test.ts'

let requestResponseStack: ServerResponse[] = [];

export function createMockRequest(
    requestBodyJsonString: string,
    headersInit: string[][],
    url = "/index.html",
    proto = "HTTP/1.1",
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
        body: new RequestBodyMock(requestBodyJsonString).json,
        method: "GET",
        accepts: (_contentType: string) => {
            return true;
        },
        serverRequest: {
            headers: [["token", "blaaaaaa"]]
        }

    }
    return request as any;
}