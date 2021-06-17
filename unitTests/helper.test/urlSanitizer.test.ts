import {assertEquals} from "https://deno.land/std@0.97.0/testing/asserts.ts"
import {urlSanitizer} from "../../src/helper/url.ts";

Deno.test({
    name: "testUrlWithHttp",
    async fn(): Promise<void> {
        let url = "http://www.uni-ulm.de"
        url = await urlSanitizer(url)

        assertEquals(url, "https://www.uni-ulm.de")
    }

})


Deno.test({
    name: "testUrlWithoutHttp",
    async fn(): Promise<void> {
        let url = "uni-ulm.de"
        url = await urlSanitizer(url)

        assertEquals(url, "https://uni-ulm.de")
    }

})