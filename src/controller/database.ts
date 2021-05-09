import { Database } from "https://deno.land/x/denodb/mod.ts";

const PostgresDB = Deno.env.get("POSTGRES_DB");
const PostgresUser = Deno.env.get("POSTGRES_USER");
const PostgresPassword = Deno.env.get("POSTGRES_PASSWORD");


export const db = new Database('postgres', {
    host: 'postgresdb',
    username: PostgresUser,
    password: PostgresPassword,
    database: PostgresDB,
});

export const closeDB = async () => {
    await db.close;
}