import { Database, PostgresConnector } from "https://deno.land/x/denodb/mod.ts";
import { Client } from "https://deno.land/x/postgres/mod.ts";

const PostgresDB = String(Deno.env.get("POSTGRES_DB"));
const PostgresUser = String(Deno.env.get("POSTGRES_USER"));
const PostgresPassword = String(Deno.env.get("POSTGRES_PASSWORD"));
const PostgresHost = String(Deno.env.get("POSTGRES_HOST"));


/**
 * Database for DENODB
 */
const connection = new PostgresConnector({
    host: PostgresHost,
    username: PostgresUser,
    password: PostgresPassword,
    database: PostgresDB,
});

export const db = new Database(connection)

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


/**
 * Direct query for all info of a project paper
 * @param id 
 * @returns 
 */
export const getProjectStageStuff = (id: number) => {
    //      0         1                   2                      3                   4               5           6           7                   8           9              10          11                12          13         14       15              16              17       18                 
    return client.queryArray(`
SELECT result.id, result.final_decision, result.addition_date, result.paper_id, result.doi, result.title, result.abstract, result.year, result.publisher, result.type, result.scope, result.scope_name, result.pdf, a.id, a.raw_string, a.first_name, a.last_name, a.orcid , reviewresult.ids FROM (SELECT i.id, i.final_decision, i.addition_date, p.id as paper_id, p.doi, p.title, p.abstract, p.year, p.publisher, p.type, p.scope, p.scope_name, STRING_AGG(pdf.url, ' ') pdf FROM inscopefor as i JOIN paper as p ON i.paper_id = p.id LEFT OUTER JOIN pdf ON p.id = pdf.paper_id WHERE i.stage_id = ${id} GROUP BY i.id,p.id ORDER BY i.id) AS result LEFT OUTER JOIN (SELECT i.id, STRING_AGG(r.user_id::character varying, ' ') ids FROM inscopefor as i JOIN review as r ON r.paperscopeforstage_id = i.id GROUP BY i.id) as reviewresult ON result.id = reviewresult.id LEFT OUTER JOIN wrote as w ON w.paper_id = result.paper_id LEFT JOIN author as a ON a.id = w.author_id`)
}

export const getProjectStageCount = (id: number) => {
    return client.queryArray(`
    SELECT COUNT(*) FROM inscopefor as i WHERE i.stage_id = ${id}`)
}