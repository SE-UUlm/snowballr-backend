import {
	ClientMySQL,
	ClientPostgreSQL,
	ClientSQLite,
	NessieConfig,
} from "https://deno.land/x/nessie@2.0.5/mod.ts";

/** Select one of the supported clients */
const client = new ClientPostgreSQL({
	database: "nessie",
	hostname: "localhost",
	port: 5432,
	user: "root",
	password: "pwd",
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
