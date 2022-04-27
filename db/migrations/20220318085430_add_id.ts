import {
	AbstractMigration,
	ClientPostgreSQL,
	Info,
} from "https://deno.land/x/nessie/mod.ts";

export default class extends AbstractMigration<ClientPostgreSQL> {
	async up({ dialect }: Info): Promise<void> {
		await this.client.queryArray('ALTER TABLE "paper" ADD COLUMN IF NOT EXISTS "custom_id" VARCHAR;');
	}

	async down({ dialect }: Info): Promise<void> {
		await this.client.queryArray('ALTER TABLE "paper" DROP COLUMN "custom_id";');
	}
}

// docker exec -it backend-develop sh -c "cd /nessie && /bin/deno run -A --unstable https://deno.land/x/nessie/cli.ts migrate"
// docker exec -it backend-develop sh -c "cd /nessie && /bin/deno run -A --unstable https://deno.land/x/nessie/cli.ts rollback"