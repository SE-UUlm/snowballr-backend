import { createHash } from "https://deno.land/std/hash/mod.ts";
import { IApiQuery } from "../api/iApiQuery.ts";

/**
	 * Create a hash out of the Query to use as key for the Cache
	 *
	 * @param query - Object defined by interface in IApiQuery to filter and query api calls.
	 * @returns hash-string identifing a specific fetch
	 */
export const hashQuery = (query: IApiQuery) => {
	let queryIdentifier = createHash("sha3-256");
	// unset enabled apis, since the hash is only dependant on a single api and is valid and independent of the other fetches
	query.enabledApis = undefined;
	queryIdentifier.update(JSON.stringify(query));
	return queryIdentifier.toString();
}