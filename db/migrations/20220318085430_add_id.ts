import {
	AbstractMigration,
	ClientPostgreSQL,
	Info,
} from "https://deno.land/x/nessie/mod.ts";

export default class extends AbstractMigration<ClientPostgreSQL> {
	async up({ dialect }: Info): Promise<void> {
		await this.client.queryArray("ALTER TABLE papers ADD COLUMN IF NOT EXISTS custom_id VARCHAR;");
	}

	async down({ dialect }: Info): Promise<void> {
		await this.client.queryArray("ALTER TABLE papers DROP COLUMN custom_id;");
	}
}
