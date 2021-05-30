import {Body, BodyOptions, BodyReader, BodyStream} from 'https://deno.land/x/oak/mod.ts';

export class RequestBodyMock {
    constructor(bodyJsonString?: string){
        bodyJsonString?this.bodyJsonString = bodyJsonString: undefined ;
}
    bodyJsonString

    json = (bodyJsonString:any) => this.get(bodyJsonString, this.bodyJsonString);

    get(
        { type, contentTypes = {} }: BodyOptions,
        bla?: string
    ): Body | BodyReader | BodyStream {

        if(type === "json") {
            const body: Body = Object.create(null);
            Object.defineProperties(body, {
                type: {
                    value: "json",
                    configurable: true,
                    enumerable: true,
                },
                value: {
                    get: () => this.bodyJsonString? JSON.parse(this.bodyJsonString): undefined,
                    configurable: true,
                    enumerable: true,
                },
            });
            return body;
        } else{
            if(this.bodyJsonString){
                throw "filled body"
            } else{
                return Object.create(null);
            }
        }
    }
}




