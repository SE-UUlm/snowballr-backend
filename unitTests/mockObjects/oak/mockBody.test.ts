import { Body, BodyOptions, BodyReader, BodyStream } from 'https://deno.land/x/mod.ts';

/**
 * This class is for mocking a body in an oak request.
 * It currently only holds the possibility to either hold an empty body or a json body.
 * The json body can be set through the constructor and can then be parsed like a normal request in the code.
 */
export class RequestBodyMock {
    bodyJsonString

    constructor(bodyJsonString?: string) {
        bodyJsonString ? this.bodyJsonString = bodyJsonString : undefined;
    }

    json = (bodyJsonString: any) => this.get(bodyJsonString, this.bodyJsonString);

    get(
        { type, contentTypes = {} }: BodyOptions,
        bla?: string
    ): Body | BodyReader | BodyStream {

        if (type === "json") {
            const body: Body = Object.create(null);
            Object.defineProperties(body, {
                type: {
                    value: "json",
                    configurable: true,
                    enumerable: true,
                },
                value: {
                    get: () => JSON.parse(this.bodyJsonString as any),
                    configurable: true,
                    enumerable: true,
                },
            });
            return body;
        } else {
            if (this.bodyJsonString) {
                throw Error("filled body")
            } else {
                return Object.create(null);
            }
        }
    }
}




