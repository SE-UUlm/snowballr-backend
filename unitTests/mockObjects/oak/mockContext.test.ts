import {Application, Context, Cookies} from 'https://deno.land/x/oak/mod.ts';
import {createMockRequest} from "./mockRequest.test.ts";
import {createMockResponse} from "./mockResponse.test.ts";

export async function createMockContext<S extends Record<string | number | symbol, any> = Record<string, any>,
    >(
    app: Application<S>,
    requestBodyJsonString?: string,
    standardHeader: string[][] = [["Content-Type", "application/json"]],
    path = "/",
    token?: string,
    method = "GET",
) {
    if(token){ standardHeader.push(["authenticationToken", token])}
    const request = await createMockRequest(standardHeader, requestBodyJsonString, path);
    const response = await createMockResponse(standardHeader);
    const cookies = token ? new CookieMock(token) : new Cookies(request, response);
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