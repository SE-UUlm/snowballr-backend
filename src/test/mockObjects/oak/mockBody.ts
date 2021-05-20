import {Body, BodyOptions, BodyReader, BodyStream} from 'https://deno.land/x/oak/mod.ts';

export class RequestBodyMock {
    constructor(bodyJsonString: string){
        this.bodyJsonString = bodyJsonString;
}
    bodyJsonString

    json = (bodyJsonString:any) => this.get(bodyJsonString, this.bodyJsonString);

    get(
        { type, contentTypes = {} }: BodyOptions,
        bla: string
    ): Body | BodyReader | BodyStream {

        const body: Body = Object.create(null);
        Object.defineProperties(body, {
            type: {
                value: "json",
                configurable: true,
                enumerable: true,
            },
            value: {
                get: () => JSON.parse(this.bodyJsonString),
                configurable: true,
                enumerable: true,
            },
        });
        return body;
    }
}




