import { config } from "https://deno.land/x/dotenv/mod.ts";
import {Application, Router} from 'https://deno.land/x/oak/mod.ts';
import {validate} from "./controller/validation.ts";
import {login} from "./controller/login.ts";
import {setup} from "./helper/setup.ts";
import {createNumericTerminationDate, createTerminationDate} from "./helper/dateHelper.ts";

config({export: true, path: "./app/.env"});

await setup();
/*const router = new Router();
router
    .get("/", (context) => {
        context.response.body = "Welcome!";
    })
    .post("/login",async (context) =>{
        await login(context);
    })

const app = new Application();
app.use(await validate)
app.use(router.routes());
app.use(router.allowedMethods());*/

//await app.listen({port: 8000});
