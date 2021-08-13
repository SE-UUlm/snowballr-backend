import { Database } from "https://deno.land/x/denodb/mod.ts";
import { Client } from "https://deno.land/x/postgres/mod.ts";

const PostgresDB = Deno.env.get("POSTGRES_DB");
const PostgresUser = Deno.env.get("POSTGRES_USER");
const PostgresPassword = Deno.env.get("POSTGRES_PASSWORD");
const PostgresHost = Deno.env.get("POSTGRES_HOST");


/**
 * Database for DENODB
 */
export const db = new Database('postgres', {
    host: PostgresHost,
    username: PostgresUser,
    password: PostgresPassword,
    database: PostgresDB,
});

/**
 * Database for native SQL (currently used for Selfjoins, since denodb can't use it)
 */
export const client = new Client({
    user: PostgresUser,
    database: PostgresDB,
    password: PostgresPassword,
    hostname: PostgresHost,
    port: 5432,
});

/**
 * Inserts into a Table that is used for many-to-many relationships. Currently used to save references/citations
 * @param into table name 
 * @param column1 column name for the first item
 * @param column2 column name for the second item
 * @param firstId id that is saved in column1
 * @param secondId id that is saved in column2
 */
export const saveChildren = async (into: string, column1: string, column2: string, firstId: number, secondId: number) => {
    await client.queryArray(`insert into ${into}(${column1}, ${column2})
                        VALUES (${firstId}, ${secondId})`);
}

/**
 * Returns the values of a many-to-many relationship table, currently written to retrieve papers from the citation/reference table.
 * @param table name of the table
 * @param column1 column name for the first item
 * @param column2 column name for the second item
 * @param id id of the looked up item
 * @returns 
 */
export const getChildren = (table: string, column1: string, column2: string, id: number) => {
    return client.queryArray(`select p.* from ${table} as i JOIN paper as p ON i.${column2} = p.id WHERE i.${column1} = ${id}`);
}
