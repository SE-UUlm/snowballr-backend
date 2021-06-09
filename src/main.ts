import {Application, Router} from 'https://deno.land/x/oak/mod.ts';
import {validateContentType, validateJWTIfExists} from "./controller/validation.ts";
import {login} from "./controller/login.ts";
import {setup} from "./helper/setup.ts";
import {createUser, getUser, getUsers, patchUser} from "./controller/user.ts";
import {logout} from "./controller/logout.ts";
import {SmtpClient} from "https://deno.land/x/smtp/mod.ts";

//TODO user authentication header patch
await setup(true);
const client = new SmtpClient();

const router = new Router();
router
    .get("/", (context) => {
        context.response.body = {message: "hello there"}
    })
    .get("/login", async (context) => {
        await login(context)
    })
    .get("/logout", async (context) => {
        await logout(context)
    })
    .post("/users", async (context) => {
        await createUser(context, client)
    })
    .get("/users", async (context) => {
        await getUsers(context)
    })
    .get("/users/:id", async (context) => {
        await getUser(context, context.params.id)
    })
    .patch("/users/:id", async (context, Methods) => {
        await patchUser(context, Number(context.params.id))
    })

const app = new Application();
app.use(await validateContentType)
app.use(await validateJWTIfExists)
app.use(router.routes());
app.use(router.allowedMethods());

await app.listen({port: 80});
