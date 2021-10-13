import { parse } from "https://deno.land/std/encoding/yaml.ts";

//lvl 1
export interface IConfig {
	googleScholar: IGoogleScholarConfig;
}

//lvl 2
export interface IGoogleScholarConfig {
	proxy: IGoogleScholarProxyConfig;
}

//lvl 3
export interface IGoogleScholarProxyConfig {
	enabled: boolean;
	mode: "tor" | "pool";
	urls: string[];
	cooldown: number;
}

// read in sync since config has to be parsed.
export const CONFIG = parse(Deno.readTextFileSync("../../config.yaml")) as IConfig;