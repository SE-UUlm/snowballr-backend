import { parse, Type } from "https://deno.land/std/encoding/yaml.ts";
import { CacheType } from "../api/cache.ts";

//lvl 1
export interface IConfig {
	googleScholar: IGoogleScholarConfig;
	ieee: IIeeeConfig;
	semanticScholar: ISemanticScholarConfig;
	crossRef: ICrossRefConfig;
	openCitations: IOpenCitationsConfig;
	microsoftAcademic: IMicrosoftAcademicConfig;
	cache: ICacheConfig;
}

//lvl 2
export interface IGoogleScholarConfig {
	proxy: IGoogleScholarProxyConfig;
	requestInterval: IGoogleScholarRequestInterval;
	useCache: boolean;
	baseUrl: string;
}

//lvl 2
export interface IIeeeConfig {
	useCache: boolean;
	baseUrl: string;
}

//lvl 2
export interface ISemanticScholarConfig {
	useCache: boolean;
	baseUrl: string;
}

//lvl 2
export interface ICrossRefConfig {
	useCache: boolean;
	baseUrl: string;
}

//lvl 2
export interface IOpenCitationsConfig {
	useCache: boolean;
	baseUrl: string;
}

//lvl 2
export interface IMicrosoftAcademicConfig {
	useCache: boolean;
	baseUrl: string;
}

//lvl 2
export interface ICacheConfig {
	timeToLiveInSeconds: number;
	type: CacheType;
}

//lvl 3
export interface IGoogleScholarProxyConfig {
	enabled: boolean;
	mode: "tor" | "pool";
	urls: string[];
	cooldown: number;
}

//lvl 3
export interface IGoogleScholarRequestInterval {
	min: number;
	max: number;
}

//console.log("==============GETTING IMPORTED================")
const loadYaml = (): IConfig => {
	try {
		const raw: string = Deno.readTextFileSync("../../config.yaml");
		const yaml: unknown = parse(raw);
		return <IConfig>yaml;
	}
	catch (e) {
		throw new Error(`Couldnt load config.yaml: ${e}`);
	}
}

export const CONFIG: IConfig = loadYaml();