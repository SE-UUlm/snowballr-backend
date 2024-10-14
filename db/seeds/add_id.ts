import { AbstractSeed, Info, ClientPostgreSQL } from "https://deno.land/x/nessie@2.0.10/mod.ts";
import type { Client } from "https://deno.land/x/postgres@v0.17.0/mod.ts";

export default class extends AbstractSeed<ClientPostgreSQL> {
	/** Runs on seed */
	async run(info: Info): Promise<void> {
	}
}
