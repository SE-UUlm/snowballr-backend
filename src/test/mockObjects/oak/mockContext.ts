import {Application, Context, Cookies} from 'https://deno.land/x/oak/mod.ts';
import {createMockRequest} from "./mockRequest.ts";
import {createMockResponse} from "./mockResponse.ts";
import {RequestBodyMock} from "./mockBody.ts";

export async function createMockContext<S extends Record<string | number | symbol, any> = Record<string, any>,
    >(
    app: Application<S>,
    requestBodyJsonString: string,
    requestHeader: string[][] = [["Content-Type", "application/json"]],
    path = "/",
    method = "GET",
) {
    const request = await createMockRequest(requestBodyJsonString, requestHeader);
    const response = await createMockResponse();
    const cookies = new Cookies(request, response);
    return ({
        app,
        request: request,
        response: response,
        cookies: cookies,
        state: app.state,
    } as unknown) as Context<S>;
}