import {Application, Router} from 'https://deno.land/x/oak/mod.ts';
import {validateContentType, validateTokenIfExists} from "./controller/validation.ts";
import {login} from "./controller/login.ts";
import {setup} from "./helper/setup.ts";

await setup(false);
const router = new Router();
router
    .get("/", (context) => {
        context.response.body = {message: "hello there"}
    })
    .post("/login",async (context) =>{ await login(context)})

const app = new Application();
app.use(await validateContentType)
app.use(await validateTokenIfExists)
app.use(router.routes());
app.use(router.allowedMethods());

await app.listen({port: 8000});
