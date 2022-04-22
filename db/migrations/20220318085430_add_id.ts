import { AbstractMigration, Info, ClientPostgreSQL } from "https://deno.land/x/nessie@2.0.5/mod.ts";
import type { Client } from "https://deno.land/x/postgres@v0.4.5/mod.ts";

export default class extends AbstractMigration<ClientPostgreSQL> {
	/** Runs on migrate */
	async up(info: Info): Promise<void> {
		// add auto unique key id to papers table
		this.client.query("ALTER TABLE papers ADD COLUMN IF NOT EXISTS custom_id VARCHAR;");
	}

	/** Runs on rollback */
	async down(info: Info): Promise<void> {
		// remove unique key from papers tabel
		this.client.query("ALTER TABLE papers DROP COLUMN custom_id;");
	}
}
