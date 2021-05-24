import { listenAndServe } from "https://deno.land/std@0.95.0/http/mod.ts";

const BINDING = ":80";

console.log(`Binding to ${BINDING}...`)

await listenAndServe(BINDING, (req) => {
    req.respond({body: "Hello world!"});
})
