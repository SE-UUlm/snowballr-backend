import {
	ClientMySQL,
	ClientPostgreSQL,
	ClientSQLite,
	NessieConfig,
} from "https://deno.land/x/nessie@2.0.5/mod.ts";

const PostgresDB = String(Deno.env.get("POSTGRES_DB"));
const PostgresUser = String(Deno.env.get("POSTGRES_USER"));
const PostgresPassword = String(Deno.env.get("POSTGRES_PASSWORD"));
const PostgresHost = String(Deno.env.get("POSTGRES_HOST"));

/** Select one of the supported clients */
const client = new ClientPostgreSQL({
	database: PostgresDB,
	hostname: PostgresHost,
	port: 5432,
	user: PostgresUser,
	password: PostgresPassword,
});

// const client = new ClientMySQL({
//     hostname: "localhost",
//     port: 3306,
//     username: "root",
//     // password: "pwd", // uncomment this line for <8
//     db: "nessie",
// });

// const client = new ClientSQLite("./sqlite.db");

/** This is the final config object */
const config: NessieConfig = {
	client,
	migrationFolders: ["./db/migrations"],
	seedFolders: ["./db/seeds"],
};

export default config;
