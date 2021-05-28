import {Database} from "https://deno.land/x/denodb/mod.ts";
import {config} from "https://deno.land/x/dotenv/mod.ts";

config({export: true, path: "../.env"});
const PostgresDB = Deno.env.get("POSTGRES_DB");
const PostgresUser = Deno.env.get("POSTGRES_USER");
const PostgresPassword = Deno.env.get("POSTGRES_PASSWORD");
const PostgresHost = Deno.env.get("POSTGRES_HOST");


export const db = new Database('postgres', {
    host: PostgresHost,
    username: PostgresUser,
    password: PostgresPassword,
    database: PostgresDB,
});
