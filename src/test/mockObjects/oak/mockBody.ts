import {BodyType, Body, BodyOptions, BodyReader, BodyStream, Request} from 'https://deno.land/x/oak/mod.ts';

type BodyValueGetter = () => Body["value"];
const decoder = new TextDecoder()


export class RequestBodyMock1 {
    get(
        { type, contentTypes = {} }: BodyOptions,
    ): Body | BodyReader | BodyStream {

        const body: Body = Object.create(null);
        Object.defineProperties(body, {
            type: {
                value: "json",
                configurable: true,
                enumerable: true,
            },
            value: {
                get: () => JSON.parse(`{"email": "test@test"}`),
                configurable: true,
                enumerable: true,
            },
        });
        return body;
    }
}

export class RequestBodyMock2 {
    get(
        { type, contentTypes = {} }: BodyOptions,
    ): Body | BodyReader | BodyStream {

        const body: Body = Object.create(null);
        Object.defineProperties(body, {
            type: {
                value: "json",
                configurable: true,
                enumerable: true,
            },
            value: {
                get: () => JSON.parse(`{"email": "test@test", "password":"ash"}`),
                configurable: true,
                enumerable: true,
            },
        });
        return body;
    }
}

export class RequestBodyMock3 {
    get(
        { type, contentTypes = {} }: BodyOptions,
    ): Body | BodyReader | BodyStream {

        const body: Body = Object.create(null);
        Object.defineProperties(body, {
            type: {
                value: "json",
                configurable: true,
                enumerable: true,
            },
            value: {
                get: () => JSON.parse(`{"email": "test@test1", "password":"ash"}`),
                configurable: true,
                enumerable: true,
            },
        });
        return body;
    }
}




