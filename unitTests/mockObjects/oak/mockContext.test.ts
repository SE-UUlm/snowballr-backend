import {Application, Context, Cookies} from 'https://deno.land/x/oak/mod.ts';
import {createMockRequest} from "./mockRequest.test.ts";
import {createMockResponse} from "./mockResponse.test.ts";
import {RequestBodyMock} from "./mockBody.test.ts";

export async function createMockContext<S extends Record<string | number | symbol, any> = Record<string, any>,
    >(
    app: Application<S>,
    requestBodyJsonString: string,
    standardHeader: string[][] = [["Content-Type", "application/json"]],
    path = "/",
    method = "GET",
) {
    const request = await createMockRequest(requestBodyJsonString, standardHeader);
    const response = await createMockResponse(standardHeader);
    const cookies = new Cookies(request, response);
    return ({
        app,
        request: request,
        response: response,
        cookies: cookies,
        state: app.state,
    } as unknown) as Context<S>;
}