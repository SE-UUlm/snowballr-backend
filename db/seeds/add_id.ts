import { AbstractSeed, Info, ClientPostgreSQL } from "https://deno.land/x/nessie/mod.ts";
import type { Client } from "https://deno.land/x/postgres/mod.ts";

export default class extends AbstractSeed<ClientPostgreSQL> {
	/** Runs on seed */
	async run(info: Info): Promise<void> {
	}
}
