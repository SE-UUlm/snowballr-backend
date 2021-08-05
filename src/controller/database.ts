import {Database} from "https://deno.land/x/denodb/mod.ts";
import {Client} from "https://deno.land/x/postgres/mod.ts";

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

export const client = new Client({
    user: PostgresUser,
    database: PostgresDB,
    password: PostgresPassword,
    hostname: PostgresHost,
    port: 5432,
});

export const saveChildren = async (into: string, column1: string, column2: string, firstId: number, secondId: number) => {
    await client.queryArray(`insert into ${into}(${column1}, ${column2})
                        VALUES (${firstId}, ${secondId})`);
}