import { Request, ServerResponse } from "https://deno.land/x/oak@v11.1.0/mod.ts";
import { RequestBodyMock } from './mockBody.test.ts'

const requestResponseStack: ServerResponse[] = [];

/**
 * Creates a request that is set in the context.
 * Most of the params are usually set in the context mock.
 *
 * @param headersInit
 * @param requestBodyJsonString
 * @param pathname
 * @param proto
 * @param host
 */
export function createMockRequest(
    headersInit: string[][],
    requestBodyJsonString?: string,
    pathname = "/index.html",
    proto = "HTTP/1.1",
    host = "localhost",
): Promise<Request> {
    const request = {
        url: {
            pathname: pathname
        },
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