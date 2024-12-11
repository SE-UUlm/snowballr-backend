import {
  ClientPostgreSQL,
  NessieConfig,
} from "https://deno.land/x/nessie@2.0.10/mod.ts";

const PostgresDB = String(Deno.env.get("POSTGRES_DB"));
const PostgresUser = String(Deno.env.get("POSTGRES_USER"));
const PostgresPassword = String(Deno.env.get("POSTGRES_PASSWORD"));
const PostgresHost = String(Deno.env.get("POSTGRES_HOST"));

/** Select one of the supported clients */
const clientPg = new ClientPostgreSQL({
  database: PostgresDB,
  hostname: PostgresHost,
  port: 5432,
  user: PostgresUser,
  password: PostgresPassword,
});

const config: NessieConfig = {
  client: clientPg,
  migrationFolders: ["./db/migrations"],
  seedFolders: ["./db/seeds"],
  additionalMigrationFiles: [],
  additionalSeedFiles: [],
  migrationTemplate: undefined,
  seedTemplate: undefined,
  debug: false,
};

export default config;
