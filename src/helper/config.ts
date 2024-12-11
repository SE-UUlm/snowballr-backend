import { parse } from "https://deno.land/std@0.150.0/encoding/yaml.ts";
import { CacheType } from "../api/cache.ts";

// Complex interface structure to define a configuration

//lvl 1
export interface IConfig {
	googleScholar: IGoogleScholarConfig;
	ieee: IIeeeConfig;
	semanticScholar: ISemanticScholarConfig;
	crossRef: ICrossRefConfig;
	openCitations: IOpenCitationsConfig;
	microsoftAcademic: IMicrosoftAcademicConfig;
	cache: ICacheConfig;
	batcher: IBatcherConfig;
}

export type IBatcherConfig = {
	workerPollingRateInMilSecs: number;
}

//lvl 2
export type IGoogleScholarConfig = {
	proxy: IGoogleScholarProxyConfig;
	requestInterval: IGoogleScholarRequestInterval;
	useCache: boolean;
	baseUrl: string;
	maxCitationCount: number;
	enabled: boolean;
}

//lvl 2
export type IIeeeConfig = {
	useCache: boolean;
	baseUrl: string;
	enabled: boolean;
}

//lvl 2
export type ISemanticScholarConfig = {
	useCache: boolean;
	baseUrl: string;
	enabled: boolean;
}

//lvl 2
export type ICrossRefConfig = {
	useCache: boolean;
	baseUrl: string;
	enabled: boolean;
}

//lvl 2
export type IOpenCitationsConfig = {
	useCache: boolean;
	baseUrl: string;
	enabled: boolean;
	linkedFetchSize: number;
}

//lvl 2
export type IMicrosoftAcademicConfig = {
	useCache: boolean;
	baseUrl: string;
	enabled: boolean;
}

//lvl 2
export type ICacheConfig = {
	timeToLiveInSeconds: number;
	type: CacheType;
}

//lvl 3
export type IGoogleScholarProxyConfig = {
	enabled: boolean;
	mode: "tor" | "pool";
	urls: string[];
	cooldown: number;
}

//lvl 3
export type IGoogleScholarRequestInterval = {
	min: number;
	max: number;
}

// default configuration, to be overwritten by the loaded one, if a property is given there

const DEFAULTCONFIG: IConfig = {
	googleScholar: {
		proxy: { enabled: false, mode: "tor", urls: ["http://localhost:8118"], cooldown: 600, maxCitationCount: 1000 },
		requestInterval: { min: 30, max: 60 },
		useCache: true,
		baseUrl: "https://scholar.google.com",
		enabled: true,
		maxCitationCount: 1000
	},
	ieee: { baseUrl: "http://ieeexploreapi.ieee.org/api/v1/search/articles", useCache: true, enabled: true },
	semanticScholar: { baseUrl: "https://api.semanticscholar.org/v1/paper", useCache: true, enabled: true },
	crossRef: { baseUrl: "https://api.crossref.org/works", useCache: true, enabled: true },
	openCitations: { baseUrl: "https://opencitations.net", useCache: true, enabled: true, linkedFetchSize: 50 },
	microsoftAcademic: {
		baseUrl: "https://api.labs.cognitive.microsoft.com/academic/v1.0/evaluate",
		useCache: true,
		enabled: true
	},
	cache: { timeToLiveInSeconds: 10080000, type: "FileCache" },
	batcher: { workerPollingRateInMilSecs: 500 }
} as IConfig;


//console.log("==============GETTING IMPORTED================")
const loadYaml = () => {
	try {
		const raw: string = Deno.readTextFileSync(`${new URL('.', import.meta.url).pathname}/../config.yaml`);
		const yaml = parse(raw);
		//console.log((yaml as IConfig).cache)
		return yaml;
	}
	catch (e) {
		throw new Error(`Couldnt load config.yaml: ${e}`);
	}
}

const LOADEDCONFIG = loadYaml();

// merge and overwrite default config with custom config

export const CONFIG = { ...DEFAULTCONFIG, ...LOADEDCONFIG as IConfig };

//console.log(CONFIG)
