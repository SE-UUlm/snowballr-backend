import { Application, Context, Cookies, Response } from "https://deno.land/x/oak@v11.1.0/mod.ts";
import { createMockRequest } from "./mockRequest.test.ts";
import { createMockResponse } from "./mockResponse.test.ts";


/**
 * This function mocks a context in oak.
 *
 * @param app The app, normally the mockAPP
 * @param requestBodyJsonString if a request body in json is needed, it can be set here
 * @param standardHeader Header for the request AND Response (distinguished headers weren't needed yet) normally to allow json
 * @param path path of the request
 * @param token authentication token that will be set in the header to make calls as a logged in user
 * @param method method of the request, normally GET
 */
export async function createMockContext<S extends Record<string | number | symbol, any> = Record<string, any>,
    >(
        app: Application<S>,
        requestBodyJsonString?: string,
        standardHeader: string[][] = [["Content-Type", "application/json"]],
        path = "/",
        token?: string,
        method = "GET",
) {
    if (token) {
        standardHeader.push(["authenticationToken", token])
    }
    const request = await createMockRequest(standardHeader, requestBodyJsonString, path);
    const response = await createMockResponse(standardHeader);
    const cookies = token ? new CookieMock(token) : new Cookies(request, response as unknown as Response);
    return ({
        app,
        request: request,
        response: response,
        cookies: cookies,
        state: app.state,
    } as unknown) as Context<S>;
}

class CookieMock {
    token

    constructor(token: string) {
        this.token = token;
    }

    get(bla: string) {
        return this.token;
    }

    delete(bla: string) {
        return;
    }
}